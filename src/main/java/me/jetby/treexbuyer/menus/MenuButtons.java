package me.jetby.treexbuyer.menus;

import lombok.Getter;
import me.jetby.treexbuyer.tools.SkullCreator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MenuButtons {

    String buttonKey;
    Integer slotButton;
    String TitleButton;
    boolean hide_enchantments;
    boolean hide_attributes;
    boolean enchanted;
    List<String> loreButton;
    String materialButton;
    List<String> command;
    Map<ClickType, List<String>> commands;
    private final Map<String, ClickRequirement> clickRequirements;
    private final Map<ClickType, Map<String, ClickRequirement>> clickTypeRequirements;

    public MenuButtons(String buttonKey, Integer slotButton,
                      String titleButton,
                      List<String> loreButton,
                      String materialButton,
                      Map<ClickType, List<String>> commands,
                      List<String> command,
                      boolean hide_enchantments,
                      boolean hide_attributes,
                      boolean enchanted,
                       Map<String, ClickRequirement> clickRequirements,
                       Map<ClickType, Map<String, ClickRequirement>> clickTypeRequirements) {
        this.buttonKey = buttonKey;
        this.slotButton = slotButton;
        this.TitleButton = titleButton;
        this.loreButton = loreButton;
        this.command = command;
        this.materialButton = materialButton;
        this.commands = commands;
        this.hide_enchantments = hide_enchantments;
        this.hide_attributes = hide_attributes;
        this.enchanted = enchanted;
        this.clickRequirements = clickRequirements != null ? clickRequirements : new HashMap<>();
        this.clickTypeRequirements = clickTypeRequirements != null ? clickTypeRequirements : new HashMap<>();
    }


    public boolean checkRequirements(Player player, ClickType clickType) {
        if (!checkGeneralRequirements(player)) {
            return false;
        }

        if (clickType != null && clickTypeRequirements.containsKey(clickType)) {
            for (ClickRequirement requirement : clickTypeRequirements.get(clickType).values()) {
                if (!requirement.check(player)) {
                    requirement.runDenyCommands(player);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkGeneralRequirements(Player player) {
        if (clickRequirements.isEmpty()) {
            return true;
        }

        for (ClickRequirement requirement : clickRequirements.values()) {
            if (!requirement.check(player)) {
                requirement.runDenyCommands(player);
                return false;
            }
        }
        return true;
    }

    public Material getMaterialButton() {

        if (materialButton.startsWith("basehead-")) {
            return Material.valueOf(SkullCreator.createSkull().getType().name());

        }


        return Material.valueOf(materialButton);
    }

    public ItemStack getItemStackofMaterial() {

        ItemStack itemStack;
        if (materialButton.startsWith("basehead-")) {
            try {
                itemStack = SkullCreator.itemFromBase64(materialButton.replace("basehead-", ""));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при создании кастомного черепа: " + e.getMessage());
                itemStack = new ItemStack(Material.PLAYER_HEAD);
            }
        } else {
            itemStack = new ItemStack(Material.valueOf(materialButton));
        }

        return itemStack;
    }



    public List<String> getAllCommands() {
        List<String> allCommands = new ArrayList<>(command);
        for (List<String> commands : this.commands.values()) {
            allCommands.addAll(commands);
        }
        return allCommands;
    }

    public boolean isSellZone() {
        return command.contains("[sell_zone]");
    }

}
