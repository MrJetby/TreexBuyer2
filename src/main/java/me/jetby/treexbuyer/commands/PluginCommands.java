package me.jetby.treexbuyer.commands;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.MenuLoader;
import me.jetby.treexbuyer.menus.MenusManager;
import me.jetby.treexbuyer.functions.AutoBuyManager;
import me.jetby.treexbuyer.functions.BoostManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.jetby.treexbuyer.configurations.Config.CFG;
import static me.jetby.treexbuyer.tools.Hex.hex;

public class PluginCommands implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final MenusManager menusManager;
    private final AutoBuyManager autoBuyManager;
    private final BoostManager boostManager;
    private final MenuLoader menuLoader;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public PluginCommands(Main plugin){
        this.plugin = plugin;
        this.menusManager = plugin.getMenusManager();
        this.autoBuyManager = plugin.getAutoBuyManager();
        this.boostManager = plugin.getBoostManager();
        this.menuLoader = plugin.getMenuLoader();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {

        if (!sender.hasPermission("treexbuyer.admin")) {
            return true;
        }


        if (args.length==0) {

            sender.sendMessage(hex("&b/tbuyer reload"));
            sender.sendMessage(hex("&b/tbuyer open <id> <ник>"));
            sender.sendMessage(hex("&b/tbuyer score give <ник> <кол-во>"));
            sender.sendMessage(hex("&b/tbuyer score take <ник> <кол-во>"));
            sender.sendMessage(hex("&b/tbuyer score set <ник> <кол-во>"));


            return true;
        }


        if (args[0].equalsIgnoreCase("reload")) {


            if (plugin.getDatabase() != null) {
                for (UUID uuid : boostManager.getCachedScores().keySet()) {
                    boostManager.savePlayerScoresSync(uuid);
                }

                try {
                    plugin.getDatabase().getDbExecutor().shutdown();
                    if (!plugin.getDatabase().getDbExecutor().awaitTermination(5, TimeUnit.SECONDS)) {
                        plugin.getDatabase().getDbExecutor().shutdownNow();
                    }

                    plugin.getDatabase().restartExecutor();
                    plugin.getDatabase().initDatabase();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            plugin.getConfigLoader().reloadCfg(plugin);
            plugin.getPriceItemCfg().reloadCfg(plugin);
            String path = CFG().getString("priceItem.path", "priceItem.yml");
            File itemFile = new File(plugin.getDataFolder(), path);
            plugin.setItemPrice(plugin.getPriseItemLoader().loadItemValuesFromFile(itemFile));
            plugin.getMenuLoader().loadMenus(plugin.getDataFolder());

            for (Player player : Bukkit.getOnlinePlayers()) {
                autoBuyManager.loadPlayerData(player.getUniqueId());
                boostManager.loadPlayersScores(player.getUniqueId());
            }
            boostManager.loadBoosts();
            plugin.createCommand();
            plugin.setupEconomy();


            if (sender instanceof Player player) {
                player.sendMessage(hex("&aУспешная перезагрузка."));
            } else {
                sender.sendMessage("Успешная перезагрузка.");
            }


            return true;
        }

        if (args[0].equalsIgnoreCase("score")) {
            if (args.length<4) {
                sender.sendMessage(hex("&b/tbuyer score give <ник> <кол-во>"));
                sender.sendMessage(hex("&b/tbuyer score take <ник> <кол-во>"));
                sender.sendMessage(hex("&b/tbuyer score set <ник> <кол-во>"));
                return true;
            }
            if (args[1].equalsIgnoreCase("give")) {
                Player player = Bukkit.getOfflinePlayer(args[2]).getPlayer();

                if (player != null) {
                    boostManager.addPlayerScores(player, Double.parseDouble(df.format(Double.parseDouble(args[3]))));
                    sender.sendMessage(hex("&b&lTreexBuyer &7▶ &aУспешная выдача. &fТекущий буст игрока " + player.getName() + " составляет x" + boostManager.getPlayerCoefficient(player)));
                } else {
                    sender.sendMessage(hex("&b&lTreexBuyer &7▶ &cИгрок не найден."));
                }

                return true;
            }
            if (args[1].equalsIgnoreCase("take")) {
                Player player = Bukkit.getOfflinePlayer(args[2]).getPlayer();

                if (player != null) {
                    boostManager.removePlayerScores(player, Double.parseDouble(df.format(Double.parseDouble(args[3]))));
                    sender.sendMessage(hex("&b&lTreexBuyer &7▶ &aУспешно отнято. &fТекущий буст игрока " + player.getName() + " составляет x" + boostManager.getPlayerCoefficient(player)));
                } else {
                    sender.sendMessage(hex("&b&lTreexBuyer &7▶ &cИгрок не найден."));

                }

                return true;
            }
            if (args[1].equalsIgnoreCase("set")) {
                Player player = Bukkit.getOfflinePlayer(args[2]).getPlayer();

                if (player != null) {
                    boostManager.setPlayerScores(player, Double.parseDouble(df.format(Double.parseDouble(args[3]))));
                    sender.sendMessage(hex("&b&lTreexBuyer &7▶ &aУспешно. &fТекущий буст игрока " + player.getName() + " составляет x" + boostManager.getPlayerCoefficient(player)));
                } else {
                    sender.sendMessage(hex("&b&lTreexBuyer &7▶ &cИгрок не найден."));

                }

                return true;
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {


            if (args.length==2) {
                if (sender instanceof Player player) {
                    menusManager.openMenu(player, args[1]);
                } else {
                    sender.sendMessage("Данная команда доступа только для игроков.");
                }


            } else if (args.length>2) {
                Player player = Bukkit.getPlayer(args[2]);
                if (player!=null) {
                    menusManager.openMenu(player, args[1]);
                } else {
                    if (sender instanceof Player p) p.sendMessage(hex("&b&lTreexBuyer &7▶ &cИгрок не найден."));
                    else {
                        sender.sendMessage("[TreexBuyer] ▶ Игрок не найден.");
                    }
                }

            } else {
                if (sender instanceof Player player) {
                    player.sendMessage(hex("&b&lTreexBuyer &7▶ &fИспользование &b/" + cmd.getName() + " open <id> <игрок>"));
                } else {
                    sender.sendMessage("[TreexBuyer] ▶ Использование /" + cmd.getName() + " open <id> <игрок>");
                }

            }

        }


        return true;

    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender instanceof Player player) {

            if (args.length == 1) {
                completions.add("open");
                completions.add("score");
                completions.add("reload");
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
                if (player.hasPermission("treexbuyer.admin")) {
                    return menuLoader.getListMenu().keySet().stream()
                            .filter(menuId -> menuId.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("score")) {
                if (player.hasPermission("treexbuyer.admin")) {
                    completions.add("take");
                    completions.add("give");
                    completions.add("set");
                }
            }
        }

        return completions;
    }
}
