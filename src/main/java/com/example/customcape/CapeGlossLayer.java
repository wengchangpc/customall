package com.example.customcape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 披风流光镀层：用全亮度自发光方式把披风再画一遍，
 * 与披风本体完全相同的摆动数学（CapePoseMath，源自原版 CapeLayer），
 * 严丝合缝地镀上一层呼吸式脉动的光泽——暗处也会发亮。
 * 纯客户端渲染叠加，服务器零感知。
 */
public class CapeGlossLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final ModelPart cloak;

    public CapeGlossLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
        super(parent);
        this.cloak = root.getChild("cloak");
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player != Minecraft.getInstance().player) return;   // 只渲染自己
        if (!CapeConfig.gloss) return;
        if (!CapeTextureManager.isAvailable()) return;          // 未加载自定义披风时不镀
        if (player.isInvisible() || player.isSpectator()) return;
        if (!player.isCapeLoaded() || player.getCloakTextureLocation() == null) return;
        if (!player.isModelPartShown(PlayerModelPart.CAPE)) return;
        if (player.isCrouching() || player.isFallFlying()) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(Items.ELYTRA)) return;

        ps.pushPose();
        // 与原版 CapeLayer 完全一致的摆动（yaw 来源也一致：yBodyRot 插值）
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        CapePoseMath.applyCloakSwing(ps, player, partialTick, yaw);

        float t = player.tickCount + partialTick;
        float pulse = 0.78F + 0.22F * Mth.sin(t * 0.09F);
        // eyes 渲染类型为 ONE,ONE 加法混合：alpha 不参与混合，亮度脉动必须走顶点色
        float k = Mth.clamp(CapeConfig.glossAlpha * 2.0F, 0.0F, 1.0F) * pulse;

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(player.getCloakTextureLocation()));
        cloak.render(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1.0F);
        ps.popPose();
    }
}
