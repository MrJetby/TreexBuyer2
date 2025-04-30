package me.jetby.treexbuyer.menus;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.PriseItemLoader;
import me.jetby.treexbuyer.functions.AutoBuyManager;
import me.jetby.treexbuyer.functions.BoostManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.jetby.treexbuyer.configurations.Config.CFG;
import static me.jetby.treexbuyer.tools.Hex.hex;

public class ClickHandler implements Listener {

    private final Main plugin;
    private final SellZone sellZone;
    private final MenusManager menusManager;
    private final BoostManager boostManager;
    private final AutoBuyManager autoBuyManager;

    public ClickHandler(Main plugin) {
        this.plugin = plugin;
        this.sellZone = plugin.getSellZone();
        this.menusManager = plugin.getMenusManager();
        this.boostManager = plugin.getBoostManager();
        this.autoBuyManager = plugin.getAutoBuyManager();
    }

    private final DecimalFormat df = new DecimalFormat("#.##");


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getInventory();
        if (!(topInventory.getHolder() instanceof Menus menu)) {
            return;
        }


        if (event.isShiftClick() && clickedInventory.equals(player.getInventory())) {
            boolean foundSellZone = false;



            for (MenuButtons button : menu.getButtons()) {
                if (button.isSellZone()) {
                    ItemStack itemInSlot = topInventory.getItem(button.getSlotButton());
                    if (itemInSlot == null) {
                        foundSellZone = true;
                        break;
                    }
                }
            }

            if (!foundSellZone) {
                event.setCancelled(true);
                return;
            }
        }


        double totalMoney = 0;
        double totalScores = 0;
        double finalTotalScores = totalScores;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            List<ItemStack> itemStacks = new ArrayList<>();
            for (int i = 0; i < topInventory.getSize(); i++) {
                ItemStack itemStack = topInventory.getItem(i);
                if (itemStack != null) {
                    for (MenuButtons btn : menu.getButtons()) {
                        if (btn.getSlotButton() == i && btn.isSellZone()) {
                            itemStacks.add(itemStack);
                            break;
                        }
                    }
                }
            }

            sellZone.checkItem(itemStacks, plugin.getItemPrice(), player);
            for (MenuButtons btn : menu.getButtons()) {
                menusManager.updateMenu(btn,
                        topInventory,
                        sellZone.getCountPlayerString(player.getUniqueId(), Integer.valueOf(String.valueOf(df.format(finalTotalScores)))),
                        player);
            }
        }, 10L);



        boolean isSellZoneSlot = false;
        for (MenuButtons button : menu.getButtons()) {
            if (button.isSellZone() && event.getSlot() == button.getSlotButton()) {
                isSellZoneSlot = true;
                break;
            }
        }

        if (!isSellZoneSlot) event.setCancelled(true);

        if (clickedInventory.equals(topInventory)) {

            for (MenuButtons button : menu.getButtons()) {
                if (event.getSlot() == button.getSlotButton()) {


                    if (button.isSellZone()) return;


                    List<String> allCommands = button.getAllCommands();

                    for (String command : allCommands) {
                        if (command.equalsIgnoreCase("[sell_all]")) {
                            event.setCancelled(true);

                            List<ItemStack> itemsToRemove = new ArrayList<>();

                            for (MenuButtons btn : menu.getButtons()) {
                                if (btn.isSellZone()) {
                                    ItemStack item = topInventory.getItem(btn.getSlotButton());
                                    if (item != null) {

                                        boolean isRegularItem = isIsRegularItem(item);

                                        if (isRegularItem) {
                                            PriseItemLoader.ItemData itemData = plugin.getItemPrice(item.getType().name());
                                            if (itemData != null) {
                                                double price = itemData.price() * boostManager.getPlayerCoefficient(player);
                                                int addScores = itemData.addScores();

                                                totalMoney += price * item.getAmount();
                                                totalScores += addScores * item.getAmount();
                                                itemsToRemove.add(item);

                                            }
                                        }
                                    }
                                }
                            }

                            for (ItemStack item : itemsToRemove) {
                                for (MenuButtons btn : menu.getButtons()) {
                                    if (btn.isSellZone() && item.equals(topInventory.getItem(btn.getSlotButton()))) {
                                        topInventory.setItem(btn.getSlotButton(), null);
                                        break;
                                    }
                                }
                            }

                            if (totalMoney > 0 || totalScores > 0) {
                                plugin.getEconomy().depositPlayer(player, totalMoney);
                                boostManager.addPlayerScores(player, totalScores);
                                player.sendMessage(hex(CFG().getString("completeSaleMessage", "&eВы продали предметы на сумму: &a%sell_pay% &eи получили &b%sell_score% очков")
                                        .replace("%sell_pay%", String.valueOf(totalMoney))
                                        .replace("%sell_score%", String.valueOf(totalScores))));
                            } else {
                                player.sendMessage(hex(CFG().getString("noItemsToSellMessage", "У вас нет предметов для продажи")));
                            }
                        } else {
                            event.setCancelled(true);
                            Map<ClickType, List<String>> commandsMap = button.getCommands();

                            if (commandsMap==null) {
                                return;
                            }

                            ClickType clickType = event.getClick();
                            
                            List<String> commands = commandsMap.get(clickType);

                            if (commands==null) {
                                return;
                            }

                            for (String actions : commands) {
                                executeCommand(player, actions, button);
                            }

                        }

                        break;
                    }
                    return;
                }
            }

        }
    }

    public static boolean isIsRegularItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        return meta != null &&
                !(meta.hasDisplayName() && !meta.getDisplayName().isEmpty()) &&
                !meta.hasLore() &&
                !meta.hasAttributeModifiers() &&
                !meta.hasEnchants() &&
                !meta.hasCustomModelData();
    }

    private void sell_item(Player player, String type, ItemStack itemStack) {
        double sumCount = 0d;
        int totalScores = 0;
        ItemStack air = new ItemStack(Material.AIR);
        Material targetType = itemStack.getType();

        if (type.equalsIgnoreCase("all")) {
            for (ItemStack var : player.getInventory().getContents()) {
                if (var != null && targetType.equals(var.getType())) {
                    int amount = var.getAmount();

                    PriseItemLoader.ItemData itemData = plugin.getItemPrice(targetType.name());
                    if (itemData != null && itemData.price() > 0d) {
                        double totalPrice = itemData.price() * amount;
                        int scores = itemData.addScores() * amount;

                        plugin.getEconomy().depositPlayer(player, totalPrice);
                        sumCount += totalPrice * boostManager.getPlayerCoefficient(player);
                        totalScores += scores;

                        player.getInventory().removeItem(new ItemStack(targetType, amount));
                    }
                }
            }
        } else {
            int amount;
            try {
                amount = Integer.parseInt(type);
            } catch (NumberFormatException e) {
                player.sendMessage("§cОшибка: неверный формат количества. Укажите число или 'all'.");
                return;
            }

            for (ItemStack var : player.getInventory().getContents()) {
                if (var != null && targetType.equals(var.getType()) && var.getAmount() >= amount) {
                    ItemStack item = new ItemStack(targetType, amount);
                    PriseItemLoader.ItemData itemData = plugin.getItemPrice(targetType.name());

                    if (itemData != null && itemData.price() > 0d) {
                        double totalPrice = itemData.price() * amount;
                        int scores = itemData.addScores() * amount;

                        plugin.getEconomy().depositPlayer(player, totalPrice);
                        sumCount += totalPrice * boostManager.getPlayerCoefficient(player);
                        totalScores += scores;

                        // Удаление предмета из слотов брони и оффхэнда
                        PlayerInventory inv = player.getInventory();
                        if (player.getEquipment().getItemInOffHand() != null && player.getEquipment().getItemInOffHand().isSimilar(item))
                            player.getEquipment().setItemInOffHand(air);
                        if (inv.getHelmet() != null && inv.getHelmet().isSimilar(item)) inv.setHelmet(air);
                        if (inv.getChestplate() != null && inv.getChestplate().isSimilar(item)) inv.setChestplate(air);
                        if (inv.getLeggings() != null && inv.getLeggings().isSimilar(item)) inv.setLeggings(air);
                        if (inv.getBoots() != null && inv.getBoots().isSimilar(item)) inv.setBoots(air);

                        inv.removeItem(item);
                    }
                    break;
                }
            }
        }

        if (sumCount > 0d) {
            player.sendMessage(hex(CFG().getString("autoBuy.message", "&aВы успешно продали все предметы на сумму &f%sum%")
                    .replace("%sum%", String.valueOf(sumCount))
                    .replace("%score%", String.valueOf(totalScores))
            ));
        } else {
            player.sendMessage(CFG().getString("noItemsToSellMessage", "§cУ вас не достаточно предметов для продажи."));
        }

        if (totalScores > 0) {
            boostManager.addPlayerScores(player, totalScores);
        }
    }


    private void executeCommand(Player player, String command, MenuButtons button) {
        String[] args = command.split(" ");
        String withoutCMD = command.replace(args[0] + " ", "");
        if (args[0].equalsIgnoreCase("[sell_item]")) {
            sell_item(player, withoutCMD, new ItemStack(button.getMaterialButton()));
            return;
        }
        UUID playerId = player.getUniqueId();

        switch (command.toLowerCase()) {

            case "[autobuy_item_toggle]": {
                String materialName = button.getMaterialButton().name();

                autoBuyManager.getPlayerData(plugin, playerId).toggleItem(materialName);

                menusManager.updateMenu(button, player.getOpenInventory().getTopInventory(),
                        sellZone.getCountPlayerString(playerId, 0), player);

                break;
            }



            case "[autobuy_status_toggle]": {
                autoBuyManager.getPlayerData(plugin, playerId).setAutoBuyEnabled(
                        !autoBuyManager.getPlayerData(plugin, playerId).isAutoBuyEnabled()
                );

                menusManager.updateMenu(button, player.getOpenInventory().getTopInventory(),
                        sellZone.getCountPlayerString(playerId, 0), player);
                break;
            }


            case "[enable_all]": {
                for (ItemStack content : player.getOpenInventory().getTopInventory().getContents()) {
                    if (content == null) continue;
                    ItemMeta contentMeta = content.getItemMeta();
                    if (contentMeta == null) continue;
                    if (contentMeta.getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.INTEGER)) continue;
                    autoBuyManager.getPlayerData(plugin, playerId).addItem(content.getType().name());
                }
                menusManager.updateMenu(button, player.getOpenInventory().getTopInventory(),
                        sellZone.getCountPlayerString(playerId, 0), player);
                break;
            }


            case "[disable_all]": {
                for (ItemStack content : player.getOpenInventory().getTopInventory().getContents()) {
                    if (content == null) continue;
                    ItemMeta contentMeta = content.getItemMeta();
                    if (contentMeta == null) continue;
                    if (contentMeta.getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.INTEGER)) continue;
                    autoBuyManager.getPlayerData(plugin, playerId).removeItem(content.getType().name());
                }
                break;
            }

            default: {
                plugin.getActions().execute(player, command);
                break;
            }

        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getInventory();

        if (!(topInventory.getHolder() instanceof Menus menu)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        for (Integer slot : event.getRawSlots()) {
            if (slot >= topInventory.getSize()) {
                continue;
            }

            boolean isAllowedSlot = false;

            for (MenuButtons button : menu.getButtons()) {
                if (slot.equals(button.getSlotButton()) && button.isSellZone()) {
                    isAllowedSlot = true;
                    break;
                }
            }

            if (!isAllowedSlot) {
                event.setCancelled(true);
                return;
            }
        }

        List<ItemStack> itemStacks = new ArrayList<>();
        for (int i = 0; i < topInventory.getSize(); i++) {
            ItemStack itemStack = topInventory.getItem(i);
            if (itemStack != null) {

                for (MenuButtons btn : menu.getButtons()) {
                    if (btn.getSlotButton() == i && btn.isSellZone()) {
                        itemStacks.add(itemStack);
                        break;
                    }
                }
            }
        }
        sellZone.checkItem(itemStacks, plugin.getItemPrice(), player);
        for (MenuButtons btn : menu.getButtons()) {
            menusManager.updateMenu(btn, topInventory, sellZone.getCountPlayerString(player.getUniqueId(), 0), player);
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Inventory topInventory = event.getInventory();
        if (!(topInventory.getHolder() instanceof Menus menu)) {
            return;
        }

        menu.getButtons().stream()
                .filter(MenuButtons::isSellZone)
                .forEach(button -> {
                    ItemStack item = topInventory.getItem(button.getSlotButton());
                    if (item != null && !item.getItemMeta().getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING)) {
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItem(player.getLocation(), item);
                        } else {
                            player.getInventory().addItem(item);
                        }
                        topInventory.setItem(button.getSlotButton(), null);
                    }
                });

        sellZone.getCountPlayer().remove(playerUUID);
    }

    public static NamespacedKey NAMESPACED_KEY = new NamespacedKey("treexbuyer", "key");




}
