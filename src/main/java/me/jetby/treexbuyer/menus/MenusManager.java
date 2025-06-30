package me.jetby.treexbuyer.menus;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.PriseItemLoader;
import me.jetby.treexbuyer.configurations.Config;
import me.jetby.treexbuyer.functions.BoostManager;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.jetby.treexbuyer.menus.ClickHandler.NAMESPACED_KEY;
import static me.jetby.treexbuyer.tools.Hex.hex;
import static me.jetby.treexbuyer.tools.Hex.setPlaceholders;

public class MenusManager {

    private final Map<String, Menus> listMenus;
    private final BoostManager boostManager;
    private final SellZone sellZone;
    private final Main plugin;
    private final Config config;

    public MenusManager(Main plugin) {
        this.plugin = plugin;
        this.boostManager = plugin.getBoostManager();
        this.listMenus = plugin.getMenuLoader().listMenu;
        this.sellZone = plugin.getSellZone();
        this.config = plugin.getCfg();
    }

    public void menuCheck() {
        listMenus.forEach((key, vault) -> {
            vault.getButtons().forEach(edit -> {
                plugin.getLogger().info("Меню: " + key + " загружен");
            });
        });
    }

    public void updateMenu(MenuButtons button, Inventory topInventory, String counts, Player player) {
        PriseItemLoader.ItemData itemData = plugin.getItemPrice(button.getMaterialButton().name());
        double price = (itemData != null ? itemData.price() : 0);
        double price_with_coefficient = price * boostManager.getPlayerCoefficient(player);
        double coefficient = boostManager.getPlayerCoefficient(player);
        double score = boostManager.getCachedScores(player);
        String enabled = config.getAutoBuyEnable();
        String disabled = config.getAutoBuyDisable();
        String global_auto_sell_toggle_string = plugin.getAutoBuyManager().getPlayerData(plugin, player.getUniqueId()).isAutoBuyEnabled() ? enabled : disabled;
        List<String> autoBuyList = plugin.getAutoBuyManager().getPlayerData(plugin, player.getUniqueId()).getItems();

        if (!button.getCommand().contains("[sell_zone]")) {
            ItemStack itemStack = button.getItemStackofMaterial();
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (button.getTitleButton() != null) {
                    meta.setDisplayName(setInsidePlaceholders(button.getTitleButton(), player, coefficient, score, counts, global_auto_sell_toggle_string));
                }
                boolean isSpecialButton = button.getCommand().contains("[AUTOBUY_ITEM_TOGGLE]") ||
                        button.getCommand().contains("[SELL_ALL]");
                boolean isStatusToggle = button.getCommand().contains("[AUTOBUY_STATUS_TOGGLE]");
                if (!isSpecialButton) {
                    meta.getPersistentDataContainer().set(NAMESPACED_KEY, PersistentDataType.STRING, "menu_button");
                }
                if (button.isEnchanted() || autoBuyList != null && autoBuyList.contains(button.getMaterialButton().name())
                        && !meta.getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING)) {
                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else if (isStatusToggle && plugin.getAutoBuyManager().getPlayerData(plugin, player.getUniqueId()).isAutoBuyEnabled()) {
                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                meta.setLore(setInsidePlaceholders(button.getLoreButton(), button, player, price, coefficient, score, counts, price_with_coefficient, autoBuyList, global_auto_sell_toggle_string));
                itemStack.setItemMeta(meta);
            }
            topInventory.setItem(button.getSlotButton(), itemStack);
        }
    }

    public String setInsidePlaceholders(String strings, Player player, double coefficient, double score, String count, String global_auto_sell_toggle_string) {
        return hex(setPlaceholders(player, strings
                .replace("%coefficient%", String.valueOf(coefficient))
                .replace("%score%", String.valueOf(score))
                .replace("%sell_pay%", String.valueOf(count))
                .replace("%global_auto_sell_toggle_state%", hex(global_auto_sell_toggle_string))));
    }

    public List<String> setInsidePlaceholders(List<String> strings, MenuButtons button, Player player, double price, double coefficient, double score, String count, double price_with_coefficient, List<String> autoBuyList, String global_auto_sell_toggle_string) {
        return strings.stream()
                .map(s -> hex(setPlaceholders(player, s)))
                .map(s -> s.replace("%price%", String.valueOf(price)))
                .map(s -> s.replace("%coefficient%", String.valueOf(coefficient)))
                .map(s -> s.replace("%score%", String.valueOf(score)))
                .map(s -> s.replace("%sell_pay%", String.valueOf(count)))
                .map(s -> s.replace("%price_with_coefficient%", String.valueOf(price_with_coefficient)))
                .map(s -> s.replace("%auto_sell_toggle_state%", hex(autoBuyList != null && autoBuyList.contains(button.getMaterialButton().name())
                        ? config.getAutoBuyEnable()
                        : config.getAutoBuyDisable())))
                .map(s -> s.replace("%global_auto_sell_toggle_state%", hex(global_auto_sell_toggle_string)))
                .toList();
    }

    public void openMenu(Player player, String menuName) {
        Menus menu = listMenus.get(menuName);
        if (menu == null) {
            player.sendMessage("Меню не найдено!");
            return;
        }
        double coefficient = boostManager.getPlayerCoefficient(player);
        double score = boostManager.getCachedScores(player);
        String count = sellZone.getCountPlayerString(player.getUniqueId(), 0);
        String global_auto_sell_toggle_string = plugin.getAutoBuyManager().getPlayerData(plugin, player.getUniqueId()).isAutoBuyEnabled()
                ? config.getAutoBuyEnable()
                : config.getAutoBuyDisable();

        Inventory inventory = Bukkit.createInventory(menu, menu.getSize(), setInsidePlaceholders(menu.getTitle(), player, coefficient, score, count, global_auto_sell_toggle_string));
        if (!(inventory.getHolder() instanceof Menus)) {
            return;
        }
        for (MenuButtons btn : menu.getButtons()) {
            List<ItemStack> itemStacks = new ArrayList<>();
            sellZone.checkItem(itemStacks, plugin.getItemPrice(), player);
            updateMenu(btn, inventory, count, player);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(inventory), 3L);
    }

    public Map<String, Menus> getListMenu() {
        return listMenus;
    }
}