package me.jetby.treexbuyer.functions;

import lombok.Getter;
import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BoostManager {

    private final Main plugin;
    private final Config config;

    public BoostManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getCfg();
    }

    @Getter
    private final Map<UUID, Double> cachedScores = new ConcurrentHashMap<>();


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
        if (!cachedScores.containsKey(uuid)) return;

        double scores = cachedScores.get(uuid);
        Map<String, Object> data = new HashMap<>();
        data.put("scores", scores);
        plugin.getDatabase().insertOrUpdateSync("players", uuid, data);
    }
    public void savePlayerScoresSync(UUID uuid) {
        if (!cachedScores.containsKey(uuid)) return;
            double score = cachedScores.get(uuid);
            if (score != 0) {plugin.getDatabase().insertOrUpdate("players", uuid, Collections.singletonMap("scores", score));
        }
    }

    public double getPlayerCoefficient(Player player) {
        double defaultCoefficient = config.getDefaultCoefficient();
        double maxLegalCoefficient = config.getMaxCoefficient();
        boolean boostersExceptLegal = config.isBoosters_except_legal_coefficient();
        double playerScore = getCachedScores(player);
        int scoreStep = config.getScores();
        double coefficientStep = config.getCoefficient();
        int multiplierCount = (int)(playerScore / scoreStep);
        double coefficient = defaultCoefficient + multiplierCount * coefficientStep;
        double baseCoefficient = Math.min(coefficient, maxLegalCoefficient);
        baseCoefficient = Math.max(baseCoefficient, defaultCoefficient);
        double boosterCoefficient = 0.0F;

        for(Boost boost : config.getBoosts().values()) {
            if (boost.permission() != null && player.hasPermission(boost.permission())) {
                boosterCoefficient += boost.coefficient();
            }
        }

        if (boostersExceptLegal) {
            return round(baseCoefficient + boosterCoefficient, 2);
        } else {
            return round(Math.min(baseCoefficient + boosterCoefficient, maxLegalCoefficient), 2);
        }
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
