package com.example.customcape;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomCape 配置：config/CustomCape/settings.txt
 * fpCape=true/false      第一人称披风可见开关
 * vivid=true/false       增艳开关（加载贴图时提饱和提亮度）
 * gloss=true/false       流光镀层开关（全亮度光泽叠加）
 * glossAlpha=0.0~1.0     流光强度
 */
public final class CapeConfig {
    public static boolean fpCape = true;
    public static boolean vivid = true;
    public static boolean gloss = true;
    public static float glossAlpha = 0.35F;

    private CapeConfig() {
    }

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("CustomCape").resolve("settings.txt");
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
        fpCape = parseBool(kv.get("fpcape"), true);
        vivid = parseBool(kv.get("vivid"), true);
        gloss = parseBool(kv.get("gloss"), true);
        try {
            glossAlpha = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(kv.getOrDefault("glossalpha", "0.35"))));
        } catch (Exception e) {
            glossAlpha = 0.35F;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), """
                    # CustomCape settings
                    # fpCape: 第一人称是否渲染披风 (true/false)
                    # vivid: 增艳(提饱和提亮度)开关 (true/false)
                    # gloss: 流光镀层开关 (true/false)
                    # glossAlpha: 流光强度 0.0~1.0
                    fpCape=%s
                    vivid=%s
                    gloss=%s
                    glossAlpha=%s
                    """.formatted(fpCape, vivid, gloss, glossAlpha), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        return s.equals("true") || s.equals("1") || s.equals("on") || s.equals("yes");
    }
}
