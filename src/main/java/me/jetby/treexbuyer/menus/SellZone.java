package me.jetby.treexbuyer.menus;

import lombok.Getter;
import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.configurations.PriseItemLoader;
import me.jetby.treexbuyer.sex.BoostManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.jetby.treexbuyer.configurations.Config.CFG;
import static org.bukkit.Bukkit.getLogger;

public class SellZone {

    private final BoostManager boostManager;
    private final Main plugin;

    public SellZone(Main plugin) {
        this.plugin = plugin;
        this.boostManager = plugin.getBoostManager();
    }

    @Getter
    private final Map<UUID, Double> countPlayer = new HashMap<>();
    public void checkItem(List<ItemStack> itemStacks, Map<String, PriseItemLoader.ItemData> prise, Player player) {
        countPlayer.put(player.getUniqueId(), 0d);

        Map<Material, Double> materialPrise = new HashMap<>();
        prise.forEach((key, vault) -> {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                materialPrise.put(material, vault.price() * boostManager.getPlayerCoefficient(player));
            } catch (IllegalArgumentException e) {
                if (CFG().getBoolean("logger")) {
                    getLogger().warning("[SellZone] Неизвестный материал: " + key);
                }
            }
        });

        if (CFG().getBoolean("logger")) {
            itemStacks.forEach(item -> plugin.getLogger().info(item.getType() + " : " + item.getAmount()));
        }

        double sum = 0d;
        for (ItemStack itemStack : itemStacks) {
            Double price = materialPrise.get(itemStack.getType());
            if (price != null) {
                sum += price * itemStack.getAmount();
            }
        }

        countPlayer.put(player.getUniqueId(), sum);
    }

    public String getCountPlayerString(UUID uuid, Integer residue) {
        Double value = countPlayer.get(uuid);
        if (value == null) {
            return "0";
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(residue, RoundingMode.DOWN);
        return bd.toPlainString();
    }

}
