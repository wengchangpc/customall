package com.example.customcape.mixin;

import com.example.customcape.CapeConfig;
import com.example.customcape.CapeTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 AbstractClientPlayer 的披风贴图入口处"偷梁换柱"：
 * 默认(applyToAll=true)所有玩家都披上自定义披风；设为 false 时仅本地玩家生效。
 * 服务器始终看不到任何变化。
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Unique
    private boolean customcape$isLocalPlayer() {
        return (Object) this == Minecraft.getInstance().player;
    }

    // 1.20.1 Forge 运行期为 SRG 混淆名；双目标写法保证开发环境(mojmap)与生产 jar(SRG) 都能命中
    @Inject(method = {"getCloakTextureLocation", "m_108561_"}, at = @At("HEAD"), cancellable = true)
    private void customcape$getCapeTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        if (CapeTextureManager.isAvailable()
                && (CapeConfig.applyToAll || customcape$isLocalPlayer())) {
            cir.setReturnValue(CapeTextureManager.getCapeTextureId());
        }
    }

    @Inject(method = {"isCapeLoaded", "m_108555_"}, at = @At("HEAD"), cancellable = true)
    private void customcape$canRenderCapeTexture(CallbackInfoReturnable<Boolean> cir) {
        if (CapeTextureManager.isAvailable()
                && (CapeConfig.applyToAll || customcape$isLocalPlayer())) {
            cir.setReturnValue(true);
        }
    }
}
