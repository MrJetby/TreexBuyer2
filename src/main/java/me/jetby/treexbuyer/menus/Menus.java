package me.jetby.treexbuyer.menus;

import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class Menus implements InventoryHolder {
    String id;
    String title;
    Integer size;
    List<String> open_commands;
    String open_permission;
    List<MenuButtons> buttons;

    public Menus(String id, String title, Integer size, List<String> open_commands, String open_permission, List<MenuButtons> buttons) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.open_commands = open_commands;
        this.open_permission = open_permission;
        this.buttons = buttons;
    }
    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
