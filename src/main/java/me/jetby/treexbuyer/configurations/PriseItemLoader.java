package me.jetby.treexbuyer.configurations;

import me.jetby.treexbuyer.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PriseItemLoader {

    private final Main plugin;

    public PriseItemLoader (Main plugin) {
        this.plugin = plugin;
    }

    public Map<String, ItemData> loadItemValuesFromFile(File file) {
        Map<String, ItemData> itemValues = new HashMap<>();
        if (!file.exists()) {
            plugin.getLogger().warning("Файл " + file.getName() + " не найден!");
            return itemValues;
        }

        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        for (String key : fileConfig.getKeys(false)) {
            double price = fileConfig.getDouble(key + ".price", 0);
            int addScores = fileConfig.getInt(key + ".add-scores", 0);
            itemValues.put(key, new ItemData(price, addScores));
        }

        return itemValues;
    }

    public record ItemData(double price, int addScores) {
    }

}
