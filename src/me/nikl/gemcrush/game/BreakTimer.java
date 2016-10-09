package me.nikl.gemcrush.game;

import me.nikl.gemcrush.Main;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

/**
 * Created by niklas on 10/6/16.
 */
public class BreakTimer extends BukkitRunnable{
	
	ArrayList<Integer> toBreak;
	Game game;
	
	public BreakTimer(Game game, ArrayList<Integer> toBreak, int breakTicks){
		this.toBreak = toBreak;
		this.game = game;
		
		this.runTaskLater(Main.getPlugin(Main.class), breakTicks);
	}
	
	
	@Override
	public void run() {
		game.breakGems(toBreak);
	}
}
