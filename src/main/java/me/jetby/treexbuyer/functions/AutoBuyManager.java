package me.jetby.treexbuyer.functions;


import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.PriseItemLoader;
import me.jetby.treexbuyer.configurations.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.jetby.treexbuyer.menus.ClickHandler.isIsRegularItem;

public class AutoBuyManager {

    private static final Map<UUID, AutoBuy> players = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.##");

    public AutoBuy getPlayerData(Main plugin, UUID uuid) {
        return players.computeIfAbsent(uuid, id -> new AutoBuy(id, plugin));
    }

    private final Main plugin;
    private final Config config;
    private final BoostManager boostManager;
    public AutoBuyManager(Main plugin) {
        this.plugin = plugin;
        this.boostManager = plugin.getBoostManager();
        this.config = plugin.getCfg();
    }

    public void loadPlayerData(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AutoBuy data = new AutoBuy(uuid, plugin);

            data.loadFromDatabase(() -> {
                players.put(uuid, data);
                log("Данные автоскупки загружены для " + uuid);
            });
        });
    }

    public void unloadPlayerData(UUID uuid) {
        players.remove(uuid);
    }

    private void log(String msg) {
        if (config.isDebug()) {
            Bukkit.getLogger().info(msg);
        }
    }

    public void startAutoBuy() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int delay = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!getPlayerData(plugin, player.getUniqueId()).isAutoBuyEnabled()) {
                    continue;
                }

                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    checkForItems(player);
                }, delay);

                delay += 2;
            }
        }, 0L, config.getAutoBuyDelay());
    }

    private void checkForItems(Player player) {

        List<String> disabled_worlds = config.getAutoBuyDisabledWorlds();
        if (!disabled_worlds.isEmpty()) {
            for (String disabled_world : disabled_worlds) {
                World world = player.getWorld();

                if (world.equals(Bukkit.getWorld(disabled_world))) return;
            }
        }


        List<String> autoBuyList = getPlayerData(plugin, player.getUniqueId()).getItems();
        if (autoBuyList == null || autoBuyList.isEmpty()) {
            return;
        }

        double sumCount = 0d;
        int totalScores = 0;
        ItemStack air = new ItemStack(Material.AIR);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && autoBuyList.contains(item.getType().name())) {
                PriseItemLoader.ItemData itemData = plugin.getItemPrice(item.getType().name());

                boolean isRegularItem = isIsRegularItem(item);

                if (isRegularItem) {
                    if (itemData != null) {
                        double price = itemData.price();
                        int scores = itemData.addScores();

                        if (price > 0d) {
                            double totalPrice = price * item.getAmount();
                            sumCount += totalPrice * boostManager.getPlayerCoefficient(player);
                            plugin.getEconomy().depositPlayer(player, sumCount);
                            totalScores += scores * item.getAmount();
                            if (player.getEquipment().getItemInOffHand()!=null && player.getEquipment().getItemInOffHand().equals(item)) {
                                player.getEquipment().setItemInOffHand(air);
                            }
                            if (player.getEquipment().getHelmet()!=null && player.getEquipment().getHelmet().equals(item)) {
                                player.getEquipment().setHelmet(air);
                            }
                            if (player.getEquipment().getChestplate()!=null && player.getEquipment().getChestplate().equals(item)) {
                                player.getEquipment().setChestplate(air);
                            }
                            if (player.getEquipment().getLeggings()!=null && player.getEquipment().getLeggings().equals(item)) {
                                player.getEquipment().setLeggings(air);
                            }
                            if (player.getEquipment().getBoots()!=null && player.getEquipment().getBoots().equals(item)) {
                                player.getEquipment().setBoots(air);
                            }

                            player.getInventory().removeItem(item);
                        }
                    }
                }

            }
        }

        if (sumCount > 0d) {
            for (String cmd : config.getAutoBuyActions()) {
                plugin.getActions().execute(player, cmd
                        .replace("%sell_pay%", String.valueOf(df.format(sumCount)))
                        .replace("%sell_score%", String.valueOf(totalScores)));
            }
        }

        if (totalScores > 0) {
            boostManager.addPlayerScores(player, totalScores);
        }
    }
}
