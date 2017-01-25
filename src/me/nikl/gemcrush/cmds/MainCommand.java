package me.nikl.gemcrush.cmds;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;
import me.nikl.gemcrush.game.GameManager;


public class MainCommand implements CommandExecutor {
	
	private Main plugin;
	private GameManager manager;
	private Language lang;
	
	public MainCommand(Main plugin){
		this.plugin = plugin;
		this.manager = plugin.getManager();
		this.lang = plugin.lang;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 0){
			if(!(sender instanceof Player)){
				sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_ONLY_PLAYER));
				return true;
			}
			Player player = (Player) sender;
			if(!player.hasPermission("gemcrush.play")){
				sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_NO_PERM));
				return true;				
			}
			if(plugin.getEconEnabled() && !player.hasPermission("gemcrush.bypass")){
				if(Main.econ.getBalance(player) >= plugin.getPrice()){
					Main.econ.withdrawPlayer(player, plugin.getPrice());
					sender.sendMessage(plugin.chatColor(Main.prefix + lang.GAME_PAYED.replaceAll("%cost%", plugin.getPrice()+"")));
					manager.startGame(player.getUniqueId());
					return true;					
				} else {
					player.sendMessage(plugin.chatColor(Main.prefix + lang.GAME_NOT_ENOUGH_MONEY));
					return true;
				}
			} else {
				manager.startGame(player.getUniqueId());
				return true;
			}
		} else if(args.length == 1 && args[0].equalsIgnoreCase("reload")){
			if(sender.hasPermission("gemcrush.reload")){
				plugin.reload();
				sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_RELOADED));
				return true;
			} else {
				sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_NO_PERM));
				return true;
			}
		} else if(args.length == 1 && args[0].equalsIgnoreCase("reset")){
			if(sender.hasPermission("gemcrush.reset")){
				plugin.resetStatistics();
				sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_RESET));
				return true;
			} else {
				sender.sendMessage(plugin.chatColor(Main.prefix + lang.CMD_NO_PERM));
				return true;
			}
		}
		for(String message :  lang.CMD_HELP)
			sender.sendMessage(plugin.chatColor(Main.prefix + message));
		return true;
	}

}