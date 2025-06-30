package me.jetby.treexbuyer.menus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.jetby.treexbuyer.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

import static me.jetby.treexbuyer.tools.Hex.setPlaceholders;

@Getter
@AllArgsConstructor
public class ClickRequirement {
    private final Main plugin;
    private final String type;
    private final String input;
    private final String permission;
    private final String output;
    private final List<String> denyCommands;

    public boolean check(Player player) {
        String input = setPlaceholders(player, this.input);
        String output = setPlaceholders(player, this.output);

        if (type.equalsIgnoreCase("has permission")) {
            return player.hasPermission(setPlaceholders(player, permission));
        }
        if (type.equalsIgnoreCase("string equals")) {
            return input.equalsIgnoreCase(output);
        }
        else if (type.equalsIgnoreCase("javascript")) {
            String[] args = input.split(" ");
            if (args.length < 3) return false;

            args[0] = setPlaceholders(player, args[0]);
            args[2] = setPlaceholders(player, args[2]);

            try {
                int x = Integer.parseInt(args[0]);
                int x1 = Integer.parseInt(args[2]);

                return switch (args[1]) {
                    case ">" -> x > x1;
                    case ">=" -> x >= x1;
                    case "==" -> x == x1;
                    case "!=" -> x != x1;
                    case "<=" -> x <= x1;
                    case "<" -> x < x1;
                    default -> false;
                };
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public void runDenyCommands(Player player) {
        for (String command : denyCommands) {
            plugin.getActions().execute(player, command);
        }
    }
}