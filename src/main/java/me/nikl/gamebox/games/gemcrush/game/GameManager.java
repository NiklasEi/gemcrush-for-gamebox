package me.nikl.gamebox.games.gemcrush.game;

import me.nikl.gamebox.GameBoxSettings;
import me.nikl.gamebox.data.toplist.SaveType;
import me.nikl.gamebox.game.rules.GameRule;
import me.nikl.gamebox.game.exceptions.GameStartException;
import me.nikl.gamebox.games.gemcrush.GemCrush;
import me.nikl.gamebox.games.gemcrush.Language;
import me.nikl.gamebox.games.gemcrush.gems.Gem;
import me.nikl.gamebox.games.gemcrush.gems.NormalGem;
import me.nikl.gamebox.utility.Sound;
import me.nikl.gamebox.utility.StringUtility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author Niklas Eicker
 *
 * GameManager implementing the GameBox interface
 */
public class GameManager implements me.nikl.gamebox.game.manager.GameManager {
    private GemCrush game;
    private Set<Game> games;
    private Map<UUID, Integer> clicks;
    private Map<String, GameRules> gameTypes = new HashMap<>();
    private Map<String, Gem> gems = new HashMap<>();
    private float volume;
    private boolean pay, rewardBypass;
    private Language language;

    public GameManager(GemCrush game) {
        this.language = (Language) game.getGameLang();
        this.game = game;
        this.games = new HashSet<>();
        this.clicks = new HashMap<>();
        this.volume = (float) game.getConfig().getDouble("game.soundVolume", 0.5);
        getOnGameEnd();
        if (!loadGems()) {
            Bukkit.getLogger().log(Level.SEVERE, " problem while loading the gems from the configuration file");
        }
    }

    private void getOnGameEnd() {
        if (!game.getConfig().isConfigurationSection("onGameEnd")) return;
        ConfigurationSection onGameEnd = game.getConfig().getConfigurationSection("onGameEnd");
        rewardBypass = onGameEnd.getBoolean("restrictions.playersWithBypassDontGetRewards", true);
        pay = onGameEnd.getBoolean("pay");
    }

    private boolean loadGems() {
        boolean worked = true;
        Material mat = null;
        int data = 0;
        int index = 0;
        if (!game.getConfig().isConfigurationSection("normalGems")) {
            Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Outdated configuration file! Game cannot be started.");
            return false;
        }
        ConfigurationSection section = game.getConfig().getConfigurationSection("normalGems");
        for (String key : section.getKeys(false)) {
            game.debug("getting " + key);
            if (!section.isSet(key + ".material")) {
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + key);
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Skipping the gem. Is the material set?");
                continue;
            }
            if (!section.isSet(key + ".displayName") || !section.isString(key + ".displayName")) {
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + key);
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Skipping the gem. Is the displayName set?");
                continue;
            }
            String value = section.getString(key + ".material");
            String[] obj = value.split(":");
            String name = StringUtility.color(section.getString(key + ".displayName"));
            if (obj.length == 2) {
                try {
                    mat = Material.matchMaterial(obj[0]);
                } catch (Exception e) {
                    worked = false; // material name doesn't exist
                }
                try {
                    data = Integer.valueOf(obj[1]);
                } catch (NumberFormatException e) {
                    worked = false; // data not a number
                }
            } else {
                try {
                    mat = Material.matchMaterial(value);
                } catch (Exception e) {
                    worked = false; // material name doesn't exist
                }
            }
            if (mat == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + "normalGems." + key);
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "The material is not valid! Maybe your minecraft version is too old for it?");
                Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Change the material or delete the gem. Skipping...");
                continue;
            }
            game.debug("saving gem " + name + " as " + index);
            if (obj.length == 1) {
                this.gems.put(Integer.toString(index), new NormalGem(mat, name));
            } else {
                this.gems.put(Integer.toString(index), new NormalGem(mat, name, (short) data));
            }
            if (section.isSet(key + ".pointsOnBreak") && section.isInt(key + ".pointsOnBreak")) {
                this.gems.get(Integer.toString(index)).setPointsOnBreak(section.getInt(key + ".pointsOnBreak"));
            }
            if (section.isSet(key + ".probability") && (section.isDouble(key + ".probability") || section.isInt(key + ".probability"))) {
                game.debug("set probability of " + name + " to " + section.getDouble(key + ".probability"));
                ((NormalGem) this.gems.get(Integer.toString(index))).setPossibility(section.getDouble(key + ".probability"));
            }
            index++;
        }
        return worked;
    }

    private Game getGame(UUID uuid) {
        for (Game game : games) {
            if (isPlayer(uuid, game)) {
                return game;
            }
        }
        return null;
    }

    private boolean isPlayer(UUID uuid, Game game) {
        return game.getPlayer().getUniqueId().equals(uuid);
    }

    void removeGame(UUID uuid) {
        Game game = getGame(uuid);
        if (game != null) {
            removeGame(game);
        } else {
            this.game.debug(" game was already closed O.o");
        }
    }

    void removeGame(Game game) {
        clicks.remove(game.getPlayer().getUniqueId());
        game.shutDown();
        games.remove(game);
    }

    void onGameEnd(String gameID, int score, Player player, boolean payOut) {
        double reward = gameTypes.get(gameID).getMoneyToWin(score);
        if (payOut && this.pay && GameBoxSettings.econEnabled && (!player.hasPermission("gamebox.bypass." + this.game.getGameID()) && (!player.hasPermission("gamebox.bypass")) || rewardBypass)) {
            this.game.debug("Reward is: " + reward);
            if (reward > 0) {
                player.sendMessage(StringUtility.color(language.PREFIX + language.GAME_FINISHED_WITH_PAY.replaceAll("%score%", score + "").replaceAll("%reward%", reward + "")));
            } else {
                player.sendMessage(StringUtility.color(language.PREFIX + language.GAME_FINISHED_NO_PAY.replaceAll("%score%", score + "")));
            }
        } else {
            player.sendMessage(StringUtility.color(language.PREFIX + language.GAME_FINISHED_NO_PAY.replaceAll("%score%", score + "")));
        }
        this.game.onGameWon(player, gameTypes.get(gameID), score);
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!event.getAction().equals(InventoryAction.PICKUP_ALL) && !event.getAction().equals(InventoryAction.PICKUP_HALF)) {
            return;
        }
        if (event.getRawSlot() != event.getSlot()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Game game = getGame(player.getUniqueId());
        if (game == null) {
            clicks.remove(player.getUniqueId());
            player.closeInventory();
            games.remove(game);
            return;
        }
        int slot = event.getSlot();
        if (game.getState() == null) return;
        switch (game.getState()) {
            case PLAY:
                if (this.clicks.containsKey(player.getUniqueId())) {
                    int oldSlot = clicks.get(player.getUniqueId());
                    if (slot == oldSlot + 1 || slot == oldSlot - 1 || slot == oldSlot + 9 || slot == oldSlot - 9) {
                        if (game.switchGems(slot < oldSlot ? slot : oldSlot, slot > oldSlot ? slot : oldSlot)) {
                            clicks.remove(player.getUniqueId());
                            if (game.isPlaySounds())
                                player.playSound(player.getLocation(), Sound.NOTE_BASS.bukkitSound(), volume, 1f);
                        } else {
                            if (game.isPlaySounds())
                                player.playSound(player.getLocation(), Sound.VILLAGER_HIT.bukkitSound(), volume, 1f);
                        }
                    } else if (slot == oldSlot) {
                        break;
                    } else {
                        clicks.put(player.getUniqueId(), slot);
                        game.shine(slot, true);
                        game.shine(oldSlot, false);
                        if (game.isPlaySounds())
                            player.playSound(player.getLocation(), Sound.CLICK.bukkitSound(), volume, 1f);
                    }
                } else {
                    if (game.isPlaySounds())
                        player.playSound(player.getLocation(), Sound.CLICK.bukkitSound(), volume, 1f);
                    this.clicks.put(player.getUniqueId(), slot);
                    game.shine(slot, true);
                }
                break;

            case FILLING:
                break;

            default:
                break;
        }
        return;
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!isInGame(inventoryCloseEvent.getPlayer().getUniqueId())) {
            return;
        }
        getGame(inventoryCloseEvent.getPlayer().getUniqueId()).shutDown();
        removeGame(getGame(inventoryCloseEvent.getPlayer().getUniqueId()));
    }

    @Override
    public boolean isInGame(UUID uuid) {
        for (Game game : games) {
            if (isPlayer(uuid, game)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startGame(Player[] players, boolean playSounds, String... strings) throws GameStartException {
        if (strings == null || strings.length < 1) {
            throw new GameStartException(GameStartException.Reason.ERROR);
        } else if (strings.length == 1) {
            for (String id : gameTypes.keySet()) {
                if (!id.equalsIgnoreCase(strings[0])) continue;
                GameRules rules = gameTypes.get(id);
                if (!game.payIfNecessary(players[0], rules.getCost())) {
                    throw new GameStartException(GameStartException.Reason.NOT_ENOUGH_MONEY);
                }
                games.add(new Game(game, players[0], rules.getMoves(), rules.isBombs(), rules.getNumberOfGemTypes(), gems, (playSounds && game.getSettings().isPlaySounds()), rules));
                return;
            }
        }
        Bukkit.getLogger().log(Level.WARNING, "not supported number of arguments to start a game");
        throw new GameStartException(GameStartException.Reason.ERROR);
    }

    @Override
    public void removeFromGame(UUID uuid) {
        removeGame(uuid);
    }

    @Override
    public void loadGameRules(ConfigurationSection buttonSec, String buttonID) {
        boolean bombs = buttonSec.getBoolean("bombs", true);
        int moves = buttonSec.getInt("moves", 20);
        int numberOfGems = buttonSec.getInt("differentGems", 8);
        double cost = buttonSec.getDouble("cost", 0.);
        boolean saveStats = buttonSec.getBoolean("saveStats", false);
        gameTypes.put(buttonID, new GameRules(game, moves, numberOfGems, bombs, cost, saveStats, buttonID));
    }

    @Override
    public Map<String, ? extends GameRule> getGameRules() {
        return gameTypes;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void saveStats(UUID uniqueId, int points, String key) {
        game.getGameBox().getDataBase().addStatistics(uniqueId, game.getGameID(), key, (double) points, SaveType.SCORE);
    }
}
