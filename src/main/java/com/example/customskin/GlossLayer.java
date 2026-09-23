package com.example.customskin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 流光镀层：把当前玩家模型用"全亮度自发光"渲染方式再画一遍，
 * 相当于给皮肤表面镀一层脉动的光泽——暗处也会发亮。
 * 纯客户端渲染叠加，服务器零感知。
 */
public class GlossLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public GlossLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player != Minecraft.getInstance().player) return;   // 只渲染自己
        if (!SkinConfig.gloss) return;
        if (player.isInvisible()) return;
        if (!SkinTextureManager.isAvailable()) return;          // 未加载自定义皮肤时不镀

        // 使用当前实际显示的皮肤贴图（含本模组替换后的自定义皮肤）
        ResourceLocation tex = player.getSkinTextureLocation();

        // 轻微呼吸式脉动
        float t = player.tickCount + partialTick;
        float alpha = SkinConfig.glossAlpha * (0.80F + 0.20F * Mth.sin(t * 0.08F));

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(tex));
        this.getParentModel().renderToBuffer(poseStack, vc,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, alpha);
    }
}
