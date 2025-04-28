package me.jetby.treexbuyer.tools;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.functions.BoostManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final Main plugin;
    private final BoostManager boostManager;

    public PlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
        this.boostManager = plugin.getBoostManager();
    }

    @Override
    public @NotNull String getAuthor() {
        return null;
    }
    @Override
    public boolean persist() {
        return true;
    }
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    @Override
    public String getIdentifier() {

        return "treexbuyer";
    }
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {


        if (identifier.equalsIgnoreCase("score")) {

            return String.valueOf(boostManager.getCachedScores(player));

        }

        if (identifier.equalsIgnoreCase("coefficient")) {

            return String.valueOf(boostManager.getPlayerCoefficient(player));

        }
            return null;
        }
}
