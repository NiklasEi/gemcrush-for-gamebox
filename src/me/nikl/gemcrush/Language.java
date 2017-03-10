package me.nikl.gemcrush;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class Language {
	public static String prefix = "[&3GemCrush&r]";
	public static String name = "&1GemCrush&r";
	private Main plugin;
	private FileConfiguration langFile;
	
	public String CMD_NO_PERM, CMD_ONLY_PLAYER, CMD_RELOADED, CMD_RESET;
	public String CMD_NO_TOP_LIST, CMD_TOP_HEAD, CMD_TOP_TAIL, CMD_TOP_STRUCTURE;
	public String GAME_PAYED, GAME_NOT_ENOUGH_MONEY, GAME_FINISHED_NO_PAY, GAME_FINISHED_WITH_PAY;
	public String GAME_REWARD_COOLDOWN_MONEY, GAME_REWARD_COOLDOWN_ITEMS;
	public String TITLE_GAME;
	public List<String> CMD_HELP, GAME_HELP;
	private YamlConfiguration defaultLang;
	
	Language(Main plugin){
		this.plugin = plugin;
		if(!getLangFile()){
			Bukkit.getPluginManager().disablePlugin(plugin);
			plugin.disabled = true;
			return;
		}
		prefix = getString("prefix");
		name = getString("name");
		getCommandMessages();
		getGameMessages();
		getInvTitles();
	}
	
	private void getInvTitles() {
		this.TITLE_GAME = getString("inventoryTitles.title");

		
	}

	private void getGameMessages() {
		
		this.GAME_REWARD_COOLDOWN_MONEY = getString("game.rewardCooldownMoney");
		this.GAME_REWARD_COOLDOWN_ITEMS = getString("game.rewardCooldownItems");
		this.GAME_PAYED = getString("game.econ.payed");	
		this.GAME_NOT_ENOUGH_MONEY = getString("game.econ.notEnoughMoney");
		this.GAME_FINISHED_NO_PAY = getString("game.finishedWithoutPayout");
		this.GAME_FINISHED_WITH_PAY = getString("game.finishedWithPayout");
		
		this.GAME_HELP = getStringList("gameHelp");

	}

	private void getCommandMessages() {
		
		this.CMD_NO_PERM = getString("commandMessages.noPermission");
		this.CMD_ONLY_PLAYER = getString("commandMessages.onlyAsPlayer");
		this.CMD_RELOADED = getString("commandMessages.pluginReloaded");
		this.CMD_RESET = getString("commandMessages.statsReset");
		
		
		this.CMD_NO_TOP_LIST = getString("commandMessages.noTopList");
		this.CMD_TOP_HEAD = getString("commandMessages.topListHead");
		this.CMD_TOP_TAIL = getString("commandMessages.topListTail");
		this.CMD_TOP_STRUCTURE = getString("commandMessages.topListStructure");
		

		this.CMD_HELP = getStringList("commandMessages.help");		
	}


	private List<String> getStringList(String path) {
		List<String> toReturn;
		if(!langFile.isList(path)){
			toReturn = defaultLang.getStringList(path);
			for(int i = 0; i<toReturn.size(); i++){
				toReturn.set(i, ChatColor.translateAlternateColorCodes('&',toReturn.get(i)));
			}
			return toReturn;
		}
		toReturn = langFile.getStringList(path);
		for(int i = 0; i<toReturn.size(); i++){
			toReturn.set(i, ChatColor.translateAlternateColorCodes('&',toReturn.get(i)));
		}
		return toReturn;
	}

	private String getString(String path) {
		if(!langFile.isString(path)){
			return ChatColor.translateAlternateColorCodes('&',defaultLang.getString(path));
		}
		return ChatColor.translateAlternateColorCodes('&',langFile.getString(path));
	}

	private boolean getLangFile() {
		try {
			String fileName = "language/lang_en.yml";
			this.defaultLang =  YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(fileName), "UTF-8"));
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
		}
		File defaultEn = new File(plugin.getDataFolder().toString() + File.separatorChar + "language" + File.separatorChar + "lang_en.yml");
		if(!defaultEn.exists()){
			plugin.saveResource("language" + File.separatorChar + "lang_en.yml", false);
		}
		File defaultDe = new File(plugin.getDataFolder().toString() + File.separatorChar + "language" + File.separatorChar + "lang_de.yml");
		if(!defaultDe.exists()){
			plugin.saveResource("language" + File.separatorChar + "lang_de.yml", false);
		}
		
		if(!plugin.getConfig().isSet("langFile")){
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Language file is missing in the config!"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " Add the following to your config:"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " langFile: 'lang_en.yml'"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Using default language file"));
			this.langFile = defaultLang;
		} else {
			if(!plugin.getConfig().isString("langFile")){
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Language file is invalid (no String)!"));
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Using default language file"));
				this.langFile = defaultLang;
			} else {
				String fileName = plugin.getConfig().getString("langFile");
				if(fileName.equalsIgnoreCase("default") || fileName.equalsIgnoreCase("default.yml")){
					this.langFile = defaultLang;
					return true;
				}
				File languageFile = new File(plugin.getDataFolder().toString() + File.separatorChar + "language" + File.separatorChar + fileName);
				if(!languageFile.exists()){
					languageFile.mkdir();
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Language file not found!"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Using default language file"));
					this.langFile = defaultLang;
				} else {
					try { 
						this.langFile = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(languageFile), "UTF-8")); 
					} catch (UnsupportedEncodingException | FileNotFoundException e) { 
						e.printStackTrace(); 
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Error while loading language file!"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Using default language file"));
						this.langFile = defaultLang;
					}
				}
			}
		}
		int count = 0;
		for(String key : defaultLang.getKeys(true)){
			if(defaultLang.isString(key)){
				if(!this.langFile.isString(key)){// there is a message missing
					if(count == 0){
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Missing message(s) in your language file!"));
					}
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " " + key));
					count++;
				}
			}
		}
		if(count > 0){
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + ""));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Game will use default messages for these paths"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + ""));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Please get an updated language file"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4Or add the listed paths by hand"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &4*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"));
		}
		return true;
		
	}
	
}

