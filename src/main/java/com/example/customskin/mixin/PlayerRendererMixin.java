package com.example.customskin.mixin;

import com.example.customskin.SkinConfig;
import com.example.customskin.SkinTextureManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称手部流光：原版 renderHand 画完手臂与袖子后，
 * 用全亮度 eyes 渲染方式把同一手臂再画一遍，实现"镀层"效果。
 * renderHand 内部没有 pushPose/popPose，TAIL 注入可直接拿到手臂变换。
 *
 * 双目标写法：官方映射名 + SRG 混淆名（1.20.1 生产环境）。
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = {"renderHand", "m_117775_"}, at = @At("TAIL"))
    private void customskin$handGloss(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                      CallbackInfo ci) {
        if (!SkinConfig.gloss) return;
        if (!SkinTextureManager.isAvailable()) return;
        if ((Object) player != Minecraft.getInstance().player) return;   // 只镀自己

        float t = player.tickCount + Minecraft.getInstance().getFrameTime();
        float alpha = SkinConfig.glossAlpha * (0.80F + 0.20F * Mth.sin(t * 0.08F)); // 呼吸式脉动

        ResourceLocation tex = player.getSkinTextureLocation();
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(tex));
        arm.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, alpha);
        sleeve.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, alpha);
    }
}
