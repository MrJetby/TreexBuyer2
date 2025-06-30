package me.jetby.treexbuyer;

import lombok.Getter;
import lombok.Setter;
import me.jetby.treexbuyer.commands.PluginCommands;
import me.jetby.treexbuyer.configurations.MenuLoader;
import me.jetby.treexbuyer.configurations.PriceItemCfg;
import me.jetby.treexbuyer.configurations.PriseItemLoader;
import me.jetby.treexbuyer.configurations.newcfg.Config;
import me.jetby.treexbuyer.tools.Database;
import me.jetby.treexbuyer.listeners.DataLoader;
import me.jetby.treexbuyer.menus.*;
import me.jetby.treexbuyer.menus.actions.Actions;
import me.jetby.treexbuyer.functions.AutoBuyManager;
import me.jetby.treexbuyer.functions.BoostManager;
import me.jetby.treexbuyer.tools.Metrics;
import me.jetby.treexbuyer.tools.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
public final class Main extends JavaPlugin {

    private Database database;
    private MenusManager menusManager;
    private BoostManager boostManager;
    private SellZone sellZone;
    private MenuLoader menuLoader;
    @Setter private Map<String, PriseItemLoader.ItemData> itemPrice;
    @Setter private Map<String, PriseItemLoader.PotionData> potionPrices = new HashMap<>();
    private PriseItemLoader priseItemLoader;
    private AutoBuyManager autoBuyManager;
    private Actions actions;
    private Economy economy;
    private PlaceholderExpansion placeholderExpansion;

    private final Config cfg = new Config(this);

    private PriceItemCfg priceItemCfg;

    @Override
    public void onEnable() {

        priceItemCfg = new PriceItemCfg();
        priceItemCfg.loadYamlFile(this);

        final FileConfiguration configFile = cfg.getFile(getDataFolder().getAbsolutePath(), "config.yml");
        cfg.load(configFile);

        boostManager = new BoostManager(this);
        sellZone = new SellZone(this);
        menuLoader = new MenuLoader(this);
        menusManager = new MenusManager(this);
        menusManager.menuCheck();
        database = new Database(this);
        database.initDatabase();
        autoBuyManager = new AutoBuyManager(this);
        autoBuyManager.startAutoBuy();
        actions = new Actions(this);

        menuLoader.loadMenus(getDataFolder());

        priseItemLoader = new PriseItemLoader(this);
        File itemFile = new File(getDataFolder(), cfg.getPriceItemFile());
        itemPrice = priseItemLoader.loadItemValuesFromFile(itemFile);

        setupEconomy();
        createCommand();

        getServer().getPluginManager().registerEvents(new DataLoader(this), this);
        getServer().getPluginManager().registerEvents(new ClickHandler(this), this);

        getCommand("treexbuyer").setExecutor(new PluginCommands(this));
        getCommand("treexbuyer").setTabCompleter(new PluginCommands(this));

        for (Player player : Bukkit.getOnlinePlayers()) {
            autoBuyManager.loadPlayerData(player.getUniqueId());
            boostManager.loadPlayersScores(player.getUniqueId());
        }
        getLogger().info("[TreexBuyer] Плагин запущен и готов к работе.");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new PlaceholderExpansion(this);
            placeholderExpansion.register();
        } else {
            getLogger().warning("PlaceholderAPI не обнаружен! Некоторые функции могут быть недоступны.");
        }

        new Metrics(this, 25141);
    }
    public void createCommand(){
        List<String> commandNames = new ArrayList<>();
        Map<String, Menus> listMenu = menuLoader.getListMenu();
        listMenu.forEach((key, item) -> {
            commandNames.addAll(item.getOpen_commands());
        });
        for (String commandName : commandNames) {
            registerCommand(commandName, new MenusCommandRegistrar(this));
        }
    }
    private void registerCommand(String commandName, CommandExecutor executor) {
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            Command command = new BukkitCommand(commandName) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    return executor.onCommand(sender, this, label, args);
                }
            };

            command.setAliases(Collections.emptyList());

            commandMap.register(getDescription().getName(), command);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setupEconomy() {
        RegisteredServiceProvider<Economy> registeredServiceProvider = getServer().getServicesManager().getRegistration(Economy.class);

        if (registeredServiceProvider == null) {
            return;
        }
        economy = registeredServiceProvider.getProvider();
    }
    public PriseItemLoader.ItemData getItemPrice(String material) {
        return itemPrice.get(material);
    }
    public PriseItemLoader.PotionData getPotionPrice(String potionType) {
        return potionPrices.get(potionType);
    }
    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        for (UUID uuid : boostManager.getCachedScores().keySet()) {
            boostManager.savePlayerScoresSync(uuid);
        }
        try {
            database.getDbExecutor().shutdown();
            if (!database.getDbExecutor().awaitTermination(5, TimeUnit.SECONDS)) {
                database.getDbExecutor().shutdownNow();
            }
            if (database.getConnection() != null && !database.getConnection().isClosed()) {
                database.getConnection().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        getLogger().warning("[TreexBuyer] Плагин отключён.");
    }

}
