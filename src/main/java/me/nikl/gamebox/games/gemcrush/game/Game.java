package me.nikl.gamebox.games.gemcrush.game;

import me.nikl.gamebox.games.gemcrush.GemCrush;
import me.nikl.gamebox.games.gemcrush.Language;
import me.nikl.gamebox.games.gemcrush.gems.Bomb;
import me.nikl.gamebox.games.gemcrush.gems.Gem;
import me.nikl.gamebox.games.gemcrush.gems.NormalGem;
import me.nikl.gamebox.nms.NmsFactory;
import me.nikl.gamebox.nms.NmsUtility;
import me.nikl.gamebox.utility.Sound;
import me.nikl.gamebox.utility.StringUtility;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;


class Game extends BukkitRunnable {
    private GameState state;
    private GameManager manager;
    private Player player;
    private FileConfiguration config;
    private Language lang;
    private Inventory inv;
    private Gem[] grid = new Gem[54];
    private BreakTimer breakTimer;
    private String currentInventoryTitle;
    private Map<String, Gem> gems;
    private Map<String, Gem> usedNormalGems = new HashMap<>();
    private NmsUtility nmsUtility;
    private int moves;
    private int points = 0;
    private int moveTicks, breakTicks;
    private int gemsNum;
    private int cyclesUtilCheckingForExistingMove;
    // bomb options
    private boolean enableBombs;
    private ArrayList<String> bombLore;
    private int bombPointsOnBreak;
    private String bombDisplayName;
    private int ticksTillExplosion;
    private ArrayList<Integer> spawnedBombs = new ArrayList<>();
    private Random rand = new Random();
    private GameRules usedGameRules;
    private float volume;
    private boolean playSounds;
    private GemCrush gemCrush;

    public Game(GemCrush gemCrush, Player player, int moves, boolean bombs, int gemNums, Map<String, Gem> gems, boolean playSounds, GameRules usedGameRules) {
        this.gemCrush = gemCrush;
        this.playSounds = playSounds;
        this.lang = (Language) gemCrush.getGameLang();
        this.config = gemCrush.getConfig();
        this.manager = (GameManager) gemCrush.getGameManager();
        this.player = player;
        this.moves = moves;
        this.enableBombs = bombs;
        this.usedGameRules = usedGameRules;
        this.gems = gems;
        this.volume = (float) config.getDouble("game.soundVolume", 0.5);
        this.gemsNum = gemNums;
        this.nmsUtility = NmsFactory.getNmsUtility();
        if (!loadOptions()) {
            Bukkit.getConsoleSender().sendMessage("You are missing options in the configuration file.");
            Bukkit.getConsoleSender().sendMessage("Game will be started with defaults. Please get an up to date config file.");
        }
        if (!loadGems()) {
            player.sendMessage(StringUtility.color(lang.PREFIX + " &2Configuration error. Please contact the server owner!"));
            return;
        }
        this.currentInventoryTitle = lang.TITLE_GAME;
        this.inv = gemCrush.createInventory(54, currentInventoryTitle.replaceAll("%moves%", String.valueOf(moves)).replaceAll("%score%", String.valueOf(points)));
        startGame();
    }

    private void startGame() {
        this.state = GameState.FILLING;
        player.openInventory(this.inv);
        this.runTaskTimer(gemCrush.getGameBox(), 0, this.moveTicks);
    }

    private boolean loadOptions() {
        this.moveTicks = config.getInt("game.ticksBetweenMovement" , 5);
        this.breakTicks = config.getInt("game.ticksBetweenSwitchAndDestroy", 10);
        this.bombDisplayName = config.getString("game.bombs.displayName", "&4Bomb");
        this.bombPointsOnBreak = config.getInt("game.bombs.pointsOnBreak", 10);
        this.ticksTillExplosion = config.getInt("game.bombs.ticksTillExplosion", 15);
        if (this.config.isSet("game.bombs.lore") && this.config.isList("game.bombs.lore")) {
            this.bombLore = new ArrayList<>(config.getStringList("game.bombs.lore"));
        } else {
            this.bombLore = new ArrayList<>();
            bombLore.add("&4Caution: &6explosive!");
        }
        for (int i = 0; i < bombLore.size(); i++) {
            bombLore.set(i, StringUtility.color(bombLore.get(i)));
        }
        return true;
    }

    private boolean hasToBeFilled() {
        for (int slot = 0; slot < 54; slot++) {
            if (this.inv.getItem(slot) == null) return true;
        }
        return false;
    }

    public ArrayList<Integer> scanRows() {
        ArrayList<Integer> toBreak = new ArrayList<>();
        int slot, c;
        int colorInRow;
        String name;
        for (int i = 0; i < 6; i++) {
            c = 0;
            name = grid[i * 9].getName();
            colorInRow = 1;
            c++;
            while (c < 9) {
                slot = i * 9 + c;
                while (c < 9 && slot < 54 && name.equals(grid[slot].getName())) {
                    colorInRow++;
                    c++;
                    slot++;
                }
                if (colorInRow >= 3) {
                    for (int breakSlot = slot - 1; breakSlot >= slot - colorInRow; breakSlot--) {

                        // continue for not matchable gems
                        if (grid[breakSlot].getName().equalsIgnoreCase("Bomb")) continue;
                        toBreak.add(breakSlot);
                    }
                }
                if (c < 9) name = grid[slot].getName();
                colorInRow = 1;
                c++;
            }
        }
        return toBreak;
    }

    private boolean moveLeft() {
        ArrayList<Integer> add = new ArrayList<>();
        for (int column = 0; column < 9; column++) {
            for (int row = 0; row < 6; row++) {
                int slot = column + 9 * row;
                if (column < 8 && row < 5) {
                    add.clear();
                    Collections.addAll(add, 1, 9);
                } else if (column < 8 && row == 5) {
                    add.clear();
                    Collections.addAll(add, 1);
                } else if (column == 8 && row < 5) {
                    add.clear();
                    Collections.addAll(add, 9);
                } else {
                    continue;
                }
                for (int i : add) {
                    Gem oldGem = this.grid[slot];
                    this.grid[slot] = this.grid[slot + i];
                    this.grid[slot + i] = oldGem;
                    if (!scanColumns().isEmpty() || !scanRows().isEmpty()) {
                        oldGem = this.grid[slot];
                        this.grid[slot] = this.grid[slot + i];
                        this.grid[slot + i] = oldGem;
                        return true;
                    }
                    oldGem = this.grid[slot];
                    this.grid[slot] = this.grid[slot + i];
                    this.grid[slot + i] = oldGem;
                }
            }
        }
        return false;
    }

    public ArrayList<Integer> scanColumns() {
        ArrayList<Integer> toBreak = new ArrayList<>();
        int slot, c;
        int colorInRow;
        String name;
        for (int i = 0; i < 9; i++) {
            c = 0;
            name = grid[i].getName();
            c++;
            colorInRow = 1;
            while (c < 6) {
                slot = i + c * 9;
                while (c < 6 && slot < 54 && name.equals(grid[slot].getName())) {
                    colorInRow++;
                    c++;
                    slot = i + c * 9;
                }
                if (colorInRow >= 3) {
                    for (int breakSlot = slot - 9; breakSlot >= slot - colorInRow * 9; breakSlot -= 9) {
                        if (grid[breakSlot].getName().equalsIgnoreCase("Bomb")) continue;
                        toBreak.add(breakSlot);
                    }
                }
                if (c < 6) name = grid[slot].getName();
                colorInRow = 1;
                c++;
            }
        }
        return toBreak;
    }

    private boolean loadGems() {
        boolean worked = true;
        if (gemsNum > gems.size()) {
            Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] Could not load enough gems! Quiting game.");
            Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] You can add some in the config file ;)");
            manager.removeGame(this);
            return false;
        }
        ArrayList<Integer> savedGems = new ArrayList<>();
        int key;
        for (int i = 0; i < gemsNum; i++) {
            key = getKeyWithWeights(savedGems);
            if (key < 0) {
                Bukkit.getLogger().log(Level.WARNING, "[GemCrush] Problem while choosing the gems");
                gemsNum--;
                continue;
            }
            if (gems.get(Integer.toString(key)) == null) {
                Bukkit.getConsoleSender().sendMessage("gem " + key + " is null!");
                manager.removeGame(this);
                return false;
            }
            this.usedNormalGems.put(Integer.toString(i), gems.get(Integer.toString(key)));
            savedGems.add(key);
        }
        return worked;
    }

    private int getKeyWithWeights(ArrayList<Integer> savedGems) {
        int key;
        Random rand = new Random();
        double totalWeight = 0;
        for (String gemName : gems.keySet()) {
            if (!savedGems.contains(Integer.parseInt(gemName)))
                totalWeight += ((NormalGem) gems.get(gemName)).getPossibility();
        }
        double weightedKey = rand.nextDouble() * totalWeight;
        for (String gemName : gems.keySet()) {
            if (savedGems.contains(Integer.parseInt(gemName))) continue;
            totalWeight -= ((NormalGem) gems.get(gemName)).getPossibility();
            if (totalWeight < weightedKey) {
                key = Integer.parseInt(gemName);
                return key;
            }
        }
        return -1;
    }

    @Override
    public void run() {
        switch (this.state) {
            case FILLING:
                cyclesUtilCheckingForExistingMove = 0;
                for (int column = 8; column > -1; column--) {
                    for (int row = 5; row > -1; row--) {
                        int slot = row * 9 + column;
                        if (this.grid[slot] == null) {
                            if (row == 0) {
                                grid[slot] = new NormalGem((NormalGem) usedNormalGems.get(Integer.toString(rand.nextInt(gemsNum))));
                                this.inv.setItem(slot, grid[slot].getItem());
                                row--;
                            } else {
                                if (this.grid[slot - 9] != null) {
                                    if (grid[slot - 9] instanceof Bomb) {
                                        spawnedBombs.remove(Integer.valueOf(slot - 9));
                                        spawnedBombs.add(slot);
                                    }
                                    this.grid[slot] = this.grid[slot - 9];
                                    this.inv.setItem(slot, grid[slot - 9].getItem());
                                    this.grid[slot - 9] = null;
                                    this.inv.setItem(slot - 9, null);
                                    row--;
                                }
                            }
                            if (playSounds) {
                                double randDouble = rand.nextDouble();
                                if (randDouble < 0.25) {
                                    if (randDouble < 0.06) {
                                        player.playSound(player.getLocation(), Sound.NOTE_STICKS.bukkitSound(), volume, 5f);
                                    } else if (randDouble < 0.13) {
                                        player.playSound(player.getLocation(), Sound.NOTE_PLING.bukkitSound(), volume, 3f);
                                    } else if (randDouble < 0.19) {
                                        player.playSound(player.getLocation(), Sound.NOTE_PLING.bukkitSound(), volume, 1f);
                                    } else if (randDouble < 0.25) {
                                        player.playSound(player.getLocation(), Sound.NOTE_PIANO.bukkitSound(), volume, 3f);
                                    }
                                }
                            }
                        }
                    }
                }

                if (!hasToBeFilled()) {
                    if (!this.breakAll(true) && spawnedBombs.isEmpty()) {
                        if (moves > 0) {
                            state = GameState.PLAY;
                        } else {
                            state = GameState.FINISHED;
                            won();
                        }
                    } else if (!this.breakAll(false) && !spawnedBombs.isEmpty()) {
                        ArrayList<Integer> toBreak = new ArrayList<>();
                        ArrayList<Integer> addToBreak;
                        for (int bombSlot : spawnedBombs) {
                            addToBreak = getSurroundingSlots(bombSlot);
                            for (int slot : addToBreak) {
                                if (!toBreak.contains(slot)) {
                                    toBreak.add(slot);
                                }
                            }
                            if (!toBreak.contains(bombSlot)) {
                                toBreak.add(bombSlot);
                            }
                        }
                        spawnedBombs.clear();
                        breakTimer = new BreakTimer(this, toBreak, ticksTillExplosion, true);
                        state = GameState.BREAKING;
                    }
                }
                break;
            case FINISHED:
                break;
            case PLAY:
                if (cyclesUtilCheckingForExistingMove > 5) {
                    if (!moveLeft()) {
                        refill();
                        cyclesUtilCheckingForExistingMove = 0;
                    } else {
                        cyclesUtilCheckingForExistingMove = 0;
                    }
                } else {
                    cyclesUtilCheckingForExistingMove++;
                }
                break;
            case BREAKING:
                break;
            default:
                break;
        }
    }

    private ArrayList<Integer> getSurroundingSlots(int slot) {
        ArrayList<Integer> surroundingSlots = new ArrayList<>();

        for (int i : getAdd(slot)) {
            surroundingSlots.add(slot + i);
        }
        return surroundingSlots;
    }

    private ArrayList<Integer> getAdd(int slot) {
        ArrayList<Integer> add = new ArrayList<>();
        if (slot == 0) {// corner left top
            add.addAll(Arrays.asList(1, 9, 10));

        } else if (slot == 8) {// corner top right
            add.addAll(Arrays.asList(-1, 8, 9));

        } else if (slot == 45) {// corner bottom left
            add.addAll(Arrays.asList(-9, -8, 1));

        } else if (slot == 53) {// corner bottom right
            add.addAll(Arrays.asList(-10, -9, -1));

        } else if (slot > 0 && slot < 8) {// edge top
            add.addAll(Arrays.asList(-1, 1, 8, 9, 10));

        } else if (slot == 17 || slot == 26 || slot == 35 || slot == 44) {// edge right
            add.addAll(Arrays.asList(-10, -9, -1, 8, 9));

        } else if (slot > 45 && slot < 53) {// edge bottom
            add.addAll(Arrays.asList(-1, -10, -9, -8, 1));

        } else if (slot == 9 || slot == 18 || slot == 27 || slot == 36) {// edge left
            add.addAll(Arrays.asList(-9, -8, 1, 9, 10));

        } else {
            add.addAll(Arrays.asList(-10, -9, -8, -1, 1, 8, 9, 10));
        }

        return add;
    }


    private void refill() {
        for (int i = 0; i < 54; i++) {
            grid[i] = null;
        }
        this.inv.clear();
        this.state = GameState.FILLING;
    }

    private boolean breakAll(boolean schedule) {
        ArrayList<Integer> toBreak = new ArrayList<>();
        toBreak.addAll(this.scanColumns());
        toBreak.addAll(this.scanRows());
        if (toBreak.isEmpty()) return false;
        if (schedule) {
            ArrayList<Integer> all = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                if (spawnedBombs.contains(i))
                    continue;
                all.add(i);
            }
            shine(all, false);
            shine(toBreak, true);
            //player.updateInventory();
            for (int slot : toBreak) {
                inv.setItem(slot, grid[slot].getItem());
            }
            breakTimer = new BreakTimer(this, toBreak, breakTicks);
            setState(GameState.BREAKING);
        }
        return true;
    }

    public void won() {
        manager.onGameEnd(usedGameRules.getKey(), points, player, true);
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
        switch (state) {


            default:
                break;
        }
    }

    public boolean switchGems(int lowerSlot, int higherSlot) {
        Gem oldGem = this.grid[lowerSlot];
        this.grid[lowerSlot] = this.grid[higherSlot];
        this.grid[higherSlot] = oldGem;
        if (!breakAll(true)) {
            oldGem = this.grid[lowerSlot];
            this.grid[lowerSlot] = this.grid[higherSlot];
            this.grid[higherSlot] = oldGem;
            return false;
        }
        moves--;
        this.inv.setItem(lowerSlot, grid[lowerSlot].getItem());
        this.inv.setItem(higherSlot, grid[higherSlot].getItem());
        updateInventoryTitle();
        return true;
    }

    public void breakGems(ArrayList<Integer> toBreak) {
        for (int i : toBreak) {
            if (grid[i] != null) {
                points += grid[i].getPointsOnBreak();
                this.grid[i] = null;
                this.inv.setItem(i, null);
            } else if (this.enableBombs) {//If a gem is null here already a slot has to be broken twice and will spawn a bomb
                grid[i] = new Bomb(bombDisplayName, bombLore, bombPointsOnBreak);
                if (playSounds) player.playSound(player.getLocation(), Sound.FUSE.bukkitSound(), volume, 1f);
                grid[i].setItem(nmsUtility.addGlow(grid[i].getItem()));
                this.inv.setItem(i, grid[i].getItem());
                spawnedBombs.add(i);
            }
        }
        updateInventoryTitle();
        setState(GameState.FILLING);
    }

    private void updateInventoryTitle() {
        nmsUtility.updateInventoryTitle(player, currentInventoryTitle.replaceAll("%moves%", String.valueOf(moves)).replaceAll("%score%", String.valueOf(points)));
    }

    public void shutDown() {
        if (Bukkit.getScheduler().isCurrentlyRunning(this.getTaskId()) || Bukkit.getScheduler().isQueued(this.getTaskId()))
            this.cancel();
        if (breakTimer != null) {
            this.breakTimer.cancel();
            this.breakTimer = null;
        }
    }

    public void shine(int slot, boolean b) {
        if (b) {
            if (grid[slot] instanceof NormalGem) {
                grid[slot].setItem(nmsUtility.addGlow(grid[slot].getItem()));
            }
        } else {
            if (grid[slot] instanceof NormalGem) {
                grid[slot].setItem(nmsUtility.removeGlow(grid[slot].getItem()));
            }
        }
        this.inv.setItem(slot, grid[slot].getItem());
    }

    public void shine(ArrayList<Integer> slots, boolean b) {
        for (int slot : slots) {
            shine(slot, b);
        }
    }

    public void playExplodingBomb() {
        if (playSounds) player.playSound(player.getLocation(), Sound.EXPLODE.bukkitSound(), volume, 1f);
    }

    public void playBreakSound() {
        if (playSounds) player.playSound(player.getLocation(), Sound.ANVIL_BREAK.bukkitSound(), volume, 1f);
    }

    public boolean isPlaySounds() {
        return playSounds;
    }

    public Plugin getGameBox() {
        return gemCrush.getGameBox();
    }

    public Player getPlayer() {
        return player;
    }
}
