package me.jetby.treexbuyer.functions;

import lombok.Getter;
import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.Config;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BoostManager {

    private final Main plugin;

    public BoostManager(Main plugin) {
        this.plugin = plugin;
    }

    @Getter
    private final Map<UUID, Double> cachedScores = new ConcurrentHashMap<>();
    private final Map<String, Boost> boosts = new HashMap<>();

    public void loadBoosts() {
        boosts.clear();
        ConfigurationSection boosterSection = Config.CFG().getConfigurationSection("booster");
        if (boosterSection != null) {
            for (String key : boosterSection.getKeys(false)) {
                String permission = boosterSection.getString(key + ".permission");
                double coefficient = boosterSection.getDouble(key + ".external-coefficient", 0.0);
                boosts.put(key, new Boost(key, permission, coefficient));
            }
        }
    }

    public void loadPlayersScores(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Double result = plugin.getDatabase().get("players", uuid, "scores", rs -> {
                try {
                    return rs.getDouble("scores");
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0.0;
                }
            });

            double score = result == null ? 0.0 : result;
            Bukkit.getScheduler().runTask(plugin, () -> cachedScores.put(uuid, score));
        });
    }
    public void savePlayerScores(UUID uuid) {
        Double score = cachedScores.get(uuid);
        if (score != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabase().insertOrUpdate("players", uuid, Collections.singletonMap("scores", score));
            });
        }
    }
    public void savePlayerScoresSync(UUID uuid) {
        Double score = cachedScores.get(uuid);
        if (score != null) {
            plugin.getDatabase().insertOrUpdate("players", uuid, Collections.singletonMap("scores", score));
        }
    }

    public double getPlayerCoefficient(Player player) {
        double defaultCoefficient = Config.CFG().getDouble("default-coefficient", 1.0);
        double maxLegalCoefficient = Config.CFG().getDouble("max-legal-coefficient", 5.0);

        double playerScore = getCachedScores(player);

        int scoreStep = Config.CFG().getInt("score-to-multiplier-ratio.scores", 100);
        double coefficientStep = Config.CFG().getDouble("score-to-multiplier-ratio.coefficient", 0.01);

        int multiplierCount = (int) (playerScore / scoreStep);
        double coefficient = defaultCoefficient + (multiplierCount * coefficientStep);

        for (Boost boost : boosts.values()) {
            if (boost.permission() != null && player.hasPermission(boost.permission())) {
                coefficient += boost.coefficient();
            }
        }

        coefficient = Math.min(coefficient, maxLegalCoefficient);
        coefficient = Math.max(coefficient, defaultCoefficient);

        return round(coefficient, 2);
    }

    public double getCachedScores(Player player) {
        return cachedScores.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void addPlayerScores(Player player, double scores) {
        UUID uuid = player.getUniqueId();
        cachedScores.merge(uuid, scores, Double::sum);
    }
    public void setPlayerScores(Player player, double scores) {
        UUID uuid = player.getUniqueId();
        cachedScores.put(uuid, Math.max(0.0, scores));
    }

    public void removePlayerScores(Player player, double scores) {
        UUID uuid = player.getUniqueId();
        cachedScores.compute(uuid, (key, current) -> {
            if (current == null || current <= 0) return 0.0;
            return Math.max(0.0, current - scores);
        });
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
