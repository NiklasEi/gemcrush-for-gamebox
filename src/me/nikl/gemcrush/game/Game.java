package me.nikl.gemcrush.game;

import java.util.*;
import java.util.logging.Level;

import me.nikl.gemcrush.gems.Bomb;
import me.nikl.gemcrush.gems.Gem;
import me.nikl.gemcrush.gems.NormalGem;
import me.nikl.gemcrush.nms.InvTitle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;
import org.bukkit.scheduler.BukkitScheduler;


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
	
	private int nextBombSlot;
	private int explodingBomb;
	
	// check for existing move every x cycles
	// reset cycles to 0 on breaking/filling state and add one every run in case PLAY
	private int checkCycles;
	
	public Game(Main plugin, UUID playerUUID){
		this.plugin = plugin;
		this.lang = plugin.lang;
		this.config = plugin.getConfig();
		this.manager = plugin.getManager();
		this.playerUUID = playerUUID;
		this.player = Bukkit.getPlayer(playerUUID);
		this.grid = new Gem[54];
		this.gems = new HashMap<>();
		this.usedNormalGems = new HashMap<>();
		this.moves = 20;
		this.points = 0;
		
		// otherwise a bomb will be spawned
		this.nextBombSlot = -1;
		
		// no exploding bomb at the beginning;
		explodingBomb = -1;
		
		this.gemsNum = 9; // 6
		
		
		
		this.updater = plugin.getUpdater();
		
		
		if(player == null){
			Bukkit.getConsoleSender().sendMessage("Player is null!");//XXX
			manager.removeGame(this);
			return;
		}
		if(config == null){
			Bukkit.getConsoleSender().sendMessage(Main.prefix + " Failed to load config!");
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}
		
		if(!loadOptions()){
			Bukkit.getConsoleSender().sendMessage("You are missing options in the configuration file.");//XXX
			Bukkit.getConsoleSender().sendMessage("Game will be started with defaults. Please get an up to date config file.");//XXX
		}
		
		if(!loadGems()){
			player.sendMessage(chatColor(Main.prefix + " &2Configuration error. Please contact the server owner!"));
			return;
		}
		
		
		this.title = lang.TITLE_GAME;
		this.inv = Bukkit.getServer().createInventory(null, 54, chatColor(title.replaceAll("%moves%", moves + "").replaceAll("%score%", points + "")));
		
		// this basically starts the game
		this.state = GameState.FILLING;
		player.openInventory(this.inv);
		//player.sendMessage("Game was started"); //XXX
		this.runTaskTimer(Main.getPlugin(Main.class), 0, this.moveTicks);
	}
	
	private boolean loadOptions() {
		if(this.config.isSet("game.ticksBetweenMovement") && this.config.isInt("game.ticksBetweenMovement")){
			this.moveTicks = config.getInt("game.ticksBetweenMovement");
		} else {
			this.moveTicks = 5;
		}
		
		if(this.config.isSet("game.numberOfNormalGems") && this.config.isInt("game.numberOfNormalGems")){
			this.gemsNum = config.getInt("game.numberOfNormalGems");
		} else {
			this.gemsNum = 8;
		}
		
		if(this.config.isSet("game.ticksBetweenSwitchAndDestroy") && this.config.isInt("game.ticksBetweenSwitchAndDestroy")){
			this.breakTicks = config.getInt("game.ticksBetweenSwitchAndDestroy");
		} else {
			this.breakTicks = 10;
		}
		
		if(this.config.isSet("game.moves") && this.config.isInt("game.moves")){
			this.moves = config.getInt("game.moves");
		} else {
			this.moves = 20;
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
			while(c<9){
				slot = i*9 + c;
				colorInRow = 1;
				
				
				c++;
				slot ++;
				
				while(c<9 && slot<54 && (name.equals(grid[slot].getName()) || name.equalsIgnoreCase("bomb"))){
					colorInRow++;
					c++;
					slot++;
				}
				if(colorInRow < 3){
					if(c<9)
						name = grid[slot].getName();
					if(name.equalsIgnoreCase("bomb") && c<8)
						name = grid[slot + 1].getName();
					continue;
				} else {
					for(int breakSlot = slot - 1; breakSlot >= slot - colorInRow; breakSlot -- ){
						toBreak.add(breakSlot);
					}
				}
				
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
			while(c<6){
				colorInRow = 1;
				
				
				c++;
				slot = i + c*9;
				
				while(c<6 && slot<54 && (name.equals(grid[slot].getName()) || name.equalsIgnoreCase("bomb"))){
					colorInRow++;
					c++;
					slot = i + c*9;
				}
				if(colorInRow < 3){
					if(c<6)
						name = grid[slot].getName();
					if(name.equalsIgnoreCase("bomb") && c<5)
						name = grid[slot + 9].getName();
					continue;
				} else {
					for(int breakSlot = slot - 9; breakSlot >= slot - colorInRow*9; breakSlot -= 9 ){
						toBreak.add(breakSlot);
					}
				}
				
			}
		}
		return toBreak;
	}
	
	
	
	private boolean loadGems() {
		boolean worked = true;
		
		Material mat = null;
		int data = 0;
		int index = 0;
		
		if(!this.config.isConfigurationSection("normalGems")){
			Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Outdated configuration file! Game cannot be started.");
			return false;
		}
		
		ConfigurationSection section = this.config.getConfigurationSection("normalGems");
		
		for(String key : section.getKeys(false)){
			if(Main.debug)Bukkit.getConsoleSender().sendMessage("getting " + key);
			if(!section.isSet(key + ".material")){
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + key);
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Skipping the gem. Is the material set?");
				continue;
			}
			if(!section.isSet(key + ".displayName") || !section.isString(key + ".displayName")){
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + key);
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Skipping the gem. Is the displayName set?");
				continue;
			}
			
			String value = section.getString(key + ".material");
			String[] obj = value.split(":");
			String name = chatColor(section.getString(key + ".displayName"));
			
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
			if(mat == null){
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + "normalGems." + key );
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "The material is not valid! Maybe your minecraft version is too old for it?");
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Change the material or delete the gem. Skipping..." );
				continue;
			}
			
			if(Main.debug){
				Bukkit.getConsoleSender().sendMessage("saving gem " + name + " as " + index);
			}
			
			if(obj.length == 1){
				this.gems.put(Integer.toString(index), new NormalGem(mat, name));
			} else {
				this.gems.put(Integer.toString(index), new NormalGem(mat, name, (short) data));
			}
			if(section.isSet(key + ".pointsOnBreak") && section.isInt(key + ".pointsOnBreak")){
				this.gems.get(Integer.toString(index)).setPointsOnBreak(section.getInt(key + ".pointsOnBreak"));
			}
			if(section.isSet(key + ".probability") && (section.isDouble(key + ".probability") || section.isInt(key + ".probability"))){
				if(Main.debug)Bukkit.getConsoleSender().sendMessage("set probability of " + name + " to " + section.getDouble(key + ".probability"));
				((NormalGem) this.gems.get(Integer.toString(index))).setPossibility(section.getDouble(key + ".probability"));
			}
			index++;
		}
		
		if(Main.debug){
			Bukkit.getConsoleSender().sendMessage("number of loaded gems: " + gems.size());
			Bukkit.getConsoleSender().sendMessage("using " + gemsNum + " gems");
		}
		
		if(gemsNum > gems.size()){
			Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] Could not load enough gems! Quiting game.");
			Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] You can add some in the config file ;)");
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
				Random rand = new Random();
				boolean spawnBomb = false;
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
									continue;
								} else {
								if(this.grid[slot-9] != null){
									if(this.grid[slot-9].getName().equalsIgnoreCase("bomb") && ((Bomb) this.grid[slot-9]).isExploding()){
										//the bomb that is being moved was ignited already and will explode after the gamestate is changed from filling
										// track the bomb!
										this.explodingBomb = slot;
									}
									this.grid[slot] = this.grid[slot-9];
									this.inv.setItem(slot, grid[slot-9].getItem());
									this.grid[slot-9] = null;
									this.inv.setItem(slot-9, null);
									row--;
									continue;
								} else {
									continue;
								}
							}
						}
					}
				}
				//setInventory(); // maybe flickering gets fixed here?
				if(!hasToBeFilled()){
					if(explodingBomb > -1){
						// explode the bomb!
						ArrayList<Integer> toBreak = new ArrayList<>();//toDo
						toBreak.add(explodingBomb);
						new BreakTimer(this, toBreak, breakTicks);
						setState(GameState.BREAKING);
						explodingBomb = -1;
						break;
					}
					if(!this.breakAll()) {
						if(moves > 0) {
							this.setState(GameState.PLAY);
						} else {
							this.setState(GameState.FINISHED);
							won();
						}
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
	
	private void refill() {
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("Refilling...");
		for(int i = 0; i < 54 ; i++){
			grid[i] = null;
		}
		this.inv.clear();
		this.state = GameState.FILLING;
	}
	
	private boolean breakAll() {
		ArrayList<Integer> toBreak = new ArrayList<>();
		toBreak.addAll(this.scanColumns());
		toBreak.addAll(this.scanRows());
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("scheduled " + toBreak + " for breaking");
		if(toBreak.isEmpty()) return false;
		
		breakTimer = new BreakTimer(this, toBreak, breakTicks);
		setState(GameState.BREAKING);
		return true;
	}
	
	/*
	private void setInventory() {
		for(int i=0;i<54;i++){
			this.inv.setItem(i, this.grid[i] == null ? null : this.grid[i].getItem());
		}
		updater.updateTitle(player, ChatColor.translateAlternateColorCodes('&', title.replaceAll("%moves%", moves + "").replaceAll("%score%", points+"")));
	}*/
	
	
	private String chatColor(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
	
	
	public void won() {
		manager.onGameEnd(points, player);
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
		if(!breakAll()){
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
				if(grid[i] instanceof Bomb){
					if(((Bomb) grid[i]).isExploding()){
						explodingBomb = i;
					} else {
						((Bomb) grid[i]).setExploding(true);
					}
				} else {
					points += grid[i].getPointsOnBreak();
					this.grid[i] = null;
					this.inv.setItem(i, null);
				}
			} else {//If a gem is null here already a slot has to be broken twice and will spawn a bomb
				grid[i] = new Bomb();
			}
		}
		updater.updateTitle(player, ChatColor.translateAlternateColorCodes('&', title.replaceAll("%moves%", moves + "").replaceAll("%score%", points+"")));
		//setInventory(); get rid of flicker
		setState(GameState.FILLING);
	}
	
	public void shutDown() {
		if(Bukkit.getScheduler().isCurrentlyRunning(this.getTaskId()))
			this.cancel();
		this.inv = Bukkit.createInventory(null, 54, "Game was shut down!");
		if(breakTimer != null) {
			if (Bukkit.getScheduler().isCurrentlyRunning(this.getTaskId()))
				this.breakTimer.cancel();
			this.breakTimer = null;
		}
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
}
