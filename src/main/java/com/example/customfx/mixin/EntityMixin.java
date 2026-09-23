package com.example.customfx.mixin;

import com.example.customfx.FxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让发光描边在渲染查询层面恒定生效：
 * 直接覆写 isCurrentlyGlowing 的返回值，避免用 setGlowingTag 写实体数据标记时
 * 被服务器下发的实体元数据包冲掉（表现为描边一闪而过/完全不可见）。
 * 双目标：官方名（开发环境）+ SRG 名（生产环境）。
 */
@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = {"isCurrentlyGlowing", "m_142038_"}, at = @At("HEAD"), cancellable = true)
    private void customfx$isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (!FxConfig.glow) return;
        Minecraft mc = Minecraft.getInstance();
        if ((Object) this == mc.player && mc.level != null) {
            cir.setReturnValue(true);
        }
    }
}
