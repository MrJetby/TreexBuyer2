package me.jetby.treexbuyer.functions;


import lombok.Getter;
import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.tools.Database;
import org.bukkit.Bukkit;

import java.util.*;

public class AutoBuy {

    @Getter
    private final UUID uuid;
    private final List<String> items = new ArrayList<>();
    @Getter
    private boolean autoBuyEnabled = false;

    private final Main plugin;
    private final Database database;




    public AutoBuy(UUID uuid, Main plugin) {
        this.uuid = uuid;
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setAutoBuyEnabled(boolean enabled) {
        this.autoBuyEnabled = enabled;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            database.set("autobuy", uuid, "status", enabled);
        });
    }

    public void addItem(String item) {
        if (!items.contains(item)) {
            items.add(item);
            saveItemsAsync();
        }
    }

    public void removeItem(String item) {
        if (items.remove(item)) {
            saveItemsAsync();
        }
    }

    public void toggleItem(String item) {
        if (items.contains(item)) {
            removeItem(item);
        } else {
            addItem(item);
        }
    }

    private void saveItemsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (database) {
                String joinedItems = String.join(",", items);

                Map<String, Object> data = new HashMap<>();
                data.put("items", joinedItems);
                data.put("status", autoBuyEnabled);

                database.insertOrUpdate("autobuy", uuid, data);
            }
        });
    }


    public void loadFromDatabase(Runnable onFinish) {
        List<String> loadedItems = database.get("autobuy", uuid, "items", rs -> {
            try {
                String str = rs.getString("items");
                return str != null ? Arrays.asList(str.split(",")) : new ArrayList<>();
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
        Boolean loadedStatus = database.get("autobuy", uuid, "status", rs -> {
            try {
                return rs.getBoolean("status");
            } catch (Exception e) {
                return false;
            }
        });

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (loadedItems != null) {
                items.clear();
                items.addAll(loadedItems);
            }
            autoBuyEnabled = loadedStatus != null && loadedStatus;
            onFinish.run();
        });
    }

}
