package com.example.customcape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * 披风摆动数学：与原版 CapeLayer.render 完全一致（源自 1.20.1 反编译源码）。
 * 供第三人称流光镀层与第一人称渲染共用，保证所有披风渲染严丝合缝。
 */
public final class CapePoseMath {

    private CapePoseMath() {
    }

    /** 在实体模型空间内调用：应用披风偏移与摆动旋转。 */
    public static void applyCloakSwing(PoseStack ps, AbstractClientPlayer player, float pt, float yaw) {
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
    }
}
