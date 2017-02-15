package me.nikl.gemcrush.game;

/**
 * Created by Niklas on 15.02.2017.
 */
public class GameRules {
    private int moves;
    private boolean bombs;
    private int numberOfGemTypes;
    private double cost;


    public GameRules(int moves, int numberOfGemTypes, boolean bombs, double cost){
        this.moves = moves;
        this.numberOfGemTypes = numberOfGemTypes;
        this.bombs = bombs;
        this.cost = cost;
    }

    public int getMoves() {
        return moves;
    }

    public void setMoves(int moves) {
        this.moves = moves;
    }

    public boolean isBombs() {
        return bombs;
    }

    public void setBombs(boolean bombs) {
        this.bombs = bombs;
    }

    public int getNumberOfGemTypes() {
        return numberOfGemTypes;
    }

    public void setNumberOfGemTypes(int numberOfGemTypes) {
        this.numberOfGemTypes = numberOfGemTypes;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}
