package me.nikl.gemcrush.game;

import java.util.*;

import me.nikl.gemcrush.gems.Gem;
import me.nikl.gemcrush.gems.NormalGem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;


public class Game extends BukkitRunnable{

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
	
	// current inventory title
	private String title;
	
	// map with all gems
	private Map<String, Gem> gems;
	
	private Main plugin;
	
	public Game(Main plugin, UUID playerUUID){
		this.plugin = plugin;
		this.lang = plugin.lang;
		this.config = plugin.getConfig();
		this.manager = plugin.getManager();
		this.playerUUID = playerUUID;
		this.player = Bukkit.getPlayer(playerUUID);
		this.grid = new Gem[54];
		
		
		if(player == null){
			manager.removeGame(this);
			return;
		}
		if(config == null){
			Bukkit.getConsoleSender().sendMessage(Main.prefix + " Failed to load config!");
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}
		
		if(!loadGems()){
			//TODO send fail message
			return;
		}
		this.title = lang.TITLE_SET_SHIP_1.replaceAll("%count%", "");
		this.inv = Bukkit.getServer().createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', title));
		
		// this basically starts the game
		this.state = GameState.FILLING;
		player.openInventory(this.inv);
		this.runTaskTimer(Main.getPlugin(Main.class), 0, 5);
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
				
				while(grid[slot] != null && name.equals(grid[slot].getName())){
					colorInRow++;
					c++;
					slot++;
				}
				if(colorInRow < 3){
					if(c<9)
						name = grid[slot].getName();
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
				
				while(grid[slot] != null && name.equals(grid[slot].getName())){
					colorInRow++;
					c++;
					slot = i + c*9;
				}
				if(colorInRow < 3){
					if(c<6)
						name = grid[slot].getName();
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
	
	


	@Override
	public void run() {
		switch(this.state){
			case FILLING:
				Random rand = new Random();
				for(int column = 8 ; column > -1 ; column--){
					for(int row = 5; row > -1 ; row --){
						int slot = row*9 + column;
						if(this.grid[slot] == null){
							if(row == 0){
								grid[slot] = gems.get(Integer.toString(rand.nextInt(6) + 1));
								break;
							} else {
								if(this.grid[slot-9] != null){
									this.grid[slot] = this.grid[slot-9];
									this.grid[slot-9] = null;
									break;
								} else {
									continue;
								}
							}
						}
					}
				}
				setInventory();
				if(!hasToBeFilled()){
					this.state = GameState.PLAY;
				}
				break;
			case FINISHED:
				break;
			case PLAY:
				break;
			default:
				break;
		}
	}
	
	private void setInventory() {
		for(int i=0;i<54;i++){
			this.inv.setItem(i, this.grid[i].getItem());
		}
	}
	
	private boolean loadGems() {
		boolean worked = true;
		
		Material mat = null;
		int data = 0;
		int index = 1;
		for(String key : this.config.getConfigurationSection("items").getKeys(false)){
			
			if(!config.isSet(key + ".material")) return false;
			if(!config.isSet(key + ".displayName") || !config.isString(key + ".displayName")) return false;
			
			String value = config.getString(key + ".material");
			String[] obj = value.split(":");
			String name = config.getString(key + ".displayName");
			
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
			if(mat == null) return false;
			if(obj.length == 1){
				this.gems.put(Integer.toString(index), new NormalGem(mat, name));
			} else {
				this.gems.put(Integer.toString(index), new NormalGem(mat, name, (short) data));
			}
			index++;
		}
		return worked;
	}
	
	
	
	private String chatColor(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
	
	
	public void won(boolean isFirst) {
		if(plugin.getEconEnabled()){
			Main.econ.depositPlayer(player, plugin.getReward());
			player.sendMessage(manager.chatColor(Main.prefix + lang.GAME_WON_MONEY.replaceAll("%reward%", plugin.getReward()+"")));
		} else {
			player.sendMessage(manager.chatColor(Main.prefix + lang.GAME_WON));
		}
		
	}
	
	public void onGameEnd(String winner, String looser){
		String path = "onGameEnd.dispatchCommands";
		if(config.getBoolean(path + ".enabled")){
			List<String> cmdList = config.getStringList(path + ".commands");
			if(cmdList != null && !cmdList.isEmpty()){
				for(String cmd : cmdList){
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%winner%", winner).replaceAll("%looser%", looser));
				}
			}
		}
		path = "onGameEnd.broadcast";
		if(config.getBoolean(path + ".enabled")){
			List<String> broadcastList = config.getStringList(path + ".messages");
			if(broadcastList != null && !broadcastList.isEmpty()){
				for(String message : broadcastList){
					Bukkit.broadcastMessage(chatColor(Main.prefix + " " + message.replaceAll("%winner%", winner).replaceAll("%looser%", looser)));
				}
			}
		}
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
	
	public void switchGems(int lowerSlot, int higherSlot) {
		Gem oldGem = this.grid[lowerSlot];
		this.grid[lowerSlot] = this.grid[higherSlot];
		this.grid[higherSlot] = oldGem;
		setInventory();
	}
}
