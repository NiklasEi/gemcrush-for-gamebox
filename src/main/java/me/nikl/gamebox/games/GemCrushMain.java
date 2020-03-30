package me.nikl.gamebox.games;

import me.nikl.gamebox.games.gemcrush.GemCrush;
import me.nikl.gamebox.module.GameBoxModule;

/**
 * @author Niklas Eicker
 */
public class GemCrushMain extends GameBoxModule {
    public static final String GEM_CRUSH = "gemcrush";

    @Override
    public void onEnable() {
        registerGame(GEM_CRUSH, GemCrush.class, "gc");
    }

    @Override
    public void onDisable() {

    }
}
