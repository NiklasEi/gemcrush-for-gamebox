package me.nikl.gamebox.games.gemcrush.game;

import me.nikl.gamebox.games.gemcrush.GemCrush;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Niklas on 15.02.2017.
 *
 * save stuff for the different games
 */
public class GameRules {
    private GemCrush plugin;

    private int moves;
    private boolean bombs, saveStats;
    private int numberOfGemTypes;
    private double cost;
    private String key;

    private Map<Integer, Double> moneyRewards;
    private Map<Integer, Integer> tokenRewards;


    public GameRules(GemCrush plugin, int moves, int numberOfGemTypes, boolean bombs, double cost, boolean saveStats, String key){
        this.plugin = plugin;

        this.moves = moves;
        this.numberOfGemTypes = numberOfGemTypes;
        this.bombs = bombs;
        this.cost = cost;
        this.saveStats = saveStats;
        this.key = key;

        loadRewards();
    }

    private void loadRewards() {
        moneyRewards = new HashMap<>();
        tokenRewards = new HashMap<>();

        if(!plugin.getConfig().isConfigurationSection("gameBox.gameButtons." + key + ".scoreIntervals")) return;

        ConfigurationSection onGameEnd = plugin.getConfig().getConfigurationSection("gameBox.gameButtons." + key + ".scoreIntervals");
        for (String key : onGameEnd.getKeys(false)) {
            int keyInt;
            try {
                keyInt = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("[GemCrush] NumberFormatException while getting the rewards from config!");
                continue;
            }
            if (onGameEnd.isSet(key + ".money") && (onGameEnd.isDouble(key + ".money") || onGameEnd.isInt(key + ".money"))) {
                moneyRewards.put(keyInt, onGameEnd.getDouble(key + ".money"));
            } else {
                moneyRewards.put(keyInt, 0.);
            }
            if (onGameEnd.isSet(key + ".tokens") && (onGameEnd.isDouble(key + ".tokens") || onGameEnd.isInt(key + ".tokens"))) {
                tokenRewards.put(keyInt, onGameEnd.getInt(key + ".tokens"));
            } else {
                tokenRewards.put(keyInt, 0);
            }
        }
    }

    public int getMoves() {
        return moves;
    }

    public boolean isBombs() {
        return bombs;
    }

    public int getNumberOfGemTypes() {
        return numberOfGemTypes;
    }

    public double getCost() {
        return cost;
    }

    public boolean isSaveStats() {
        return saveStats;
    }

    public String getKey() {
        return key;
    }

    public Map<Integer, Double> getMoneyRewards() {
        return moneyRewards;
    }

    public Map<Integer, Integer> getTokenRewards() {
        return tokenRewards;
    }
}
