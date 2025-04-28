package me.jetby.treexbuyer.configurations;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class Config {
    private static FileConfiguration config;
    private static File file;

    public void loadYamlFile(Plugin plugin) {
        file = new File(plugin.getDataFolder(), "config.yml");


        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("config.yml", true);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration CFG() {
        return config;
    }

    public void reloadCfg(Plugin plugin) {
        if(!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("config.yml", true);
        }
        try {
            config.load(file);
            Bukkit.getConsoleSender().sendMessage("Конфигурация перезагружена! (config.yml)");
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getConsoleSender().sendMessage("Не удалось перезагрузить конфигурацию! (config.yml)");
        }
    }
    public void saveCfg(Plugin plugin) {
        try {
            File file = new File(plugin.getDataFolder(), "config.yml");
            config.save(file);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Не удалось сохранить файл config.yml", e);
        }
    }
}
