package com.example.customfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 旋转光环层：悬浮在头顶的水平发光圆环，随时间旋转，颜色可配置（彩虹或固定色）。
 * 使用 RenderType.lightning()（加色混合、无背面剔除），无需贴图。
 */
public class HaloLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int SEGMENTS = 48;
    private static final float INNER = 0.30F;
    private static final float OUTER = 0.55F;
    private static final float HEIGHT = 2.05F;

    public HaloLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!FxConfig.halo || player.isInvisible()) return;

        float t = player.tickCount + partialTick;
        int rgb = FxConfig.haloColor((long) (t * 50.0F));
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.0D, HEIGHT, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 3.0F));

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();
        int alpha = 190;

        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (i * Math.PI * 2.0 / SEGMENTS);
            float a1 = (float) ((i + 1) * Math.PI * 2.0 / SEGMENTS);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            // 正面四边形：外0 -> 内0 -> 内1 -> 外1
            vertex(vc, mat, c0 * OUTER, s0 * OUTER, r, g, b, alpha);
            vertex(vc, mat, c0 * INNER, s0 * INNER, r, g, b, alpha);
            vertex(vc, mat, c1 * INNER, s1 * INNER, r, g, b, alpha);
            vertex(vc, mat, c1 * OUTER, s1 * OUTER, r, g, b, alpha);
            // 反面四边形（反绕序）：保证俯视/仰视/任意角度都可见
            vertex(vc, mat, c1 * OUTER, s1 * OUTER, r, g, b, alpha);
            vertex(vc, mat, c1 * INNER, s1 * INNER, r, g, b, alpha);
            vertex(vc, mat, c0 * INNER, s0 * INNER, r, g, b, alpha);
            vertex(vc, mat, c0 * OUTER, s0 * OUTER, r, g, b, alpha);
        }
        poseStack.popPose();
    }

    private void vertex(VertexConsumer vc, Matrix4f mat, float x, float z, int r, int g, int b, int a) {
        vc.vertex(mat, x, 0.0F, z).color(r, g, b, a).endVertex();
    }
}
