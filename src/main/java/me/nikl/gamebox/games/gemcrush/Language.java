package me.nikl.gamebox.games.gemcrush;

import me.nikl.gamebox.game.Game;
import me.nikl.gamebox.game.GameLanguage;

public class Language extends GameLanguage {
    public String GAME_PAYED, GAME_NOT_ENOUGH_MONEY, GAME_FINISHED_NO_PAY, GAME_FINISHED_WITH_PAY;
    public String TITLE_GAME;

    Language(Game game) {
        super(game);
    }

    @Override
    protected void loadMessages() {
        getGameMessages();
        getInvTitles();
    }

    private void getInvTitles() {
        this.TITLE_GAME = getString("inventoryTitles.title");
    }

    private void getGameMessages() {
        this.GAME_PAYED = getString("game.econ.payed");
        this.GAME_NOT_ENOUGH_MONEY = getString("game.econ.notEnoughMoney");
        this.GAME_FINISHED_NO_PAY = getString("game.finishedWithoutPayout");
        this.GAME_FINISHED_WITH_PAY = getString("game.finishedWithPayout");
    }
}

