package com.example.customskin;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomSkin 配置：config/CustomSkin/settings.txt
 * gloss=true/false        流光镀层开关（模型表面叠加全亮度光泽）
 * glossAlpha=0.0~1.0      流光强度
 * vivid=true/false        增艳开关（加载贴图时提饱和提亮度）
 */
public final class SkinConfig {
    public static boolean gloss = true;
    public static float glossAlpha = 0.30F;
    public static boolean vivid = true;

    private SkinConfig() {
    }

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("CustomSkin").resolve("settings.txt");
    }

    public static void load() {
        Map<String, String> kv = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file(), StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                int i = line.indexOf('=');
                kv.put(line.substring(0, i).trim().toLowerCase(),
                        line.substring(i + 1).trim().toLowerCase());
            }
        } catch (Exception ignored) {
        }
        gloss = parseBool(kv.get("gloss"), true);
        vivid = parseBool(kv.get("vivid"), true);
        try {
            glossAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(kv.getOrDefault("glossalpha", "0.30"))));
        } catch (Exception e) {
            glossAlpha = 0.30F;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), """
                    # CustomSkin settings
                    # gloss: 流光镀层开关 (true/false)
                    # glossAlpha: 流光强度 0.0~1.0
                    # vivid: 增艳(提饱和提亮度)开关 (true/false)
                    gloss=%s
                    glossAlpha=%s
                    vivid=%s
                    """.formatted(gloss, glossAlpha, vivid), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        return s.equals("true") || s.equals("1") || s.equals("on") || s.equals("yes");
    }
}
