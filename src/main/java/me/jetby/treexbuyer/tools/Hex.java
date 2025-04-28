package me.jetby.treexbuyer.tools;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hex {
    private static final Pattern hexPattern1 = Pattern.compile("&#([a-fA-F0-9]{6})", Pattern.CASE_INSENSITIVE);
    private static final Pattern hexPattern2 = Pattern.compile("&x([&a-fA-F0-9]){12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern hexPattern3 = Pattern.compile("<#([a-fA-F0-9]{6})>", Pattern.CASE_INSENSITIVE);

    public static @NotNull String hex(@NotNull String message) {
        message = replaceHexFormat(message, hexPattern1, "&#");
        message = replaceHexFormat(message, hexPattern2, "&x");
        message = replaceHexFormat(message, hexPattern3, "<#");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String replaceHexFormat(String message, Pattern pattern, String formatPrefix) {
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexColor = matcher.group();
            String normalizedColor = normalizeHexColor(hexColor, formatPrefix);
            message = message.replace(hexColor, ChatColor.of(normalizedColor).toString());
        }
        return message;
    }
    private static String normalizeHexColor(String hexColor, String formatPrefix) {
        if (formatPrefix.equals("&#") || formatPrefix.equals("<#")) {
            hexColor = hexColor.substring(2, 8);
            return "#" + hexColor;
        }
        if (formatPrefix.equals("&x")) {
            StringBuilder normalized = new StringBuilder("#");
            for (int i = 2; i < hexColor.length(); i += 2) {
                char colorChar = hexColor.charAt(i + 1);
                if (Character.isDigit(colorChar) || (colorChar >= 'a' && colorChar <= 'f') || (colorChar >= 'A' && colorChar <= 'F')) {
                    normalized.append(colorChar);
                }
            }
            return normalized.toString();
        }
        return hexColor;
    }
    public static @NotNull String setPlaceholders(@Nullable OfflinePlayer player, @NotNull String string) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return PlaceholderAPI.setPlaceholders(player, string);
            } catch (Exception ignore) {
            }
        }
        return string;
    }
    public static String color(String string) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', string);
    }
}
