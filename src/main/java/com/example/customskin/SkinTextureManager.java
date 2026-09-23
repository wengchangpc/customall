package com.example.customskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 皮肤贴图管理器：从 config/CustomSkin/ 动态加载皮肤贴图与模型类型。
 * skin.png  = 皮肤贴图（64x64 或 64x32）
 * model.txt = 模型类型：classic（默认）或 slim（Alex 细手臂）
 */
public final class SkinTextureManager {
    private static final Logger LOGGER = LogManager.getLogger("customskin");

    public static final ResourceLocation SKIN_ID = new ResourceLocation("customskin", "skin");

    private static DynamicTexture texture;
    private static boolean available = false;
    private static boolean slim = false;

    private SkinTextureManager() {
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean isSlim() {
        return slim;
    }

    public static ResourceLocation getSkinTextureId() {
        return SKIN_ID;
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("CustomSkin");
    }

    public static Path getSkinFile() {
        return getConfigDir().resolve("skin.png");
    }

    public static Path getModelFile() {
        return getConfigDir().resolve("model.txt");
    }

    /** 释放默认贴图与模型说明（仅首次）。GL 无关，可任意线程。 */
    public static void copyDefaultIfMissing() {
        try {
            Files.createDirectories(getConfigDir());
            if (!Files.exists(getSkinFile())) {
                try (InputStream in = SkinTextureManager.class
                        .getResourceAsStream("/assets/customskin/default_skin.png")) {
                    if (in != null) {
                        Files.copy(in, getSkinFile());
                        LOGGER.info("[CustomSkin] 已释放默认皮肤到 {}", getSkinFile());
                    }
                }
            }
            if (!Files.exists(getModelFile())) {
                Files.writeString(getModelFile(), "classic\n");
            }
        } catch (Exception e) {
            LOGGER.error("[CustomSkin] 无法创建默认皮肤文件", e);
        }
    }

    /** 加载（或重新加载）皮肤。必须在渲染线程调用。 */
    public static void load() {
        SkinConfig.load();
        Path file = getSkinFile();
        if (!Files.isRegularFile(file)) {
            available = false;
            LOGGER.info("[CustomSkin] 未找到皮肤贴图: {}", file);
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            var image = com.mojang.blaze3d.platform.NativeImage.read(in);
            if (SkinConfig.vivid) {
                enhance(image);
            }
            DynamicTexture newTexture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(SKIN_ID, newTexture);
            if (texture != null) {
                texture.close();
            }
            texture = newTexture;
            available = true;
            slim = readSlim();
            LOGGER.info("[CustomSkin] 皮肤已加载: {} (model={}, vivid={})",
                    file, slim ? "slim" : "classic", SkinConfig.vivid);
        } catch (Exception e) {
            available = false;
            LOGGER.error("[CustomSkin] 加载皮肤失败: {}", file, e);
        }
    }

    /**
     * 增艳滤镜：提饱和度(+35%) + 提亮度(+10%)，让皮肤更明亮鲜艳。
     * NativeImage 像素为 ABGR 打包格式。
     */
    private static void enhance(com.mojang.blaze3d.platform.NativeImage img) {
        try {
            int w = img.getWidth(), h = img.getHeight();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = img.getPixelRGBA(x, y);
                    int a = (p >>> 24) & 0xFF;
                    int b = (p >>> 16) & 0xFF;
                    int g = (p >>> 8) & 0xFF;
                    int r = p & 0xFF;
                    float gray = 0.299F * r + 0.587F * g + 0.114F * b;
                    r = clamp255((int) ((gray + (r - gray) * 1.35F) * 1.10F));
                    g = clamp255((int) ((gray + (g - gray) * 1.35F) * 1.10F));
                    b = clamp255((int) ((gray + (b - gray) * 1.35F) * 1.10F));
                    img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[CustomSkin] 增艳处理失败，使用原图", e);
        }
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static boolean readSlim() {
        try {
            String s = Files.readString(getModelFile(), StandardCharsets.UTF_8).trim().toLowerCase();
            return s.contains("slim") || s.contains("alex");
        } catch (Exception e) {
            return false;
        }
    }
}
