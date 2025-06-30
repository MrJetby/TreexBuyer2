package me.jetby.treexbuyer.configurations.newcfg;

import lombok.Getter;
import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.functions.Boost;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.jetby.treexbuyer.tools.Hex.hex;


@Getter
public class Config {

    private boolean mysql;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;

    private boolean debug;

    private String priceItemFile;
    private final Map<String, String> menus = new HashMap<>(); // menuId, menuPath

    private int autoBuyDelay;
    private List<String> autoBuyActions;
    private List<String> autoBuyDisabledWorlds;
    private String autoBuyEnable;
    private String autoBuyDisable;

    private int scores;
    private double coefficient;
    private int maxCoefficient;
    private int defaultCoefficient;
    private boolean boosters_except_legal_coefficient;

    private final Map<String, Boost> boosts = new HashMap<>();

    private String sellMsg;
    private String noItemsMsg;


    private final Main plugin;
    public Config(Main plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration configuration) {

        if (configuration.getConfigurationSection("mysql")!=null) {
            mysql = configuration.getBoolean("mysql.enabled", false);
            mysqlHost = configuration.getString("mysql.host", "localhost");
            mysqlPort = configuration.getInt("mysql.port", 3306);
            mysqlDatabase = configuration.getString("mysql.database", "treexbuyer");
            mysqlUsername = configuration.getString("mysql.username", "root");
            mysqlPassword = configuration.getString("mysql.password", "");
        }

        debug = configuration.getBoolean("debug", false);

        priceItemFile = configuration.getString("priceItem.path", "priceItem.yml");

        for (String id : configuration.getConfigurationSection("menu").getKeys(false)) {
            String path = configuration.getString("menu." + id + ".path");
            menus.put(id, path);
        }
        if (configuration.getConfigurationSection("autobuy")!=null) {
            autoBuyDelay = configuration.getInt("autobuy.delay", 60);
            autoBuyActions = configuration.getStringList("autobuy.actions");
            autoBuyDisabledWorlds = configuration.getStringList("autobuy.disabled-worlds");
            autoBuyEnable = configuration.getString("autobuy.status.enable", "&aВключён");
            autoBuyDisable = configuration.getString("autobuy.status.disable", "&cВыключен");
        }

        scores = configuration.getInt("score-to-multiplier-ratio.scores", 100);
        coefficient = configuration.getDouble("score-to-multiplier-ratio.coefficient", 0.01);

        maxCoefficient = configuration.getInt("max-legal-coefficient", 3);
        defaultCoefficient = configuration.getInt("default-coefficient", 1);
        boosters_except_legal_coefficient = configuration.getBoolean("boosters_except_legal_coefficient", false);

        sellMsg = configuration.getString(hex("messages.sell"));
        noItemsMsg = configuration.getString(hex("messages.noItems"));

        loadBoosts(configuration);
    }


    public void loadBoosts(FileConfiguration configuration) {
        boosts.clear();
        ConfigurationSection boosterSection = configuration.getConfigurationSection("booster");
        if (boosterSection != null) {
            for (String key : boosterSection.getKeys(false)) {
                String permission = boosterSection.getString(key + ".permission");
                double coefficient = boosterSection.getDouble(key + ".external-coefficient", 0.0);
                boosts.put(key, new Boost(key, permission, coefficient));
            }
        }
    }

    public FileConfiguration getFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    public void save(String path, FileConfiguration config, String fileName) {
            try {
                config.save(new File(path, fileName));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
    }
}
