package com.example.customwings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 挂在玩家渲染器上的翅膀层：仅对本地玩家渲染，自带扇动动画。 */
public class WingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("customwings", "wings"), "main");
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("customwings", "textures/wings.png");

    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public WingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
        super(parent);
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    /** 翅膀几何：主翼膜 + 下缘羽 + 翼尖羽，双翼镜像。 */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 右翼：从背部铰点(2, 0.5, 3)向 +x 伸展
        CubeListBuilder right = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -4.0F, -0.5F, 11, 8, 1)      // 主翼膜
                .texOffs(0, 10).addBox(1.0F, 3.5F, -0.5F, 9, 3, 1)        // 下缘羽
                .texOffs(24, 0).addBox(9.5F, 0.5F, -0.5F, 4, 4, 1);       // 翼尖羽

        // 左翼：几何沿 x 轴镜像，UV 同步镜像
        CubeListBuilder left = CubeListBuilder.create()
                .texOffs(0, 0).mirror().addBox(-10.0F, -4.0F, -0.5F, 11, 8, 1)
                .texOffs(0, 10).mirror().addBox(-10.0F, 3.5F, -0.5F, 9, 3, 1)
                .texOffs(24, 0).mirror().addBox(-13.5F, 0.5F, -0.5F, 4, 4, 1);

        root.addOrReplaceChild("right_wing", right, PartPose.offset(2.0F, 0.5F, 3.0F));
        root.addOrReplaceChild("left_wing", left, PartPose.offset(-2.0F, 0.5F, 3.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // 核心规则：只画自己
        if (player != Minecraft.getInstance().player) return;
        if (!WingsConfig.enabled) return;
        if (player.isInvisible()) return;

        poseStack.pushPose();
        float s = Mth.clamp(WingsConfig.size, 0.3F, 3.0F);
        poseStack.scale(s, s, s);

        float t = player.tickCount + partialTick;

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

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        rightWing.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
        leftWing.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);

        // 流光镀层：全亮度自发光再画一遍，呼吸式脉动（暗处也会发亮）
        // eyes 渲染类型为 ONE,ONE 加法混合：alpha 不参与混合，亮度脉动必须走顶点色
        if (WingsConfig.gloss) {
            float pulse = 0.78F + 0.22F * Mth.sin(t * 0.09F);
            float k = Mth.clamp(WingsConfig.glossAlpha * 2.0F, 0.0F, 1.0F) * pulse;
            // 镀层颜色向白色提亮，流光更璀璨
            float gr = (r + (1.0F - r) * 0.35F) * k;
            float gg = (g + (1.0F - g) * 0.35F) * k;
            float gb = (b + (1.0F - b) * 0.35F) * k;
            VertexConsumer gvc = buffer.getBuffer(RenderType.eyes(TEXTURE));
            rightWing.render(poseStack, gvc, 0xF000F0, OverlayTexture.NO_OVERLAY, gr, gg, gb, 1.0F);
            leftWing.render(poseStack, gvc, 0xF000F0, OverlayTexture.NO_OVERLAY, gr, gg, gb, 1.0F);
        }

        poseStack.popPose();
    }
}
