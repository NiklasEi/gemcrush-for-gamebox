package me.nikl.gemcrush.game;

import java.util.*;
import java.util.logging.Level;

import me.nikl.gemcrush.Sounds;
import me.nikl.gemcrush.gems.Bomb;
import me.nikl.gemcrush.gems.Gem;
import me.nikl.gemcrush.gems.NormalGem;
import me.nikl.gemcrush.nms.InvTitle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;


class Game extends BukkitRunnable{
	
	private GameState state;
	private GameManager manager;
	private UUID playerUUID;
	private Player player;
	// save the config
	private FileConfiguration config;
	// language class
	private Language lang;
	// inventory
	private Inventory inv;
	// Array of all gems in the inventory
	private Gem[] grid;
	// timer to break gems
	private BreakTimer breakTimer;
	
	// current inventory title
	private String title;

	// map with all gems
	private Map<String, Gem> gems;

	// map with all normal gems that are used in this game
	private Map<String, Gem> usedNormalGems;
	
	private Main plugin;
	
	private InvTitle updater;
	
	private int moves, points;
	private int moveTicks, breakTicks;
	
	private int gemsNum;
	
	// check for existing move every x cycles
	// reset cycles to 0 on breaking/filling state and add one every run in case PLAY
	private int checkCycles;


	// bomb options
	private boolean enableBombs;
	private ArrayList<String> bombLore;
	private int bombPointsOnBreak;
	private String bombDisplayName;
	private int ticksTillExplosion;
	// Bomb was spawned, to be checked in next run
	private ArrayList<Integer> bombSpawned;
	
	private Random rand = new Random();
	double randDouble;

	private GameRules rule;

	private float volume;
	private boolean playSounds;
	
	private boolean payOut, sendMessages, dispatchCommands, sendBroadcasts, giveItemRewards;
	
	public Game(Main plugin, UUID playerUUID, int moves, boolean bombs, int gemNums, Map<String, Gem> gems, boolean playSounds, GameRules rule){
		this.plugin = plugin;
		this.playSounds= playSounds;
		this.lang = plugin.lang;
		this.config = plugin.getConfig();
		this.manager = plugin.getManager();
		this.playerUUID = playerUUID;
		this.player = Bukkit.getPlayer(playerUUID);
		this.grid = new Gem[54];
		this.usedNormalGems = new HashMap<>();
		this.moves = moves;
		this.points = 0;
		this.enableBombs = bombs;
		this.rule = rule;

		this.gems = gems;

		this.volume = (float) config.getDouble("game.soundVolume", 0.5);
		
		this.bombSpawned = new ArrayList<>();
		
		
		this.gemsNum = gemNums;
		
		payOut = true;
		sendMessages = true;
		dispatchCommands = true;
		sendBroadcasts = true;
		giveItemRewards = true;
		
		this.updater = plugin.getUpdater();
		
		
		if(player == null){
			Bukkit.getConsoleSender().sendMessage("Player is null!");//XXX
			manager.removeGame(this);
			return;
		}
		if(config == null){
			Bukkit.getConsoleSender().sendMessage(Language.prefix + " Failed to load config!");
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}
		
		if(!loadOptions()){
			Bukkit.getConsoleSender().sendMessage("You are missing options in the configuration file.");//XXX
			Bukkit.getConsoleSender().sendMessage("Game will be started with defaults. Please get an up to date config file.");//XXX
		}
		
		if(!loadGems()){
			player.sendMessage(chatColor(Language.prefix + " &2Configuration error. Please contact the server owner!"));
			return;
		}
		
		
		this.title = lang.TITLE_GAME;
		this.inv = Bukkit.getServer().createInventory(null, 54, chatColor(title.replaceAll("%moves%", moves + "").replaceAll("%score%", points + "")));
		
		// this basically starts the game
		this.state = GameState.FILLING;
		player.openInventory(this.inv);
		
		this.runTaskTimer(Main.getPlugin(Main.class), 0, this.moveTicks);
	}

	public Game(Main plugin, UUID playerUUID, int moves, boolean bombs, int gemNums, Map<String, Gem> gems, boolean playSounds, GameRules rule, boolean payOut, boolean sendMessages, boolean dispatchCommands, boolean sendBroadcasts, boolean giveItemRewards){
		this(plugin, playerUUID, moves, bombs, gemNums, gems, playSounds, rule);
		
		this.payOut = payOut;
		this.sendMessages = sendMessages;
		this.dispatchCommands = dispatchCommands;
		this.sendBroadcasts = sendBroadcasts;
		this.giveItemRewards = giveItemRewards;
	}
	
	private boolean loadOptions() {
		if(this.config.isSet("game.ticksBetweenMovement") && this.config.isInt("game.ticksBetweenMovement")){
			this.moveTicks = config.getInt("game.ticksBetweenMovement");
		} else {
			this.moveTicks = 5;
		}
		
		if(this.config.isSet("game.ticksBetweenSwitchAndDestroy") && this.config.isInt("game.ticksBetweenSwitchAndDestroy")){
			this.breakTicks = config.getInt("game.ticksBetweenSwitchAndDestroy");
		} else {
			this.breakTicks = 10;
		}
		
		this.bombDisplayName = config.getString("game.bombs.displayName", "&4Bomb");
		this.bombPointsOnBreak = config.getInt("game.bombs.pointsOnBreak", 10);
		this.ticksTillExplosion = config.getInt("game.bombs.ticksTillExplosion", 15);
		if(this.config.isSet("game.bombs.lore") && this.config.isList("game.bombs.lore")) {
			this.bombLore = new ArrayList<>(config.getStringList("game.bombs.lore"));
		} else {
			this.bombLore = new ArrayList<>();
			bombLore.add("&4Caution: &6explosive!");
		}
		for(int i = 0 ; i < bombLore.size(); i++){
			bombLore.set(i, chatColor(bombLore.get(i)));
		}
		return true;
	}
	
	private boolean hasToBeFilled() {
		for(int slot = 0 ; slot < 54 ; slot++){
			if(this.inv.getItem(slot) == null) return true;
		}
		return false;
	}
	
	public ArrayList<Integer> scanRows(){
		ArrayList<Integer> toBreak = new ArrayList<>();
		int slot, c;
		int colorInRow;
		String name;
		for(int i = 0 ; i<6 ; i++){
			// scan row number i
			c = 0;
			name = grid[i*9].getName();
			colorInRow = 1;
			c++;
			//Bukkit.getConsoleSender().sendMessage("row: " + i);
			while(c<9){
				slot = i*9 + c;
				//Bukkit.getConsoleSender().sendMessage("slot: " + slot + " (column: "+ c + ")   current name: " + name);
				while(c<9 && slot<54 && name.equals(grid[slot].getName())){
					//Bukkit.getConsoleSender().sendMessage("same name in column: " + c);
					colorInRow++;
					c++;
					slot++;
				}
				//Bukkit.getConsoleSender().sendMessage("new name in column: "+ c + "   exited with " + colorInRow + " in a row");
				if(colorInRow >= 3){
					for(int breakSlot = slot - 1; breakSlot >= slot - colorInRow; breakSlot -- ){
						// continue for not matchable gems
						if(grid[breakSlot].getName().equalsIgnoreCase("Bomb")) continue;
						toBreak.add(breakSlot);
					}
				}
				if(c<9)name = grid[slot].getName();
				colorInRow = 1;
				c++;
			}
		}
		return toBreak;
	}
	
	
	private boolean moveLeft(){
		ArrayList<Integer> add = new ArrayList<>();
		for(int column = 0; column < 9 ; column++){
			for(int row = 0; row < 6; row ++){
				int slot = column + 9 * row;
				if(column < 8 && row < 5){
					add.clear();
					Collections.addAll(add, 1, 9);
				} else if(column < 8 && row == 5){
					add.clear();
					Collections.addAll(add, 1);
				} else if(column == 8 && row < 5){
					add.clear();
					Collections.addAll(add, 9);
				} else {
					continue;
				}
				for(int i : add){
					
					Gem oldGem = this.grid[slot];
					this.grid[slot] = this.grid[slot + i];
					this.grid[slot + i] = oldGem;
		
					if(!scanColumns().isEmpty() || !scanRows().isEmpty()){
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
	
	public ArrayList<Integer> scanColumns(){
		ArrayList<Integer> toBreak = new ArrayList<>();
		int slot, c;
		int colorInRow;
		String name;
		for(int i = 0 ; i<9 ; i++){
			// scan column number i
			c = 0;
			name = grid[i].getName();
			c++;
			colorInRow = 1;
			while(c<6){
				slot = i + c*9;
				
				while(c<6 && slot<54 && name.equals(grid[slot].getName())){
					colorInRow++;
					c++;
					slot = i + c*9;
				}
				if(colorInRow >= 3){
					for(int breakSlot = slot - 9; breakSlot >= slot - colorInRow*9; breakSlot -= 9 ){
						// continue for not matchable gems
						if(grid[breakSlot].getName().equalsIgnoreCase("Bomb")) continue;
						toBreak.add(breakSlot);
					}
				}
				if(c<6)name = grid[slot].getName();
				colorInRow = 1;
				c++;
			}
		}
		return toBreak;
	}



	private boolean loadGems() {
		boolean worked = true;

		if(Main.debug){
			Bukkit.getConsoleSender().sendMessage("number of loaded gems: " + gems.size());
			Bukkit.getConsoleSender().sendMessage("using " + gemsNum + " gems");
		}

		if(gemsNum > gems.size()){
			Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] Could not load enough gems! Quiting game.");
			Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] You can add some in the config file ;)");
			manager.removeGame(this);
			return false;
		}

		ArrayList<Integer> savedGems = new ArrayList<>();
		int key;
		for(int i = 0; i < gemsNum; i++){
			key = getKeyWithWeights(savedGems);

			if(key<0){
				Bukkit.getLogger().log(Level.WARNING, "[GemCrush] Problem while choosing the gems");
				gemsNum --;
				continue;
			}

			if(gems.get(Integer.toString(key)) == null){
				Bukkit.getConsoleSender().sendMessage("gem " + key + " is null!");
				manager.removeGame(this);
				return false;
			}

			if(Main.debug){
				Bukkit.getConsoleSender().sendMessage("chose gem with the number " + key);
				Bukkit.getConsoleSender().sendMessage("using: " + gems.get(Integer.toString(key)).getName());
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
		for(String gemName : gems.keySet()){
			if(!savedGems.contains(Integer.parseInt(gemName)))
				totalWeight += ((NormalGem)gems.get(gemName)).getPossibility();
		}
		double weightedKey = rand.nextDouble() * totalWeight;
		for(String gemName : gems.keySet()){
			if(savedGems.contains(Integer.parseInt(gemName))) continue;
			totalWeight -= ((NormalGem)gems.get(gemName)).getPossibility();
			if(totalWeight < weightedKey){
				key = Integer.parseInt(gemName);
				if(Main.debug)Bukkit.getConsoleSender().sendMessage("Key: " + key + "   totalWeight was: " + totalWeight);
				return key;
			}
		}
		return -1;
	}
	
	
	@Override
	public void run() {
		//if(Main.debug)Bukkit.getConsoleSender().sendMessage("Current state: " + this.state.toString());
		switch(this.state){
			case FILLING:
				checkCycles = 0;
				for(int column = 8 ; column > -1 ; column--){
					for(int row = 5; row > -1 ; row --){
						int slot = row*9 + column;
						if(this.grid[slot] == null){
							if(row == 0){
									grid[slot] = new NormalGem((NormalGem) usedNormalGems.get(Integer.toString(rand.nextInt(gemsNum))));
									// with break the filling is slower
									//break;
									this.inv.setItem(slot, grid[slot].getItem());
									row--;
								} else {
								if(this.grid[slot-9] != null){
									if(grid[slot-9] instanceof Bomb){
										bombSpawned.remove(Integer.valueOf(slot-9));
										bombSpawned.add(slot);
									}
									this.grid[slot] = this.grid[slot-9];
									this.inv.setItem(slot, grid[slot - 9].getItem());
									this.grid[slot-9] = null;
									this.inv.setItem(slot-9, null);
									row--;
								}
							}
							
							if (playSounds) {
								double randDouble = rand.nextDouble();
								if(randDouble < 0.25) {
									if (randDouble < 0.06) {
										player.playSound(player.getLocation(), Sounds.NOTE_STICKS.bukkitSound(), volume, 5f);
									} else if (randDouble < 0.13) {
										player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), volume, 3f);
									} else if (randDouble < 0.19) {
										player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), volume, 1f);
									} else if (randDouble < 0.25) {
										player.playSound(player.getLocation(), Sounds.NOTE_PIANO.bukkitSound(), volume, 3f);
									/*} else if(randDouble < 0.09){
										player.playSound(player.getLocation(), Sounds.NOTE_PIANO.bukkitSound(), volume, 1f);
									} else if(randDouble < 0.12){
										player.playSound(player.getLocation(), Sounds.FIREWORK_LAUNCH.bukkitSound(), volume, 1f);
									} else {
										player.playSound(player.getLocation(), Sounds.FIREWORK_TWINKLE.bukkitSound(), volume, 1f);
									*/}
								}
							}
						}
					}
				}
				
				if(!hasToBeFilled()){
					if(!this.breakAll(true) && bombSpawned.isEmpty()) {
						if(moves > 0) {
							this.setState(GameState.PLAY);
						} else {
							this.setState(GameState.FINISHED);
							won();
						}
					} else if(!this.breakAll(false) && !bombSpawned.isEmpty()){
						ArrayList<Integer> toBreak = new ArrayList<>();
						ArrayList<Integer> addToBreak;
						for(int bombSlot : bombSpawned){
							addToBreak = getSurroundingSlots(bombSlot);
							for(int slot : addToBreak){
								if(!toBreak.contains(slot)){
									toBreak.add(slot);
								}
							}
							if(!toBreak.contains(bombSlot)){
								toBreak.add(bombSlot);
							}
						}
						bombSpawned.clear();
						if(Main.debug)Bukkit.getConsoleSender().sendMessage("scheduled " + toBreak + " for breaking");
						
						breakTimer = new BreakTimer(this, toBreak, ticksTillExplosion, true);
						setState(GameState.BREAKING);
					}
				}
				break;
			case FINISHED:
				break;
			case PLAY:
				if(checkCycles > 5){
					if(!moveLeft()){
						refill();
						checkCycles = 0;
					} else {
						checkCycles = 0;
					}
				} else {
					checkCycles ++;
				}
				break;
			case BREAKING:
				break;
			default:
				break;
		}
	}
	
	private ArrayList<Integer> getSurroundingSlots(int slot){
		ArrayList<Integer> surroundingSlots = new ArrayList<>();
		
		for(int i : getAdd(slot)){
			surroundingSlots.add(slot + i);
		}
		return surroundingSlots;
	}
	
	private ArrayList<Integer> getAdd(int slot){
		ArrayList<Integer> add = new ArrayList<>();
		if(slot == 0){// corner left top
			add.addAll(Arrays.asList(1, 9, 10));
			
		} else if (slot == 8){// corner top right
			add.addAll(Arrays.asList(-1, 8, 9));
			
		} else if (slot == 45){// corner bottom left
			add.addAll(Arrays.asList(-9, -8, 1));
			
		} else if (slot == 53){// corner bottom right
			add.addAll(Arrays.asList(-10, -9, -1));
			
		} else if(slot>0 && slot<8){// edge top
			add.addAll(Arrays.asList(-1, 1, 8, 9, 10));
			
		} else if(slot == 17 || slot == 26 || slot == 35 || slot == 44){// edge right
			add.addAll(Arrays.asList(-10, -9, -1, 8, 9));
			
		} else if(slot>45 && slot<53){// edge bottom
			add.addAll(Arrays.asList(-1, -10, -9, -8, 1));
			
		} else if(slot == 9 || slot == 18 || slot == 27 || slot == 36){// edge left
			add.addAll(Arrays.asList(-9, -8, 1, 9, 10));
			
		} else {
			add.addAll(Arrays.asList(-10, -9, -8, -1, 1, 8, 9, 10));
		}
		
		return add;
	}
	
	
	
	private void refill() {
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("Refilling...");
		for(int i = 0; i < 54 ; i++){
			grid[i] = null;
		}
		this.inv.clear();
		this.state = GameState.FILLING;
	}


	@SuppressWarnings("deprecation")
	private boolean breakAll(boolean schedule) {
		ArrayList<Integer> toBreak = new ArrayList<>();
		toBreak.addAll(this.scanColumns());
		toBreak.addAll(this.scanRows());
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("scheduled " + toBreak + " for breaking");
		if(toBreak.isEmpty()) return false;
		if(schedule) {
			ArrayList<Integer> all = new ArrayList<>();
			for (int i = 0; i < inv.getSize(); i++) {
				if (bombSpawned.contains(i))
					continue;
				all.add(i);
			}
			shine(all, false);
			shine(toBreak, true);
			player.updateInventory();
			for(int slot : toBreak){
				inv.setItem(slot, grid[slot].getItem());
			}
			breakTimer = new BreakTimer(this, toBreak, breakTicks);
			setState(GameState.BREAKING);
		}
		return true;
	}
	
	
	private String chatColor(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
	
	
	public void won() {
		if(rule.isSaveStats()){
			manager.saveStats(player.getUniqueId(), points, rule.getKey());
		}
		manager.onGameEnd(rule.getKey(), points, player, payOut);
	}
	
	
	public GameState getState() {
		return state;
	}
	
	public void setState(GameState state) {
		this.state = state;
		switch (state){
			
			
			default:
				break;
		}
	}
	
	public UUID getUUID() {
		return playerUUID;
	}
	
	public boolean switchGems(int lowerSlot, int higherSlot) {
		Gem oldGem = this.grid[lowerSlot];
		this.grid[lowerSlot] = this.grid[higherSlot];
		this.grid[higherSlot] = oldGem;
		
		/*
		breakAll() will set the state to BREAKING and will start the timer if there are blocks to break
		otherwise it will return false and the following if block will reset the grid
		*/
		if(!breakAll(true)){
			oldGem = this.grid[lowerSlot];
			this.grid[lowerSlot] = this.grid[higherSlot];
			this.grid[higherSlot] = oldGem;
			return false;
		}
		
		moves --;
		this.inv.setItem(lowerSlot, grid[lowerSlot].getItem());
		this.inv.setItem(higherSlot, grid[higherSlot].getItem());
		//setInventory();
		updater.updateTitle(player, ChatColor.translateAlternateColorCodes('&', title.replaceAll("%moves%", moves + "").replaceAll("%score%", points+"")));
		return true;
	}
	
	public void breakGems(ArrayList<Integer> toBreak) {
		for(int i : toBreak) {
			if(grid[i] != null){
				points += grid[i].getPointsOnBreak();
				this.grid[i] = null;
				this.inv.setItem(i, null);
			} else if(this.enableBombs){//If a gem is null here already a slot has to be broken twice and will spawn a bomb
				grid[i] = new Bomb(bombDisplayName, bombLore, bombPointsOnBreak);
				if(playSounds)player.playSound(player.getLocation(), Sounds.FUSE.bukkitSound(), volume, 1f);
				grid[i].setItem(updater.addGlow(grid[i].getItem()));
				this.inv.setItem(i, grid[i].getItem());
				bombSpawned.add(i);
			}
		}
		updater.updateTitle(player, ChatColor.translateAlternateColorCodes('&', title.replaceAll("%moves%", moves + "").replaceAll("%score%", points+"")));
		//setInventory(); get rid of flicker
		setState(GameState.FILLING);
	}
	
	public void shutDown() {
		int id = this.getTaskId();
		if(Bukkit.getScheduler().isCurrentlyRunning(this.getTaskId()) || Bukkit.getScheduler().isQueued(this.getTaskId()))this.cancel();
		if(breakTimer != null) {
			this.breakTimer.cancel();
			this.breakTimer = null;
		}
		this.inv = Bukkit.createInventory(null, 54, "Game was shut down!");
		//player.closeInventory();
	}
	
	public void shine(int slot, boolean b) {
		if(b){
			//this.grid[slot].shine(b);
			if(grid[slot] instanceof NormalGem) {
				grid[slot].setItem(updater.addGlow(grid[slot].getItem()));
			}
		} else {
			if(grid[slot] instanceof NormalGem) {
				grid[slot].setItem(updater.removeGlow(grid[slot].getItem()));
			}
		}
		this.inv.setItem(slot, grid[slot].getItem());
	}
	
	public void shine(ArrayList<Integer> slots, boolean b) {
		for (int slot : slots){
			shine(slot, b);
		}
	}
	
	public void playExplodingBomb() {
		if(playSounds)player.playSound(player.getLocation(), Sounds.EXPLODE.bukkitSound(), volume, 1f);
	}
	
	public void playBreakSound() {
		if(playSounds)player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), volume, 1f);
	}

	public boolean isPlaySounds() {
		return playSounds;
	}

	public GameRules getRule() {
		return rule;
	}
}
