package me.jetby.treexbuyer.tools;

import me.jetby.treexbuyer.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Version implements Listener {
    private final Main plugin;
    private final String versionLink = "https://raw.githubusercontent.com/MrJetby/TreexBuyer2/refs/heads/master/VERSION";
    private final String updateLink = "https://raw.githubusercontent.com/MrJetby/TreexBuyer2/refs/heads/master/UPDATE_LINK";

    public Version(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("treexbuyer.version")) {
            if (!isLastVersion()) {
                player.sendMessage("");
                player.sendMessage("§7-------- §6TreexBuyer §7--------");
                player.sendMessage("§6● §fВнимание, доступно обновление, пожалуйста обновите плагин.");
                player.sendMessage("§6● §7Ваша версия: §c" + getVersion() + " §7а последняя §a" + getLastVersion());
                player.sendMessage("");
                player.sendMessage("§6● §fСкачать тут: §b"+getUpdateLink());
                player.sendMessage("§7-------------------------");
                player.sendMessage("");
            }
        }
    }

    public List<String> getAlert() {
        List<String> oldVersion = new ArrayList<>(List.of(
                "",
                "§7-------- §6TreexBuyer §7--------",
                "§6● §fВнимание, доступно обновление, пожалуйста обновите плагин.",
                "§6● §7Ваша версия: §c" + getVersion() + " §7а последняя §a" + getLastVersion(),
                "",
                "§6● §fСкачать тут: §b" + getUpdateLink(),
                "§7-------------------------",
                ""
        ));
        List<String> lastVersion = new ArrayList<>(List.of(
                "",
                "§7-------- §6TreexBuyer §7--------",
                "§6● §7Версия плагин: §c" + getVersion(),
                "",
                "§6● §aВы используете последнюю версию ✔",
                "",
                "§7-------------------------",
                ""
        ));

        if (!isLastVersion()) {
            return oldVersion;
        }
        return lastVersion;
    }

    private String getRaw(String link) {
        try {
            URL url = new URL(link);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder builder = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            in.close();
            return builder.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getLastVersion() {
        String result = getRaw(versionLink);
        assert result != null;
        return result;
    }
    public String getUpdateLink() {
        String result = getRaw(updateLink);
        assert result != null;
        return result;
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean isLastVersion() {
        String result = getRaw(versionLink);
        if (result == null) {
            return true;
        }

        return plugin.getDescription().getVersion().equalsIgnoreCase(getLastVersion());
    }

}
