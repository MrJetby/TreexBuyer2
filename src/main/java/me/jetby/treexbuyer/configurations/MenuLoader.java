package me.jetby.treexbuyer.configurations;

import lombok.Getter;
import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.newcfg.Config;
import me.jetby.treexbuyer.menus.ClickRequirement;
import me.jetby.treexbuyer.menus.MenuButtons;
import me.jetby.treexbuyer.menus.Menus;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MenuLoader {

    private final Main plugin;
    private final Config config;

    public MenuLoader(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getCfg();
    }

    @Getter
    public final Map<String, Menus> listMenu = new HashMap<>();

    public void loadMenus(File dataFolder) {


        for (String menuId : config.getMenus().keySet()) {
            File menuFile = new File(dataFolder, config.getMenus().get(menuId));

            if (menuFile.exists()) {
                loadMenuFromFile(menuId, menuFile);
            } else {
                plugin.getDataFolder().mkdirs();
                plugin.saveResource("Menu/"+menuId+".yml", true);
                loadMenuFromFile(menuId, menuFile);
            }
        }
    }

    private void loadMenuFromFile(String menuId, File menuFile) {

        FileConfiguration menuConfig = YamlConfiguration.loadConfiguration(menuFile);



        String titleMenu = menuConfig.getString("title");
        int size = menuConfig.getInt("size");
        List<String> commandOpenMenu = menuConfig.getStringList("open_commands");
        String permissionOpenMenu = menuConfig.getString("open_permission");


        List<MenuButtons> buttons = new ArrayList<>();
        if (menuConfig.contains("Items")) {
            for (String buttonKey : menuConfig.getConfigurationSection("Items").getKeys(false)) {
                String materialName = menuConfig.getString("Items." + buttonKey + ".material");

                Object slotString = menuConfig.get("Items." + buttonKey + ".slot");
                List<Integer> slots = parseSlots(slotString);


                String titleButton = menuConfig.getString("Items." + buttonKey + ".display_name");
                List<String> loreButton = menuConfig.getStringList("Items." + buttonKey + ".lore");

                boolean hide_enchantments = menuConfig.getBoolean("Items." + buttonKey + ".hide_enchantments", false);
                boolean hide_attributes = menuConfig.getBoolean("Items." + buttonKey + ".hide_attributes", false);
                boolean enchanted = menuConfig.getBoolean("Items." + buttonKey + ".enchanted", false);

                Map<ClickType, List<String>> commandsMap = new HashMap<>();

                addCommandsToMap(commandsMap, ClickType.LEFT, menuConfig.getStringList("Items." + buttonKey + ".left_click_commands"));
                addCommandsToMap(commandsMap, ClickType.RIGHT, menuConfig.getStringList("Items." + buttonKey + ".right_click_commands"));
                addCommandsToMap(commandsMap, ClickType.MIDDLE, menuConfig.getStringList("Items." + buttonKey + ".middle_click_commands"));
                addCommandsToMap(commandsMap, ClickType.SHIFT_LEFT, menuConfig.getStringList("Items." + buttonKey + ".shift_left_click_commands"));
                addCommandsToMap(commandsMap, ClickType.SHIFT_RIGHT, menuConfig.getStringList("Items." + buttonKey + ".shift_right_click_commands"));
                addCommandsToMap(commandsMap, ClickType.DROP, menuConfig.getStringList("Items." + buttonKey + ".drop_commands"));

                List<String> oldCommands = menuConfig.getStringList("Items." + buttonKey + ".click_commands");
                if (!oldCommands.isEmpty()) {

                    if (!commandsMap.containsKey(ClickType.LEFT)) {
                        addCommandsToMap(commandsMap, ClickType.LEFT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.RIGHT)) {
                        addCommandsToMap(commandsMap, ClickType.RIGHT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.SHIFT_LEFT)) {
                        addCommandsToMap(commandsMap, ClickType.SHIFT_LEFT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.SHIFT_RIGHT)) {
                        addCommandsToMap(commandsMap, ClickType.SHIFT_RIGHT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.MIDDLE)) {
                        addCommandsToMap(commandsMap, ClickType.MIDDLE, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.DROP)) {
                        addCommandsToMap(commandsMap, ClickType.DROP, oldCommands);
                    }

                }


                Map<String, ClickRequirement> requirements = loadRequirements(menuConfig, "Items." + buttonKey + ".click_requirement");
                Map<ClickType, Map<String, ClickRequirement>> clickTypeRequirements = new HashMap<>();
                clickTypeRequirements.put(ClickType.LEFT, loadRequirements(menuConfig, "Items." + buttonKey + ".left_click_requirement"));
                clickTypeRequirements.put(ClickType.RIGHT, loadRequirements(menuConfig, "Items." + buttonKey + ".right_click_requirement"));
                clickTypeRequirements.put(ClickType.SHIFT_LEFT, loadRequirements(menuConfig, "Items." + buttonKey + ".shift_left_click_requirement"));
                clickTypeRequirements.put(ClickType.SHIFT_RIGHT, loadRequirements(menuConfig, "Items." + buttonKey + ".shift_right_click_requirement"));
                clickTypeRequirements.put(ClickType.DROP, loadRequirements(menuConfig, "Items." + buttonKey + ".drop_requirement"));

                for (int slot : slots) {
                    MenuButtons menuButton = new MenuButtons(buttonKey,
                            slot,
                            titleButton,
                            loreButton,
                            materialName,
                            commandsMap,
                            oldCommands,
                            hide_enchantments,
                            hide_attributes,
                            enchanted,
                            requirements, clickTypeRequirements);
                    buttons.add(menuButton);
                }
            }
        }

        Menus menu = new Menus(menuId, titleMenu, size, commandOpenMenu, permissionOpenMenu, buttons);
        listMenu.put(menuId, menu);
    }

    private Map<String, ClickRequirement> loadRequirements(FileConfiguration config, String path) {
        Map<String, ClickRequirement> requirements = new HashMap<>();
        if (config.contains(path)) {
            for (String reqName : config.getConfigurationSection(path).getKeys(false)) {
                String type = config.getString(path + "." + reqName + ".type");
                String input = config.getString(path + "." + reqName + ".input");
                String permission = config.getString(path + "." + reqName + ".permission");
                String output = config.getString(path + "." + reqName + ".output");
                List<String> denyCommands = config.getStringList(path + "." + reqName + ".deny_commands");

                if (type != null && input != null) {
                    requirements.put(reqName, new ClickRequirement(plugin,
                            type, input, permission, output, denyCommands));
                }
            }
        }
        return requirements;
    }
    private void addCommandsToMap(Map<ClickType, List<String>> map, ClickType clickType, List<String> commands) {
        if (commands != null && !commands.isEmpty()) {
            map.put(clickType, commands);
        }
    }

    public List<Integer> parseSlots(Object slotObject) {
        List<Integer> slots = new ArrayList<>();

        if (slotObject instanceof Integer) {
            slots.add((Integer) slotObject);
        } else if (slotObject instanceof String) {
            String slotString = ((String) slotObject).trim();
            slots.addAll(parseSlotString(slotString));
        } else if (slotObject instanceof List<?>) {
            for (Object obj : (List<?>) slotObject) {
                if (obj instanceof Integer) {
                    slots.add((Integer) obj);
                } else if (obj instanceof String) {
                    slots.addAll(parseSlotString((String) obj));
                }
            }
        } else {
            Bukkit.getLogger().warning("Неизвестный формат слотов: " + slotObject);
        }

        return slots;
    }

    private List<Integer> parseSlotString(String slotString) {
        List<Integer> slots = new ArrayList<>();
        if (slotString.contains("-")) {
            try {
                String[] range = slotString.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Ошибка парсинга диапазона слотов: " + slotString);
            }
        } else {
            try {
                slots.add(Integer.parseInt(slotString));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Ошибка парсинга одиночного слота: " + slotString);
            }
        }
        return slots;
    }


}
