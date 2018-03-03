package me.nikl.gamebox.games.gemcrush;

import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.games.Game;
import me.nikl.gamebox.games.GameSettings;
import me.nikl.gamebox.games.GemCrushMain;
import me.nikl.gamebox.games.gemcrush.game.GameManager;

public class GemCrush extends Game {
    private final String[][] depends = new String[][]{
            new String[]{"Vault", "1.5"},
            new String[]{"GameBox", "1.5.0"}
    };
    private final String[] subCommands = new String[]{
            "gemcrush", "gc"
    };

    public GemCrush(GameBox gameBox) {
        super(gameBox, GemCrushMain.GEM_CRUSH);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void init() {

    }

    @Override
    public void loadSettings() {
        gameSettings.setGameType(GameSettings.GameType.SINGLE_PLAYER);
        gameSettings.setGameGuiSize(54);
        gameSettings.setHandleClicksOnHotbar(false);
    }

    @Override
    public void loadLanguage() {
        gameLang = new Language(this);
    }

    @Override
    public void loadGameManager() {
        gameManager = new GameManager(this);
    }
}

