package me.nikl.gemcrush;

import me.nikl.gemcrush.cmds.MainCommand;
import me.nikl.gemcrush.cmds.TopCommand;
import me.nikl.gemcrush.game.GameManager;
import me.nikl.gemcrush.nms.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.UUID;
import java.util.logging.Level;

public class Main extends JavaPlugin{
	public final static boolean debug = false;

	private GameManager manager;
	private FileConfiguration config, stats;
	private File con, sta;
	public static Economy econ = null;
	public static String prefix = "[&3GemCrush&r]";
	public static boolean playSounds = true;
	public Boolean econEnabled;
	public Double price;
	public Language lang;
	public boolean disabled;
	
	private InvTitle updater;
	
	@Override
	public void onEnable(){
		
		if (!setupUpdater()) {
			getLogger().severe("Your server version is not compatible with this plugin!");
			
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
        
		this.disabled = false;
		this.con = new File(this.getDataFolder().toString() + File.separatorChar + "config.yml");
		this.sta = new File(this.getDataFolder().toString() + File.separatorChar + "stats.yml");

		reload();
		checkStatsStructure();
		if(disabled) return;

        this.getCommand("gemcrush").setExecutor(new MainCommand(this));
		this.getCommand("gemcrushtop").setExecutor(new TopCommand(this));
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
			case "v1_12_R1":
				updater = new Update_1_12_R1();

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
		// reload config
		try {
			this.config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(con), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
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
		if(getConfig().getBoolean("economy.enabled")){
			this.econEnabled = true;
			if (!setupEconomy()){
				Bukkit.getConsoleSender().sendMessage(chatColor(prefix + " &4No economy found!"));
				getServer().getPluginManager().disablePlugin(this);
				econEnabled = false;
				disabled = true;
				return;
			}
			this.price = getConfig().getDouble("economy.cost");
		}
		
		this.manager = new GameManager(this);
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
    
    public Double getPrice(){
    	return this.price;
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
}

