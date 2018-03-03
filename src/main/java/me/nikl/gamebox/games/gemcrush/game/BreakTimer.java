package me.nikl.gamebox.games.gemcrush.game;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

/**
 * Created by niklas on 10/6/16.
 * <p>
 * Timer Class used to break matched Gems with a small delay
 */
class BreakTimer extends BukkitRunnable {

    private ArrayList<Integer> toBreak;
    private Game game;
    private boolean isBomb = false;

    BreakTimer(Game game, ArrayList<Integer> toBreak, int breakTicks) {
        this.toBreak = toBreak;
        this.game = game;

        this.runTaskLater(game.getGameBox(), breakTicks);
    }

    BreakTimer(Game game, ArrayList<Integer> toBreak, int breakTicks, boolean isBomb) {
        this(game, toBreak, breakTicks);
        this.isBomb = isBomb;
    }


    @Override
    public void run() {
        game.breakGems(toBreak);
        if (!isBomb) {
            game.playBreakSound();
        } else {
            game.playExplodingBomb();
        }
    }
}
