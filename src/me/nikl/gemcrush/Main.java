package me.nikl.gemcrush;

import me.nikl.gamebox.ClickAction;
import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.data.SaveType;
import me.nikl.gamebox.guis.GUIManager;
import me.nikl.gamebox.guis.button.AButton;
import me.nikl.gamebox.guis.gui.game.GameGui;
import me.nikl.gamebox.guis.gui.game.TopListPage;
import me.nikl.gamebox.nms.NMSUtil;
import me.nikl.gemcrush.game.GameManager;
import me.nikl.gemcrush.game.GameRules;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class Main extends JavaPlugin{
	public final static boolean debug = false;

	private GameManager manager;
	private FileConfiguration config;
	private File con;
	public static Economy econ = null;
	public static boolean playSounds = true;
	public Boolean econEnabled;
	public Language lang;
	public boolean disabled = false;

	public static final String gameID = "gemcrush";
	
	private NMSUtil updater;

	public me.nikl.gamebox.GameBox gameBox;

	private final String[][] depends =  new String[][]{
			new String[]{"Vault", "1.5"},
			new String[]{"GameBox", "1.0.1"}
	};
	
	@Override
	public void onEnable(){

		this.con = new File(this.getDataFolder().toString() + File.separatorChar + "config.yml");

		reload();
		if(disabled) return;

		hook();
		if(disabled) return;

		this.updater = gameBox.getNMS();
	}

	private void hook() {
		if (Bukkit.getPluginManager().getPlugin("GameBox") == null || !Bukkit.getPluginManager().getPlugin("GameBox").isEnabled()) {
			Bukkit.getLogger().log(Level.SEVERE, " GameBox not found");
			Bukkit.getLogger().log(Level.SEVERE, "   Get the newest version here:");
			Bukkit.getLogger().log(Level.SEVERE, "   https://www.spigotmc.org/resources/37273/");
			Bukkit.getPluginManager().disablePlugin(this);
			disabled = true;
			return;
		}



		gameBox = (me.nikl.gamebox.GameBox)Bukkit.getPluginManager().getPlugin("GameBox");


		String[] versionString = gameBox.getDescription().getVersion().replaceAll("[^0-9.]", "").split("\\.");
		String[] minVersionString = depends[1][1].split("\\.");
		Integer[] version = new Integer[versionString.length];
		Integer[] minVersion = new Integer[minVersionString.length];

		for(int i = 0; i < minVersionString.length; i++){
			try {
				minVersion[i] = Integer.valueOf(minVersionString[i]);
				version[i] = Integer.valueOf(versionString[i]);
			} catch (NumberFormatException exception){
				exception.printStackTrace();
			}
		}

		for(int i = 0; i < minVersion.length; i++){
			if(minVersion[i] < version[i]) break;
			if(minVersion[i].equals(version[i])) continue;

			Bukkit.getLogger().log(Level.WARNING, " Your GameBox is outdated!");
			Bukkit.getLogger().log(Level.WARNING, " Get the latest version here: https://www.spigotmc.org/resources/37273/");
			Bukkit.getLogger().log(Level.WARNING, " You need at least version " + depends[1][1]);
			Bukkit.getPluginManager().disablePlugin(this);
			disabled = true;
			return;
		}


		// disable economy if it is disabled for either one of the plugins
		this.econEnabled = this.econEnabled && gameBox.getEconEnabled();
		playSounds = playSounds && GameBox.playSounds;

		GUIManager guiManager = gameBox.getPluginManager().getGuiManager();

		this.manager = new GameManager(this);

		gameBox.getPluginManager().registerGame(manager, gameID, Language.name, 1);


		int gameGuiSlots = 54;
		GameGui gameGui = new GameGui(gameBox, guiManager, gameGuiSlots, gameID, "main");
		gameGui.setHelpButton(lang.GAME_HELP);



		Map<String, GameRules> gameTypes = new HashMap<>();

		if(config.isConfigurationSection("gameBox.gameButtons")){
			ConfigurationSection gameButtons = config.getConfigurationSection("gameBox.gameButtons");
			ConfigurationSection buttonSec;
			int moves, numberOfGems;
			double cost;
			boolean bombs, saveStats;

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
				saveStats = buttonSec.getBoolean("saveStats", false);


				rules = new GameRules(this, moves, numberOfGems, bombs, cost, saveStats, buttonID);

				setTheButton:
				if(buttonSec.isInt("slot")){
					int slot = buttonSec.getInt("slot");
					if(slot < 0 || slot >= gameGuiSlots){
						Bukkit.getLogger().log(Level.WARNING, "the slot of gameBox.gameButtons." + buttonID + " is out of the inventory range (0 - "+ gameGuiSlots +")");
						gameGui.setButton(button);
						break setTheButton;
					}
					gameGui.setButton(button, slot);
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


		// get top list buttons
		if(config.isConfigurationSection("gameBox.topListButtons")){
			ConfigurationSection topListButtons = config.getConfigurationSection("gameBox.topListButtons");
			ConfigurationSection buttonSec;

			ArrayList<String> lore;


			for(String buttonID : topListButtons.getKeys(false)){
				buttonSec = topListButtons.getConfigurationSection(buttonID);

				if(!gameTypes.keySet().contains(buttonID)){
					Bukkit.getLogger().log(Level.WARNING, " the top list button 'gameBox.topListButtons." + buttonID + "' does not have a corresponding game button");
					continue;
				}


				if(!gameTypes.get(buttonID).isSaveStats()){
					Bukkit.getLogger().log(Level.WARNING, " the top list buttons 'gameBox.topListButtons." + buttonID + "' corresponding game button has statistics turned off!");
					Bukkit.getLogger().log(Level.WARNING, " With these settings there is no toplist to display");
					continue;
				}

				if(!buttonSec.isString("materialData")){
					Bukkit.getLogger().log(Level.WARNING, " missing material data under: gameBox.topListButtons." + buttonID + "        can not load the button");
					continue;
				}

				ItemStack mat = getItemStack(buttonSec.getString("materialData"));
				if(mat == null){
					Bukkit.getLogger().log(Level.WARNING, " error loading: gameBox.topListButtons." + buttonID);
					Bukkit.getLogger().log(Level.WARNING, "     invalid material data");
					continue;
				}


				AButton button =  new AButton(mat.getData(), 1);
				ItemMeta meta = button.getItemMeta();

				if(buttonSec.isString("displayName")){
					meta.setDisplayName(chatColor(buttonSec.getString("displayName")));
				}


				if(buttonSec.isList("lore")){
					lore = new ArrayList<>(buttonSec.getStringList("lore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, chatColor(lore.get(i)));
					}
					meta.setLore(lore);
				}

				button.setItemMeta(meta);
				button.setAction(ClickAction.SHOW_TOP_LIST);
				button.setArgs(gameID, buttonID + GUIManager.TOP_LIST_KEY_ADDON);



				setTheButton:
				if(buttonSec.isInt("slot")){
					int slot = buttonSec.getInt("slot");
					if(slot < 0 || slot >= gameGuiSlots){
						Bukkit.getLogger().log(Level.WARNING, "the slot of gameBox.topListButtons." + buttonID + " is out of the inventory range (0 - "+ gameGuiSlots +")");
						gameGui.setButton(button);
						break setTheButton;
					}
					gameGui.setButton(button, slot);
				} else {
					gameGui.setButton(button);
				}

				// get skull lore and pass on to the top list page
				if(buttonSec.isList("skullLore")){
					lore = new ArrayList<>(buttonSec.getStringList("skullLore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, chatColor(lore.get(i)));
					}
				} else {
					lore = new ArrayList<>(Arrays.asList("", "No lore specified in the config!"));
				}

				TopListPage topListPage = new TopListPage(gameBox, guiManager, 54, gameID, buttonID + GUIManager.TOP_LIST_KEY_ADDON, buttonSec.isString("inventoryTitle")? ChatColor.translateAlternateColorCodes('&',buttonSec.getString("inventoryTitle")):"Title missing in config", SaveType.SCORE, lore);

				guiManager.registerTopList(gameID, buttonID, topListPage);
			}
		}
	}
	
	@Override
	public void onDisable(){

	}

	
	public NMSUtil getUpdater(){
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
			disabled = true;
		}

		/*
		InputStream defConfigStream = this.getResource("config.yml"); 
		if (defConfigStream != null){		
			@SuppressWarnings("deprecation") 
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream); 
			this.config.setDefaults(defConfig); 
		}
		*/
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
		reloadConfig();
		
		if(config.isBoolean("playSounds"))
			playSounds = config.getBoolean("playSounds");
		
		
		this.lang = new Language(this);
		
		this.econEnabled = false;
		if(config.getBoolean("economy.enabled")){
			this.econEnabled = true;
			if(!setupEconomy()){
				Bukkit.getLogger().log(Level.WARNING, " Failed to set up economy...");
				Bukkit.getLogger().log(Level.WARNING, " Do you have vault and an economy plugin installed?");
				this.econEnabled = false;
				disabled = true;
				return;
			}
		}
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

