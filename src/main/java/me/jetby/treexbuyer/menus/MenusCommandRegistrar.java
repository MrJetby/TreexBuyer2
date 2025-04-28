package me.jetby.treexbuyer.menus;

import me.jetby.treexbuyer.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenusCommandRegistrar implements CommandExecutor {

    private final Main plugin;

    public MenusCommandRegistrar(Main plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }
        plugin.getMenusManager().getListMenu().forEach((key, vault) ->{
            vault.getOpen_commands().forEach(item -> {
                if (item.equals(label)){
                    plugin.getMenusManager().openMenu(player, key);
                }
            });
        });

        return false;
    }
}
