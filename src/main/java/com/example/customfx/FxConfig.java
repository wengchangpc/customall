package com.example.customfx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 简易配置：config/CustomFX/settings.txt
 * 每行 key=value，# 开头为注释。
 */
public class FxConfig {
    /** 发光描边开关 */
    public static boolean glow = true;
    /** 旋转光环开关 */
    public static boolean halo = true;
    /** 彩色名字开关 */
    public static boolean nameColorEnabled = true;
    /** 光环颜色：rainbow 或 #RRGGBB */
    public static String haloColorMode = "rainbow";
    /** 名字颜色：rainbow 或 #RRGGBB */
    public static String nameColorMode = "rainbow";

    public static Path file() {
        return Paths.get("config", "CustomFX", "settings.txt");
    }

    public static synchronized void load() {
        Path f = file();
        try {
            if (!Files.exists(f)) {
                Files.createDirectories(f.getParent());
                save();
                return;
            }
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                String[] kv = line.split("=", 2);
                String k = kv[0].trim().toLowerCase();
                String v = kv[1].trim();
                switch (k) {
                    case "glow" -> glow = Boolean.parseBoolean(v);
                    case "halo" -> halo = Boolean.parseBoolean(v);
                    case "name" -> nameColorEnabled = Boolean.parseBoolean(v);
                    case "halocolor" -> haloColorMode = v;
                    case "namecolor" -> nameColorMode = v;
                    default -> { }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static synchronized void save() {
        String content = """
                # CustomFX 配置文件（仅自己可见的角色特效）
                # glow / halo / name: true 或 false
                # haloColor / nameColor: rainbow（彩虹循环）或十六进制 #RRGGBB
                glow=%s
                halo=%s
                name=%s
                haloColor=%s
                nameColor=%s
                """.formatted(glow, halo, nameColorEnabled, haloColorMode, nameColorMode);
        try {
            Files.createDirectories(file().getParent());
            Files.write(file(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    /**
     * 解析颜色模式：rainbow -> 随时间循环的彩虹；否则按 #RRGGBB / RRGGBB 解析，失败则回退白色。
     * @return 24bit 0xRRGGBB
     */
    public static int parseColor(String mode, long time) {
        if (!"rainbow".equalsIgnoreCase(mode)) {
            String s = mode.startsWith("#") ? mode.substring(1) : mode;
            try {
                return Integer.parseInt(s, 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
            }
        }
        float hue = (time % 2000L) / 2000.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0xFFFFFF;
    }

    public static int haloColor(long time) { return parseColor(haloColorMode, time); }

    public static int nameColor(long time) { return parseColor(nameColorMode, time); }
}
