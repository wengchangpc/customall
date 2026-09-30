package com.example.customskin.mixin;

import com.example.customskin.SkinConfig;
import com.example.customskin.SkinTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 AbstractClientPlayer 的皮肤入口处"偷梁换柱"：
 * 默认(applyToAll=true)所有玩家都显示自定义皮肤贴图与模型；
 * 设为 false 时仅本地玩家生效。服务器始终看不到任何变化。
 *
 * 双目标写法：官方映射名（开发环境）+ SRG 混淆名（1.20.1 生产环境）。
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Unique
    private boolean customskin$isLocalPlayer() {
        return (Object) this == Minecraft.getInstance().player;
    }

    @Inject(method = {"getSkinTextureLocation", "m_108560_"}, at = @At("HEAD"), cancellable = true)
    private void customskin$getSkinTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        if (SkinTextureManager.isAvailable()
                && (SkinConfig.applyToAll || customskin$isLocalPlayer())) {
            cir.setReturnValue(SkinTextureManager.getSkinTextureId());
        }
    }

    @Inject(method = {"getModelName", "m_108564_"}, at = @At("HEAD"), cancellable = true)
    private void customskin$getModelName(CallbackInfoReturnable<String> cir) {
        if (SkinTextureManager.isAvailable()
                && (SkinConfig.applyToAll || customskin$isLocalPlayer())) {
            cir.setReturnValue(SkinTextureManager.isSlim() ? "slim" : "classic");
        }
    }
}
