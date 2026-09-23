package com.example.customwings;

import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 翅膀配置：config/CustomWings/settings.txt */
public class WingsConfig {
    public static boolean enabled = true;
    /** rainbow 或 #RRGGBB */
    public static String colorMode = "rainbow";
    public static float size = 1.0F;
    public static float flapSpeed = 0.12F;
    /** 第一人称翅膀可见（回头/飞行时屏幕边缘能看到翅膀） */
    public static boolean fp = true;
    /** 流光镀层（全亮度自发光脉动） */
    public static boolean gloss = true;
    public static float glossAlpha = 0.35F;

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("CustomWings").resolve("settings.txt");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            save();
            return;
        }
        try {
            for (String line : Files.readAllLines(f)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                switch (k) {
                    case "enabled" -> enabled = Boolean.parseBoolean(v);
                    case "colorMode" -> colorMode = v;
                    case "size" -> { try { size = Float.parseFloat(v); } catch (NumberFormatException ignored) {} }
                    case "flapSpeed" -> { try { flapSpeed = Float.parseFloat(v); } catch (NumberFormatException ignored) {} }
                    case "gloss" -> gloss = Boolean.parseBoolean(v);
                    case "glossAlpha" -> { try { glossAlpha = Float.parseFloat(v); } catch (NumberFormatException ignored) {} }
                }
            }
        } catch (IOException e) {
            if (FMLEnvironment.production) System.err.println("[CustomWings] 读取配置失败: " + e);
        }
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            List<String> lines = List.of(
                    "# CustomWings 配置",
                    "# colorMode: rainbow 或 #RRGGBB（如 #FF8800）",
                    "enabled=" + enabled,
                    "colorMode=" + colorMode,
                    "size=" + size,
                    "flapSpeed=" + flapSpeed,
                    "fp=" + fp,
                    "gloss=" + gloss,
                    "glossAlpha=" + glossAlpha
            );
            Files.write(f, lines);
        } catch (IOException e) {
            System.err.println("[CustomWings] 写入配置失败: " + e);
        }
    }
}
