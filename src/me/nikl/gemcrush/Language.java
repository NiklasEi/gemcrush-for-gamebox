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
	private Main plugin;
	private FileConfiguration langFile;
	
	public String CMD_NO_PERM, CMD_ONLY_PLAYER, CMD_PLAYER_OFFLINE, CMD_PLAYER_INGAME, CMD_PLAYER_HAS_INVITE, CMD_NOT_YOURSELF, CMD_FIRST_OFFLINE, CMD_RELOADED, CMD_ONLY_ONE_ONLINE;
	public String GAME_PAYED, GAME_NOT_ENOUGH_MONEY, GAME_WON_MONEY, GAME_WON_MONEY_GAVE_UP, GAME_WON, GAME_INVITE_ACCEPT, GAME_LOOSE, GAME_GAVE_UP, GAME_OTHER_GAVE_UP,
		GAME_TOO_SLOW, GAME_WON_MONEY_TOO_SLOW, GAME_WON_TOO_SLOW, GAME_INVITE_EXPIRED, GAME_INVITE_RETURNED_MONEY;
	public String TITLE_GUI, TITLE_SET_SHIP_1, TITLE_SET_SHIP_2, TITLE_SET_SHIP_3, TITLE_SET_SHIP_4, TITLE_ATTACKER, TITLE_DEFENDER, TITLE_WON, TITLE_LOST;
	public List<String> CMD_HELP, GAME_INVITE_FIRST, GAME_INVITE_SECOND;
	private YamlConfiguration defaultLang;
	public String TITLE_CHANGING;
	
	public Language(Main plugin){
		this.plugin = plugin;
		if(!getLangFile()){
			Bukkit.getPluginManager().disablePlugin(plugin);
			plugin.disabled = true;
			return;
		}
		getCommandMessages();
		getGameMessages();
		getInvTitles();
	}
	
	private void getInvTitles() {
		this.TITLE_GUI = getString("inventoryTitles.guiTitle");
		this.TITLE_SET_SHIP_1 = getString("inventoryTitles.setShip1");
		this.TITLE_SET_SHIP_2 = getString("inventoryTitles.setShip2");
		this.TITLE_SET_SHIP_3 = getString("inventoryTitles.setShip3");
		this.TITLE_SET_SHIP_4 = getString("inventoryTitles.setShip4");
		this.TITLE_ATTACKER = getString("inventoryTitles.attacker");
		this.TITLE_DEFENDER = getString("inventoryTitles.defender");
		this.TITLE_WON = getString("inventoryTitles.won");		
		this.TITLE_LOST = getString("inventoryTitles.lost");

		this.TITLE_CHANGING = getString("inventoryTitles.changingGrids");
		
	}

	private void getGameMessages() {
		this.GAME_INVITE_FIRST = getStringList("game.invite.messageToFirstPlayer");
		this.GAME_INVITE_SECOND = getStringList("game.invite.messageToSecondPlayer");	
		
		this.GAME_PAYED = getString("game.econ.payed");	
		this.GAME_NOT_ENOUGH_MONEY = getString("game.econ.notEnoughMoney");	
		this.GAME_WON_MONEY = getString("game.econ.wonMoney");	
		this.GAME_WON_MONEY_GAVE_UP = getString("game.econ.wonMoneyGaveUp");
		this.GAME_WON_MONEY_TOO_SLOW = getString("game.econ.wonMoneyTooSlow");		
		this.GAME_WON = getString("game.won");		
		this.GAME_INVITE_ACCEPT = getString("game.invite.inviteAccept");	
		this.GAME_LOOSE = getString("game.lost");	
		this.GAME_GAVE_UP = getString("game.gaveUp");	
		this.GAME_OTHER_GAVE_UP = getString("game.otherGaveUp");	
		this.GAME_TOO_SLOW = getString("game.tooSlow");	
		this.GAME_WON_TOO_SLOW = getString("game.otherTooSlow");	
		this.GAME_INVITE_EXPIRED = getString("game.invite.expired");
		this.GAME_INVITE_RETURNED_MONEY = getString("game.invite.returnedMoney");
		
	}

	private void getCommandMessages() {
		
		this.CMD_NO_PERM = getString("commandMessages.noPermission");
		this.CMD_ONLY_PLAYER = getString("commandMessages.onlyAsPlayer");
		this.CMD_PLAYER_OFFLINE = getString("commandMessages.playerIsOffline");
		this.CMD_PLAYER_INGAME = getString("commandMessages.playerAlreadyIngame");
		this.CMD_PLAYER_HAS_INVITE = getString("commandMessages.playerHasInviteAlready");
		this.CMD_NOT_YOURSELF = getString("commandMessages.cannotInviteYourself");
		this.CMD_FIRST_OFFLINE = getString("commandMessages.firstPlayerIsOffline");
		this.CMD_RELOADED = getString("commandMessages.pluginReloaded");
		

		this.CMD_ONLY_ONE_ONLINE = getString("commandMessages.aloneOnServer");
		

		this.CMD_HELP = getStringList("commandMessages.help");		
	}

	private List<String> getStringList(String path) {
		if(!langFile.isList(path)){
			return defaultLang.getStringList(path);
		}
		return langFile.getStringList(path);
	}

	private String getString(String path) {
		if(!langFile.isString(path)){
			return defaultLang.getString(path);
		}
		return langFile.getString(path);
	}

	private boolean getLangFile() {
		InputStream inputStream = null;
		OutputStream outputStream = null;

		File defaultFile = null;
		try {
		
			// read this file into InputStream
			String fileName = "language/lang_en.yml";
			inputStream = plugin.getResource(fileName);

			// write the inputStream to a FileOutputStream
			defaultFile = new File(plugin.getDataFolder().toString() + File.separatorChar + "language" + File.separatorChar + "default.yml");
			defaultFile.getParentFile().mkdirs();
			outputStream = new FileOutputStream(defaultFile);

			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
		try {
			this.defaultLang =  YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(defaultFile), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e2) {
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
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Language file is missing in the config!"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " Add the following to your config:"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " langFile: 'lang_en.yml'"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Using default language file"));
			this.langFile = defaultLang;
		} else {
			if(!plugin.getConfig().isString("langFile")){
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Language file is invalid (no String)!"));
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Using default language file"));
				this.langFile = defaultLang;
			} else {
				File languageFile = new File(plugin.getDataFolder().toString() + File.separatorChar + "language" + File.separatorChar + plugin.getConfig().getString("langFile"));
				if(!languageFile.exists()){
					languageFile.mkdir();
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Language file not found!"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Using default language file"));
					this.langFile = defaultLang;
				} else {
					try { 
						this.langFile = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(languageFile), "UTF-8")); 
					} catch (UnsupportedEncodingException | FileNotFoundException e) { 
						e.printStackTrace(); 
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Error while loading language file!"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4*******************************************************"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Using default language file"));
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
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"));
						Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Missing message(s) in your language file!"));
					}
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " " + key));
					count++;
				}
			}
		}
		if(count > 0){
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + ""));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Game will use default messages for these paths"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + ""));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Please get an updated language file"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4Or add the listed paths by hand"));
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + " &4*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"));
		}
		return true;
		
	}
	
}

