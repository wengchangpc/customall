package com.example.customwings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 第一人称翅膀：原版第一人称不渲染玩家身体，翅膀自然看不见。
 * 这里在实体渲染阶段手动把翅膀画到本地玩家背后——
 * 与第三人称翅膀层相同的几何、扇动动画与配色；
 * 滑翔时还复刻了鞘翅的俯仰姿态，俯冲时翅膀会在视野中展开。
 * 纯客户端渲染叠加，服务器零感知。
 */
@Mod.EventBusSubscriber(modid = "customall", value = Dist.CLIENT)
public class FirstPersonWingsRenderer {

    private static ModelPart rightWing;
    private static ModelPart leftWing;

    private static boolean ensureModel(Minecraft mc) {
        if (rightWing == null) {
            try {
                ModelPart root = mc.getEntityModels().bakeLayer(WingsLayer.LAYER);
                rightWing = root.getChild("right_wing");
                leftWing = root.getChild("left_wing");
            } catch (Exception e) {
                com.example.customall.CustomAllMod.LOGGER.error("[CustomWings] 第一人称翅膀模型初始化失败", e);
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!WingsConfig.enabled || !WingsConfig.fp) return;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return; // F5 下翅膀层已生效
        if (player.isSpectator() || player.isInvisible()) return;
        if (!ensureModel(mc)) return;

        float pt = event.getPartialTick();
        PoseStack ps = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();

        ps.pushPose();
        // 与 LivingEntityRenderer 相同的实体空间变换
        double px = Mth.lerp(pt, player.xOld, player.getX());
        double py = Mth.lerp(pt, player.yOld, player.getY());
        double pz = Mth.lerp(pt, player.zOld, player.getZ());
        ps.translate(px - cam.x, py - cam.y, pz - cam.z);
        float yaw = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // 滑翔（鞘翅）时复刻原版 setupRotations 的俯仰姿态
        if (player.isFallFlying()) {
            float f6 = (float) player.getFallFlyingTicks() + pt;
            float f7 = Mth.clamp(f6 * f6 / 100.0F, 0.0F, 1.0F);
            if (!player.isAutoSpinAttack()) {
                ps.mulPose(Axis.XP.rotationDegrees(f7 * (-90.0F - player.getXRot())));
            }
        }
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0.0F, -1.501F, 0.0F);

        float s = Mth.clamp(WingsConfig.size, 0.3F, 3.0F);
        ps.scale(s, s, s);

        float t = player.tickCount + pt;
        if (player.isFallFlying()) {
            // 滑翔（鞘翅）时双翼全展，气流感拉满
            rightWing.yRot = 1.15F;
            rightWing.zRot = 0.10F;
        } else {
            float flap = Mth.sin(t * WingsConfig.flapSpeed) * 0.5F;
            rightWing.yRot = 0.30F + flap;                 // 开合
            rightWing.zRot = 0.30F - flap * 0.55F;         // 上扬/下压
        }
        leftWing.yRot = -rightWing.yRot;
        leftWing.zRot = -rightWing.zRot;

        // 配色：彩虹循环 或 固定色
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (WingsConfig.colorMode != null && WingsConfig.colorMode.startsWith("#") && WingsConfig.colorMode.length() == 7) {
            try {
                int rgb = Integer.parseInt(WingsConfig.colorMode.substring(1), 16);
                r = ((rgb >> 16) & 0xFF) / 255.0F;
                g = ((rgb >> 8) & 0xFF) / 255.0F;
                b = (rgb & 0xFF) / 255.0F;
            } catch (NumberFormatException ignored) {}
        } else {
            float hue = (t * 0.01F) % 1.0F;
            int rgb = Mth.hsvToRgb(hue, 0.85F, 1.0F);
            r = ((rgb >> 16) & 0xFF) / 255.0F;
            g = ((rgb >> 8) & 0xFF) / 255.0F;
            b = (rgb & 0xFF) / 255.0F;
        }

        int light = LevelRenderer.getLightColor(player.level(), player.blockPosition());
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(WingsLayer.TEXTURE));
        rightWing.render(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
        leftWing.render(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);

        // 流光镀层：全亮度自发光再画一遍，呼吸式脉动
        // eyes 渲染类型为 ONE,ONE 加法混合：alpha 不参与混合，亮度脉动必须走顶点色
        if (WingsConfig.gloss) {
            float pulse = 0.78F + 0.22F * Mth.sin(t * 0.09F);
            float k = Mth.clamp(WingsConfig.glossAlpha * 2.0F, 0.0F, 1.0F) * pulse;
            float gr = (r + (1.0F - r) * 0.35F) * k;
            float gg = (g + (1.0F - g) * 0.35F) * k;
            float gb = (b + (1.0F - b) * 0.35F) * k;
            VertexConsumer gvc = bufferSource.getBuffer(RenderType.eyes(WingsLayer.TEXTURE));
            rightWing.render(ps, gvc, 0xF000F0, OverlayTexture.NO_OVERLAY, gr, gg, gb, 1.0F);
            leftWing.render(ps, gvc, 0xF000F0, OverlayTexture.NO_OVERLAY, gr, gg, gb, 1.0F);
        }
        ps.popPose();

        // 只冲刷翅膀用到的缓冲区，不影响其他渲染
        bufferSource.endBatch(RenderType.entityTranslucent(WingsLayer.TEXTURE));
        if (WingsConfig.gloss) {
            bufferSource.endBatch(RenderType.eyes(WingsLayer.TEXTURE));
        }
    }
}
