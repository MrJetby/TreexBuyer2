package me.jetby.treexbuyer.listeners;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.functions.AutoBuyManager;
import me.jetby.treexbuyer.functions.BoostManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DataLoader implements Listener {

    private final AutoBuyManager autoBuyManager;
    private final BoostManager boostManager;

    public DataLoader(Main plugin) {
        this.autoBuyManager = plugin.getAutoBuyManager();
        this.boostManager = plugin.getBoostManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        autoBuyManager.loadPlayerData(e.getPlayer().getUniqueId());
        boostManager.loadPlayersScores(e.getPlayer().getUniqueId());

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {

        boostManager.savePlayerScores(e.getPlayer().getUniqueId());
        boostManager.getCachedScores().remove(e.getPlayer().getUniqueId());
        autoBuyManager.unloadPlayerData(e.getPlayer().getUniqueId());

    }
}
