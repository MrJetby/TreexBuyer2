package me.jetby.treexbuyer.menus;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.PriseItemLoader;
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
import java.util.UUID;

import static me.jetby.treexbuyer.configurations.Config.CFG;
import static me.jetby.treexbuyer.menus.ClickHandler.NAMESPACED_KEY;
import static me.jetby.treexbuyer.tools.Hex.hex;
import static me.jetby.treexbuyer.tools.Hex.setPlaceholders;

public class MenusManager {

    private final Map<String, Menus> listMenus;
    private final BoostManager boostManager;
    private final SellZone sellZone;
    private final Main plugin;

    public MenusManager(Main plugin) {
        this.plugin = plugin;
        this.boostManager = plugin.getBoostManager();
        this.listMenus = plugin.getMenuLoader().listMenu;
        this.sellZone = plugin.getSellZone();
    }
    public void menuCheck(){
        listMenus.forEach((key, vault) -> {
            plugin.getLogger().info("Меню: " + key + " загружен");
            vault.getButtons().forEach(edit -> {

            });
        });
    }
    public void updateMenu(MenuButtons button, Inventory topInventory, String count, Player player) {

        if (!button.getCommand().contains("[sell_zone]")) {



            ItemStack itemStack = button.getItemStackofMaterial();

            ItemMeta meta = itemStack.getItemMeta();

            PriseItemLoader.ItemData itemData = plugin.getItemPrice(button.getMaterialButton().name());
            double price = (itemData != null ? itemData.price() : 0);
            double price_with_coefficient = price * boostManager.getPlayerCoefficient(player);
            double coefficient = boostManager.getPlayerCoefficient(player);
            double score = boostManager.getCachedScores(player);

            String enabled = CFG().getString("autoBuy.enable", "&aВключён");
            String disabled = CFG().getString("autoBuy.disable", "&cВыключён");

            String global_auto_sell_toggle_string;

            if (meta != null) {
                if (button.getTitleButton() != null) {
                    meta.setDisplayName(hex(button.getTitleButton()));
                }
                boolean isSpecialButton = button.getCommand().contains("[AUTOBUY_ITEM_TOGGLE]") ||
                        button.getCommand().contains("[SELL_ALL]");
                boolean isStatusToggle = button.getCommand().contains("[AUTOBUY_STATUS_TOGGLE]");

                if (!isSpecialButton) {
                    meta.getPersistentDataContainer().set(NAMESPACED_KEY, PersistentDataType.STRING, "menu_button");
                }

                UUID playerId = player.getUniqueId();
                List<String> autoBuyList = plugin.getAutoBuyManager().getPlayerData(plugin, playerId).getItems();
                if (autoBuyList != null && autoBuyList.contains(button.getMaterialButton().name())
                        && !meta.getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING)) {

                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);


                } else if (isStatusToggle &&  plugin.getAutoBuyManager().getPlayerData(plugin, playerId).isAutoBuyEnabled()) {
                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                }


                if (plugin.getAutoBuyManager().getPlayerData(plugin, playerId).isAutoBuyEnabled()) {
                    global_auto_sell_toggle_string = enabled;

                } else {
                    global_auto_sell_toggle_string = disabled;

                }




                meta.setLore(button.getLoreButton().stream()
                        .map(s -> hex(setPlaceholders(player, s)))
                        .map(s -> s.replace("%price%", String.valueOf(price)))
                        .map(s -> s.replace("%coefficient%", String.valueOf(coefficient)))
                        .map(s -> s.replace("%score%", String.valueOf(score)))
                        .map(s -> s.replace("%sell_pay%", String.valueOf(count)))
                        .map(s -> s.replace("%price_with_coefficient%", String.valueOf(price_with_coefficient)))
                        .map(s -> s.replace("%auto_sell_toggle_state%", hex(autoBuyList != null && autoBuyList.contains(button.getMaterialButton().name())
                                ? CFG().getString("autoBuy.enable", "&aВключён")
                                : CFG().getString("autoBuy.disable", "&cВыключён"))))
                        .map(s -> s.replace("%global_auto_sell_toggle_state%", hex(global_auto_sell_toggle_string)))
                        .toList());

                itemStack.setItemMeta(meta);
            }
            topInventory.setItem(button.getSlotButton(), itemStack);
        }
    }


    public void openMenu(Player player, String menuName) {
        Menus menu = listMenus.get(menuName);
        if (menu == null) {
            player.sendMessage("Меню не найдено!");
            return;
        }

        Inventory inventory = Bukkit.createInventory(menu, menu.getSize(), hex(menu.getTitle()));

        if (!(inventory.getHolder() instanceof Menus)) {
            return;
        }



        for (MenuButtons btn : menu.getButtons()) {
            List<ItemStack> itemStacks = new ArrayList<>();
            sellZone.checkItem(itemStacks, plugin.getItemPrice(), player);


            updateMenu(btn, inventory, sellZone.getCountPlayerString(player.getUniqueId(), 0), player);

        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(inventory), 1L);
    }

    public Map<String, Menus> getListMenu(){
        return listMenus;
    }

}
