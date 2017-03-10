package me.nikl.gemcrush.game;

/**
 * Created by Niklas on 15.02.2017.
 */
public class GameRules {
    private int moves;
    private boolean bombs, saveStats;
    private int numberOfGemTypes;
    private double cost;
    private String key;


    public GameRules(int moves, int numberOfGemTypes, boolean bombs, double cost, boolean saveStats, String key){
        this.moves = moves;
        this.numberOfGemTypes = numberOfGemTypes;
        this.bombs = bombs;
        this.cost = cost;
        this.saveStats = saveStats;
        this.key = key;
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
}
