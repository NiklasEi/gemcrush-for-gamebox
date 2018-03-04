package me.nikl.gamebox.games.gemcrush.game;

import me.nikl.gamebox.data.toplist.SaveType;
import me.nikl.gamebox.game.rules.GameRuleMultiRewards;
import me.nikl.gamebox.games.gemcrush.GemCrush;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Niklas Eicker
 *
 * save stuff for the different games
 */
public class GameRules extends GameRuleMultiRewards {
    private int moves;
    private boolean bombs;
    private int numberOfGemTypes;

    public GameRules(GemCrush plugin, int moves, int numberOfGemTypes, boolean bombs, double cost, boolean saveStats, String key) {
        super(key, saveStats, SaveType.SCORE, cost);
        this.moves = moves;
        this.numberOfGemTypes = numberOfGemTypes;
        this.bombs = bombs;
        loadRewards(plugin);
    }

    private void loadRewards(GemCrush plugin) {
        if (!plugin.getConfig().isConfigurationSection("gameBox.gameButtons." + key + ".scoreIntervals")) return;
        ConfigurationSection onGameEnd = plugin.getConfig().getConfigurationSection("gameBox.gameButtons." + key + ".scoreIntervals");
        for (String key : onGameEnd.getKeys(false)) {
            int keyInt;
            try {
                keyInt = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.warn(" NumberFormatException while getting the rewards from config!");
                continue;
            }
            if (onGameEnd.isSet(key + ".money") && ((onGameEnd.isDouble(key + ".money") || onGameEnd.isInt(key + ".money")))) {
                addMoneyReward(keyInt, onGameEnd.getDouble(key + ".money"));
            } else {
                addMoneyReward(keyInt, 0.);
            }
            if (onGameEnd.isSet(key + ".tokens") && (onGameEnd.isDouble(key + ".tokens") || onGameEnd.isInt(key + ".tokens"))) {
                addTokenReward(keyInt, onGameEnd.getInt(key + ".tokens"));
            } else {
                addTokenReward(keyInt, 0);
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
}
