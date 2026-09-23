package com.example.customall;

import com.example.customcape.CapeConfig;
import com.example.customcape.CapeGlossLayer;
import com.example.customcape.CapeTextureManager;
import com.example.customfx.FxConfig;
import com.example.customfx.HaloLayer;
import com.example.customskin.GlossLayer;
import com.example.customskin.SkinConfig;
import com.example.customskin.SkinTextureManager;
import com.example.customwings.WingsConfig;
import com.example.customwings.WingsLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * CustomAll - 披风 / 皮肤 / 特效 / 翅膀 四合一纯客户端模组。
 * 各功能模块的配置路径、贴图路径与原独立模组完全一致：
 *   config/CustomCape/  config/CustomSkin/  config/CustomFX/  config/CustomWings/
 */
@Mod(CustomAllMod.MODID)
public class CustomAllMod {
    public static final String MODID = "customall";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public CustomAllMod() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            // 纯客户端模组：在专用服务器上什么都不做
            return;
        }

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onAddLayers);
        modBus.addListener(this::onRegisterLayerDefinitions);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderNameTag);

        WingsConfig.load();
        FxConfig.load();

        LOGGER.info("[CustomAll] 已初始化：披风 / 皮肤 / 特效 / 翅膀，全部仅自己可见！");
    }

    // ---------- 生命周期 ----------

    private void onClientSetup(FMLClientSetupEvent event) {
        // GL 纹理操作必须回到渲染线程执行
        Minecraft.getInstance().execute(() -> {
            CapeTextureManager.copyDefaultIfMissing();
            CapeTextureManager.load();
            SkinTextureManager.copyDefaultIfMissing();
            SkinTextureManager.load();
        });
    }

    /** 翅膀模型几何注册 */
    private void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WingsLayer.LAYER, WingsLayer::createLayer);
    }

    /** 把四套渲染层挂到玩家渲染器上（Forge 47.x 全版本兼容写法） */
    private void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // 用原版玩家模型的 bake 产物拿 cloak 部件（披风流光镀层用，几何与原版披风完全一致）
        var root = event.getContext().bakeLayer(ModelLayers.PLAYER);
        for (String skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer pr) {
                pr.addLayer(new CapeGlossLayer(pr, root));   // 披风流光镀层
                pr.addLayer(new GlossLayer(pr));             // 皮肤流光镀层
                pr.addLayer(new HaloLayer(pr));              // 旋转光环
                pr.addLayer(new WingsLayer(pr, event.getContext().bakeLayer(WingsLayer.LAYER))); // 翅膀
            }
        }
        LOGGER.info("[CustomAll] 全部渲染层已挂载（披风流光 / 皮肤流光 / 光环 / 翅膀）！");
    }

    // ---------- 特效：彩色名字（只给自己的名字染色，服务器零感知） ----------

    private void onRenderNameTag(RenderNameTagEvent event) {
        if (!FxConfig.nameColorEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;

        long time = System.currentTimeMillis();
        int rgb = FxConfig.nameColor(time);
        String base = event.getContent() != null ? event.getContent().getString()
                : mc.player.getGameProfile().getName();
        MutableComponent colored = Component.literal(base)
                .withStyle(Style.EMPTY.withColor(TextColor.parseColor(String.format("#%06X", rgb))));
        event.setContent(colored);
    }

    // ---------- 指令（四棵指令树，与原独立模组完全一致） ----------

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        registerCapeCommands(event);
        registerSkinCommands(event);
        registerFxCommands(event);
        registerWingsCommands(event);
    }

    private void registerCapeCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("customcape")
            .then(Commands.literal("reload").executes(ctx -> {
                // 反馈必须在加载完成之后给出（load 在渲染线程执行）
                Minecraft.getInstance().execute(() -> {
                    CapeTextureManager.load();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            CapeTextureManager.isAvailable()
                                ? "[CustomCape] 披风贴图已重新加载！"
                                : "[CustomCape] 未找到 cape.png，请把贴图放到 config/CustomCape/cape.png"),
                        false);
                });
                return 1;
            }))
            .then(Commands.literal("fp").executes(ctx -> {
                CapeConfig.fpCape = !CapeConfig.fpCape;
                CapeConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        CapeConfig.fpCape
                            ? "[CustomCape] 第一人称披风已开启！回头或俯冲即可看到。"
                            : "[CustomCape] 第一人称披风已关闭。"), false);
                return 1;
            }))
            .then(Commands.literal("vivid").executes(ctx -> {
                CapeConfig.vivid = !CapeConfig.vivid;
                CapeConfig.save();
                Minecraft.getInstance().execute(() -> CapeTextureManager.load()); // 贴图需重载生效
                ctx.getSource().sendSuccess(() -> Component.literal(
                        CapeConfig.vivid
                            ? "[CustomCape] 增艳已开启，贴图已重载！"
                            : "[CustomCape] 增艳已关闭，贴图已重载。"), false);
                return 1;
            }))
            .then(Commands.literal("gloss").executes(ctx -> {
                CapeConfig.gloss = !CapeConfig.gloss;
                CapeConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        CapeConfig.gloss
                            ? "[CustomCape] 流光镀层已开启！暗处也会发光。"
                            : "[CustomCape] 流光镀层已关闭。"), false);
                return 1;
            })));
    }

    private void registerSkinCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("customskin")
            .then(Commands.literal("reload").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    SkinTextureManager.load();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            SkinTextureManager.isAvailable()
                                ? "[CustomSkin] 皮肤已重新加载！(model=" + (SkinTextureManager.isSlim() ? "slim" : "classic") + ")"
                                : "[CustomSkin] 未找到 skin.png，请把皮肤放到 config/CustomSkin/skin.png"),
                        false);
                });
                return 1;
            }))
            .then(Commands.literal("gloss").executes(ctx -> {
                SkinConfig.gloss = !SkinConfig.gloss;
                SkinConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        SkinConfig.gloss ? "[CustomSkin] 流光镀层已开启！" : "[CustomSkin] 流光镀层已关闭。"), false);
                return 1;
            }))
            .then(Commands.literal("vivid").executes(ctx -> {
                SkinConfig.vivid = !SkinConfig.vivid;
                SkinConfig.save();
                Minecraft.getInstance().execute(() -> {
                    SkinTextureManager.load();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[CustomSkin] 增艳" + (SkinConfig.vivid ? "已开启，" : "已关闭，") + "皮肤已重新加载！"), false);
                });
                return 1;
            })));
    }

    private void registerFxCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("customfx")
            .then(Commands.literal("reload").executes(ctx -> {
                FxConfig.load();
                ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                        "[CustomFX] 已重载：glow=%s halo=%s name=%s haloColor=%s nameColor=%s",
                        FxConfig.glow, FxConfig.halo, FxConfig.nameColorEnabled,
                        FxConfig.haloColorMode, FxConfig.nameColorMode)), false);
                return 1;
            }))
            .then(Commands.literal("glow").executes(ctx -> {
                FxConfig.glow = !FxConfig.glow;
                FxConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "[CustomFX] 发光描边：" + (FxConfig.glow ? "开" : "关")), false);
                return 1;
            }))
            .then(Commands.literal("halo").executes(ctx -> {
                FxConfig.halo = !FxConfig.halo;
                FxConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "[CustomFX] 旋转光环：" + (FxConfig.halo ? "开" : "关")), false);
                return 1;
            }))
            .then(Commands.literal("name").executes(ctx -> {
                FxConfig.nameColorEnabled = !FxConfig.nameColorEnabled;
                FxConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "[CustomFX] 彩色名字：" + (FxConfig.nameColorEnabled ? "开" : "关")), false);
                return 1;
            })));
    }

    private void registerWingsCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("customwings")
            .then(Commands.literal("toggle").executes(ctx -> {
                WingsConfig.enabled = !WingsConfig.enabled;
                WingsConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        WingsConfig.enabled ? "[CustomWings] 翅膀已开启！" : "[CustomWings] 翅膀已关闭。"), false);
                return 1;
            }))
            .then(Commands.literal("color")
                .then(Commands.argument("mode", StringArgumentType.word()).executes(ctx -> {
                    String mode = StringArgumentType.getString(ctx, "mode");
                    if (!mode.equalsIgnoreCase("rainbow") && !(mode.startsWith("#") && mode.length() == 7)) {
                        ctx.getSource().sendFailure(Component.literal(
                                "[CustomWings] 无效颜色！用法: /customwings color rainbow 或 /customwings color #FF8800"));
                        return 0;
                    }
                    WingsConfig.colorMode = mode;
                    WingsConfig.save();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[CustomWings] 配色已切换为 " + mode), false);
                    return 1;
                })))
            .then(Commands.literal("size")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.3F, 3.0F)).executes(ctx -> {
                    WingsConfig.size = FloatArgumentType.getFloat(ctx, "value");
                    WingsConfig.save();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[CustomWings] 翅膀大小已设为 " + WingsConfig.size), false);
                    return 1;
                })))
            .then(Commands.literal("speed")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01F, 1.0F)).executes(ctx -> {
                    WingsConfig.flapSpeed = FloatArgumentType.getFloat(ctx, "value");
                    WingsConfig.save();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[CustomWings] 扇动速度已设为 " + WingsConfig.flapSpeed), false);
                    return 1;
                })))
            .then(Commands.literal("fp").executes(ctx -> {
                WingsConfig.fp = !WingsConfig.fp;
                WingsConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        WingsConfig.fp
                            ? "[CustomWings] 第一人称翅膀已开启！回头或飞行时即可看到。"
                            : "[CustomWings] 第一人称翅膀已关闭。"), false);
                return 1;
            }))
            .then(Commands.literal("gloss").executes(ctx -> {
                WingsConfig.gloss = !WingsConfig.gloss;
                WingsConfig.save();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        WingsConfig.gloss
                            ? "[CustomWings] 流光镀层已开启！暗处也会发光。"
                            : "[CustomWings] 流光镀层已关闭。"), false);
                return 1;
            }))
            .then(Commands.literal("reload").executes(ctx -> {
                WingsConfig.load();
                ctx.getSource().sendSuccess(() -> Component.literal("[CustomWings] 配置已重新加载！"), false);
                return 1;
            })));
    }
}
