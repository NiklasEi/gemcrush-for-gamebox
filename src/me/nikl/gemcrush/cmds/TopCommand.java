package me.nikl.gemcrush.cmds;

import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Niklas on 10/14/16.
 *
 * Command /gemcrushtop will display a top 10 list
 */
public class TopCommand implements CommandExecutor {
	
	private Main plugin;
	private FileConfiguration stats;
	private Language lang;
	private String structure;
	
	public TopCommand(Main plugin){
		this.plugin = plugin;
		this.lang = plugin.lang;
		this.structure = lang.CMD_TOP_STRUCTURE;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("gemcrush.top")){
			sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_NO_PERM));
			return true;
		}
		Map<UUID, Integer> scores = new HashMap<>();
		this.stats = plugin.getStatistics();
		stats.getKeys(false).stream().filter(uuid -> stats.isInt(uuid + ".stat")).forEach(uuid -> scores.put(UUID.fromString(uuid), stats.getInt(uuid + ".stat")));
		
		if(scores.size() == 0){
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.prefix + lang.CMD_NO_TOP_LIST));
			return true;
		}
		int length = (scores.size() > 10? 10 : scores.size());
		String[] messages = new String[length];
		UUID bestRecord = null;
		int record;
		for(int i = 0; i<length;i++){
			record = 0;
			for(UUID current : scores.keySet()){
				if(record == 0){
					record = scores.get(current);
					bestRecord = current;
					continue;
				}
				if(scores.get(current) > record){
					record = scores.get(current);
					bestRecord = current;
				}
			}
			// remove the entry that will be put into messages[] now
			scores.remove(bestRecord);
			
			//Get the name of the current top player
			String name;
			if(bestRecord == null){
				name = "PlayerNotFound";
			} else {
				name = (Bukkit.getOfflinePlayer(bestRecord) == null ? "PlayerNotFound" : (Bukkit.getOfflinePlayer(bestRecord).getName() == null ? "PlayerNotFound" : Bukkit.getOfflinePlayer(bestRecord).getName()));
			}
			
			// put the current top player in the String array
			messages[i] = structure.replaceAll("%rank%", (i+1)+"").replaceAll("%name%", name).replaceAll("%score%", Integer.toString(record));
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.CMD_TOP_HEAD));
		for(String message : messages){
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.CMD_TOP_TAIL));
		return true;
	}
}
