package me.nikl.gemcrush;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.nikl.gemcrush.cmds.MainCommand;
import me.nikl.gemcrush.game.GameManager;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin{

	private GameManager manager;
	private FileConfiguration config, stats;
	private File con, sta;
	public static Economy econ = null;
	public static String prefix = "[&3Battleship&r]";
	public Boolean econEnabled;
	public Double reward, price;
	public Language lang;
	public boolean disabled;
	
	@Override
	public void onEnable(){
        
		this.disabled = false;
		this.con = new File(this.getDataFolder().toString() + File.separatorChar + "config.yml");
		this.sta = new File(this.getDataFolder().toString() + File.separatorChar + "stats.yml");

		reload();
		if(disabled) return;

		this.manager = new GameManager(this);
        this.getCommand("gemcrush").setExecutor(new MainCommand(this));
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
	
    private boolean setupEconomy(){
    	if (getServer().getPluginManager().getPlugin("Vault") == null) {
    		return false;
    	}
    	RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    	if (rsp == null) {
    		return false;
    	}
    	econ = (Economy)rsp.getProvider();
    	return econ != null;
    }
	
	public void reloadConfig(){
		try { 
			this.config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(this.con), "UTF-8")); 
		} catch (UnsupportedEncodingException e) { 
			e.printStackTrace(); 
		} catch (FileNotFoundException e) { 
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
		
		// load statsfile
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
				return;
			}
			this.price = getConfig().getDouble("economy.cost");
			this.reward = getConfig().getDouble("economy.reward");
			if(price == null || reward == null || price < 0. || reward < 0.){
				Bukkit.getConsoleSender().sendMessage(chatColor(prefix + " &4Wrong configuration in section economy!"));
				getServer().getPluginManager().disablePlugin(this);
			}
		}
	}

	public void addWinToStatistics(UUID player) {
		if(this.stats == null) return;
		if(!stats.isInt(player.toString() + "." + "won")){
			stats.set(player.toString() + "." + "won", 1);
			return;
		}
		this.stats.set(player.toString() + "." + "won", (this.stats.getInt(player.toString() + "." + "won")+1));
	}
	
	public FileConfiguration getStatistics(){
		return this.stats;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public void setConfig(FileConfiguration config) {
		this.config = config;
	}
	
    public String chatColor(String message){
    	return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public Boolean getEconEnabled(){
    	return this.econEnabled;
    }
    
    public Double getReward(){
    	return this.reward;
    }
    
    public Double getPrice(){
    	return this.price;
    }

}

