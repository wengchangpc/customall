package com.example.customcape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 第一人称披风：原版第一人称完全不渲染玩家身体，披风自然看不见。
 * 这里在实体渲染阶段手动把披风画到本地玩家背后——
 * 与第三人称完全一致的摆动数学（源自原版 CapeLayer），
 * 转头回望、飞行俯冲、从高处向下看时都能看到披风飘动。
 * 纯客户端渲染叠加，服务器零感知。
 */
@Mod.EventBusSubscriber(modid = "customall", value = Dist.CLIENT)
public class FirstPersonCapeRenderer {

    private static PlayerModel<LocalPlayer> model;
    private static net.minecraft.client.model.geom.ModelPart cloakPart;

    private static boolean ensureModel(Minecraft mc) {
        if (model == null) {
            try {
                model = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
                model.setAllVisible(false);
                // 单独 bake 一份拿 cloak 部件（上面 setAllVisible(false) 会隐藏原部件）
                cloakPart = mc.getEntityModels().bakeLayer(ModelLayers.PLAYER).getChild("cloak");
            } catch (Exception e) {
                com.example.customall.CustomAllMod.LOGGER.error("[CustomCape] 第一人称披风模型初始化失败", e);
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
        if (!CapeConfig.fpCape) return;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return; // F5 下原版披风层已生效
        if (player.isSpectator() || player.isInvisible()) return;
        // 与原版 CapeLayer 相同的渲染条件
        if (!player.isCapeLoaded() || player.getCloakTextureLocation() == null) return;
        if (!player.isModelPartShown(PlayerModelPart.CAPE)) return;
        if (player.isCrouching() || player.isFallFlying()) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(Items.ELYTRA)) return;
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
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0.0F, -1.501F, 0.0F);

        // ===== 以下摆动数学与原版 CapeLayer.render 完全一致 =====
        ps.translate(0.0F, 0.0F, 0.125F);
        double d0 = Mth.lerp(pt, player.xCloakO, player.xCloak) - Mth.lerp(pt, player.xo, player.getX());
        double d1 = Mth.lerp(pt, player.yCloakO, player.yCloak) - Mth.lerp(pt, player.yo, player.getY());
        double d2 = Mth.lerp(pt, player.zCloakO, player.zCloak) - Mth.lerp(pt, player.zo, player.getZ());
        double sinYaw = Mth.sin(yaw * (float) (Math.PI / 180.0));
        double cosYaw = -Mth.cos(yaw * (float) (Math.PI / 180.0));
        float f1 = Mth.clamp((float) d1 * 10.0F, -6.0F, 32.0F);
        float f2 = Mth.clamp((float) (d0 * sinYaw + d2 * cosYaw) * 100.0F, 0.0F, 150.0F);
        float f3 = Mth.clamp((float) (d0 * cosYaw - d2 * sinYaw) * 100.0F, -20.0F, 20.0F);
        float f4 = Mth.lerp(pt, player.oBob, player.bob);
        f1 += Mth.sin(Mth.lerp(pt, player.walkDistO, player.walkDist) * 6.0F) * 32.0F * f4;
        ps.mulPose(Axis.XP.rotationDegrees(6.0F + f2 / 2.0F + f1));
        ps.mulPose(Axis.ZP.rotationDegrees(f3 / 2.0F));
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - f3 / 2.0F));
        // =======================================================

        int light = LevelRenderer.getLightColor(player.level(), player.blockPosition());
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entitySolid(player.getCloakTextureLocation()));
        model.renderCloak(ps, vc, light, OverlayTexture.NO_OVERLAY);

        // 流光镀层：全亮度自发光再画一遍，呼吸式脉动
        // eyes 渲染类型为 ONE,ONE 加法混合：alpha 不参与混合，亮度脉动必须走顶点色
        if (CapeConfig.gloss) {
            float t = player.tickCount + pt;
            float pulse = 0.78F + 0.22F * Mth.sin(t * 0.09F);
            float k = Mth.clamp(CapeConfig.glossAlpha * 2.0F, 0.0F, 1.0F) * pulse;
            VertexConsumer gvc = bufferSource.getBuffer(RenderType.eyes(player.getCloakTextureLocation()));
            cloakPart.render(ps, gvc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1.0F);
        }
        ps.popPose();

        // 只冲刷披风用到的缓冲区，不影响其他渲染
        bufferSource.endBatch(RenderType.entitySolid(player.getCloakTextureLocation()));
        if (CapeConfig.gloss) {
            bufferSource.endBatch(RenderType.eyes(player.getCloakTextureLocation()));
        }
    }
}
