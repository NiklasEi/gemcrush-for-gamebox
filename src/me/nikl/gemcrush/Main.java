package me.nikl.gemcrush;

import me.nikl.gamebox.ClickAction;
import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.guis.GUIManager;
import me.nikl.gamebox.guis.button.AButton;
import me.nikl.gamebox.guis.gui.game.GameGui;
import me.nikl.gemcrush.game.GameManager;
import me.nikl.gemcrush.game.GameRules;
import me.nikl.gemcrush.nms.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class Main extends JavaPlugin{
	public final static boolean debug = false;

	private GameManager manager;
	private FileConfiguration config, stats;
	private File con, sta;
	public static Economy econ = null;
	public static boolean playSounds = true;
	public Boolean econEnabled;
	public Language lang;
	public boolean disabled = false;

	public static final String gameID = "gemcrush";
	
	private InvTitle updater;

	public me.nikl.gamebox.GameBox gameBox;
	
	@Override
	public void onEnable(){
		
		if (!setupUpdater()) {
			getLogger().severe("Your server version is not compatible with this plugin!");
			getLogger().severe("    Please make sure, you are running the newest version");
			
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}


		this.con = new File(this.getDataFolder().toString() + File.separatorChar + "config.yml");
		this.sta = new File(this.getDataFolder().toString() + File.separatorChar + "stats.yml");

		reload();
		if(disabled) return;

		hook();
		if(disabled) return;


		checkStatsStructure();
	}

	private void hook() {
		if(Bukkit.getPluginManager().getPlugin("GameBox") == null || !Bukkit.getPluginManager().getPlugin("GameBox").isEnabled()){
			Bukkit.getLogger().log(Level.WARNING, " GameBox not found");
			Bukkit.getLogger().log(Level.WARNING, " Continuing as standalone");
			Bukkit.getPluginManager().disablePlugin(this);
			disabled = true;
			return;
		}





		gameBox = (me.nikl.gamebox.GameBox)Bukkit.getPluginManager().getPlugin("GameBox");


		// disable economy if it is disabled for either one of the plugins
		this.econEnabled = this.econEnabled && gameBox.getEconEnabled();
		playSounds = playSounds && GameBox.playSounds;

		GUIManager guiManager = gameBox.getPluginManager().getGuiManager();

		this.manager = new GameManager(this);

		gameBox.getPluginManager().registerGame(manager, gameID, Language.name);

		GameGui gameGui = new GameGui(gameBox, guiManager, 54, gameID, "main");



		Map<String, GameRules> gameTypes = new HashMap<>();

		if(config.isConfigurationSection("gameBox.gameButtons")){
			ConfigurationSection gameButtons = config.getConfigurationSection("gameBox.gameButtons");
			ConfigurationSection buttonSec;
			int moves, numberOfGems;
			double cost;
			boolean bombs;

			String displayName;
			ArrayList<String> lore;

			GameRules rules;

			for(String buttonID : gameButtons.getKeys(false)){
				buttonSec = gameButtons.getConfigurationSection(buttonID);


				if(!buttonSec.isString("materialData")){
					Bukkit.getLogger().log(Level.WARNING, " missing material data under: gameBox.gameButtons." + buttonID + "        can not load the button");
					continue;
				}

				ItemStack mat = getItemStack(buttonSec.getString("materialData"));
				if(mat == null){
					Bukkit.getLogger().log(Level.WARNING, " error loading: gameBox.gameButtons." + buttonID);
					Bukkit.getLogger().log(Level.WARNING, "     invalid material data");
					continue;
				}


				AButton button =  new AButton(mat.getData(), 1);
				ItemMeta meta = button.getItemMeta();

				if(buttonSec.isString("displayName")){
					displayName = chatColor(buttonSec.getString("displayName"));
					meta.setDisplayName(displayName);
				}

				if(buttonSec.isList("lore")){
					lore = new ArrayList<>(buttonSec.getStringList("lore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, chatColor(lore.get(i)));
					}
					meta.setLore(lore);
				}

				button.setItemMeta(meta);
				button.setAction(ClickAction.START_GAME);
				button.setArgs(gameID, buttonID);

				bombs = buttonSec.getBoolean("bombs", true);
				moves = buttonSec.getInt("moves", 20);
				numberOfGems = buttonSec.getInt("differentGems", 8);
				cost = buttonSec.getDouble("cost", 0.);


				rules = new GameRules(moves, numberOfGems, bombs, cost);

				if(buttonSec.isInt("slot")){
					gameGui.setButton(button, buttonSec.getInt("slot"));
				} else {
					gameGui.setButton(button);
				}

				gameTypes.put(buttonID, rules);
			}
		}


		this.manager.setGameTypes(gameTypes);



		getMainButton:
		if(config.isConfigurationSection("gameBox.mainButton")){
			ConfigurationSection mainButtonSec = config.getConfigurationSection("gameBox.mainButton");
			if(!mainButtonSec.isString("materialData")) break getMainButton;

			ItemStack gameButton = getItemStack(mainButtonSec.getString("materialData"));
			if(gameButton == null){
				gameButton = (new ItemStack(Material.EMERALD));
			}
			ItemMeta meta = gameButton.getItemMeta();
			meta.setDisplayName(chatColor(mainButtonSec.getString("displayName","&3GemCrush")));
			if(mainButtonSec.isList("lore")){
				ArrayList<String> lore = new ArrayList<>(mainButtonSec.getStringList("lore"));
				for(int i = 0; i < lore.size();i++){
					lore.set(i, chatColor(lore.get(i)));
				}
				meta.setLore(lore);
			}
			gameButton.setItemMeta(meta);
			guiManager.registerGameGUI(gameID, "main", gameGui, gameButton, "gemcrush", "gc");
		} else {
			Bukkit.getLogger().log(Level.WARNING, " Missing or wrong configured main button in the configuration file!");
		}
	}

	private void checkStatsStructure() {
		if(this.stats.isString("structure")){
			if(this.stats.getString("structure").equals("v1.2.1")) return;
		}
		for(String key: stats.getKeys(false)){
			if(stats.isInt(key)){
				stats.set(key + ".stat", stats.getInt(key));
			}
		}
		stats.set("structure", "v1.2.1");
	}
	
	@Override
	public void onDisable(){
		if(stats!=null){
			try {
				this.stats.save(sta);
			} catch (IOException e) {
				getLogger().log(Level.SEVERE, "Could not save statistics", e);
			}		
		}
	}
	
	private boolean setupUpdater() {
		String version;
		
		try {
			version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];
		} catch (ArrayIndexOutOfBoundsException whatVersionAreYouUsingException) {
			return false;
		}
		
		if(debug) getLogger().info("Your server is running version " + version);
		
		switch (version) {
			case "v1_10_R1":
				updater = new Update_1_10_R1();
				
				break;
			case "v1_9_R2":
				updater = new Update_1_9_R2();
				
				break;
			case "v1_9_R1":
				updater = new Update_1_9_R1();
				
				break;
			case "v1_8_R3":
				updater = new Update_1_8_R3();
				
				break;
			case "v1_8_R2":
				updater = new Update_1_8_R2();
				
				break;
			case "v1_8_R1":
				updater = new Update_1_8_R1();
				
				break;
			case "v1_11_R1":
				updater = new Update_1_11_R1();
				
				break;
		}
		return updater != null;
	}
	
	public InvTitle getUpdater(){
		return this.updater;
	}
	
    private boolean setupEconomy(){
    	if (getServer().getPluginManager().getPlugin("Vault") == null) {
    		return false;
    	}
    	RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    	if (rsp == null) {
    		return false;
    	}
    	econ = rsp.getProvider();
    	return econ != null;
    }
	
	public void reloadConfig(){
		try { 
			this.config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(this.con), "UTF-8")); 
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace(); 
		}
		
		InputStream defConfigStream = this.getResource("config.yml"); 
		if (defConfigStream != null){		
			@SuppressWarnings("deprecation") 
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream); 
			this.config.setDefaults(defConfig); 
		} 
	} 
	
	public GameManager getManager() {
		return manager;
	}
	
	public void reload(){
		//shut down all games on reload
		if(manager != null){
			manager.shutDown();
		}
		if(!con.exists()){
			this.saveResource("config.yml", false);
		}
		if(!sta.exists()){
			try {
				sta.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		reloadConfig();
		
		if(config.isBoolean("playSounds"))
			playSounds = config.getBoolean("playSounds");
		
		// if the statistics file was already loaded: save the newest version as file
		if(stats!=null){
			try {
				this.stats.save(sta);
			} catch (IOException e) {
				getLogger().log(Level.SEVERE, "[GemCrush] Could not save statistics", e);
			}
		}
		
		// load stats file
		try {
			this.stats = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(this.sta), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} 
		
		
		this.lang = new Language(this);
		
		this.econEnabled = false;
		if(config.getBoolean("economy.enabled")){
			this.econEnabled = true;
			if(!setupEconomy()){
				Bukkit.getLogger().log(Level.WARNING, " Failed to set up economy...");
				Bukkit.getLogger().log(Level.WARNING, " Do you have vault and an economy plugin installed?");
				this.econEnabled = false;
			}
		}
	}

	public void setStatistics(UUID player, int score) {
		if(this.stats == null) return;
		String uuid = player.toString();
		if(stats.isInt(uuid)){
			stats.set(uuid + ".stat", stats.getInt(uuid));
		}
		if(!stats.isInt(uuid + ".stat")){
			stats.set(uuid + ".stat", score);
		} else {
			int oldScore = stats.getInt(uuid + ".stat");
			if(score > oldScore){
				stats.set(uuid + ".stat", score);
			}
		}
	}
	
	/**
	 * Set a timestamp for the specified player and action
	 * @param player Player to set the timestamp for
	 * @param action Action to set the timestamp for
	 */
	public void setTimestamp(UUID player, String action) {
		if(this.stats == null) return;
		stats.set(player.toString() + ".timestamps." + action, System.currentTimeMillis());
		if(debug)Bukkit.getConsoleSender().sendMessage("set Timestamp to: " + stats.getLong(player.toString() + ".timestamps." + action, 0));
	}
	
	/**
	 * Get the specified timestamp from the stats file
	 * @param player Player
	 * @param action Action
	 * @return timestamp for the specified player and action
	 */
	public long getTimestamp(UUID player, String action){
		return stats.getLong(player.toString() + ".timestamps." + action, 0);
	}
	
	public FileConfiguration getStatistics(){
		return this.stats;
	}

	public FileConfiguration getConfig() {
		return config;
	}
	
    public String chatColor(String message){
    	return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public Boolean getEconEnabled(){
    	return this.econEnabled;
    }
	
	public void resetStatistics() {
		for(String uuid : stats.getKeys(false)){
			if(stats.isInt(uuid)){
				stats.set(uuid + ".stat", 0);
			} else if(stats.isInt(uuid + ".stat")){
				stats.set(uuid + ".stat", 0);
			}
		}
	}

	private ItemStack getItemStack(String itemPath){
    	Material mat; short data;
		String[] obj = itemPath.split(":");

		if (obj.length == 2) {
			try {
				mat = Material.matchMaterial(obj[0]);
			} catch (Exception e) {
				return null; // material name doesn't exist
			}

			try {
				data = Short.valueOf(obj[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return null; // data not a number
			}

			//noinspection deprecation
			if(mat == null) return null;
			ItemStack stack = new ItemStack(mat);
			stack.setDurability(data);
			return stack;
		} else {
			try {
				mat = Material.matchMaterial(obj[0]);
			} catch (Exception e) {
				return null; // material name doesn't exist
			}
			//noinspection deprecation
			return (mat == null ? null : new ItemStack(mat));
		}
	}
}

