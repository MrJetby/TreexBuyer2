package me.jetby.treexbuyer.menus;

import lombok.Getter;
import me.jetby.treexbuyer.tools.SkullCreator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class MenuButtons {

    Integer slotButton;
    String TitleButton;
    boolean hide_enchantments;
    boolean hide_attributes;
    boolean enchanted;
    List<String> loreButton;
    String materialButton;
    List<String> command;
    Map<ClickType, List<String>> commands;

    public MenuButtons(Integer slotButton,
                      String titleButton,
                      List<String> loreButton,
                      String materialButton,
                      Map<ClickType, List<String>> commands,
                      List<String> command,
                      boolean hide_enchantments,
                      boolean hide_attributes,
                      boolean enchanted) {
        this.slotButton = slotButton;
        this.TitleButton = titleButton;
        this.loreButton = loreButton;
        this.command = command;
        this.materialButton = materialButton;
        this.commands = commands;
        this.hide_enchantments = hide_enchantments;
        this.hide_attributes = hide_attributes;
        this.enchanted = enchanted;
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
