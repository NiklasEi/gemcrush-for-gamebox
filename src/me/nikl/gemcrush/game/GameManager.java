package me.nikl.gemcrush.game;

import me.nikl.gemcrush.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class GameManager implements Listener{

	private Main plugin;
	private Set<Game> games;
	private Map<UUID, Integer> clicks;
	
	private Map<Integer, Double> prices;
	private Map<Integer, List<String>> commands;
	private Map<Integer, List<String>> broadcasts;
	private Map<Integer, List<String>> messages;
	
	private boolean pay, sendMessages, sendBroadcasts, dispatchCommands;
	//private Language lang;
	
	public GameManager(Main plugin){
		this.plugin = plugin;
		this.games = new HashSet<>();
		this.clicks = new HashMap<>();
		getOnGameEnd();
		//this.lang = plugin.lang;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	private void getOnGameEnd() {
		if(plugin.getConfig().isConfigurationSection("onGameEnd.scoreIntervals")) {
			ConfigurationSection onGameEnd = plugin.getConfig().getConfigurationSection("onGameEnd");
			prices = new HashMap<>();
			commands = new HashMap<>();
			broadcasts = new HashMap<>();
			messages = new HashMap<>();
			pay = onGameEnd.getBoolean("pay");
			sendMessages = onGameEnd.getBoolean("sendMessages");
			sendBroadcasts = onGameEnd.getBoolean("sendBroadcasts");
			dispatchCommands = onGameEnd.getBoolean("dispatchCommands");
			onGameEnd = plugin.getConfig().getConfigurationSection("onGameEnd.scoreIntervals");
			for (String key : onGameEnd.getKeys(false)) {
				int keyInt;
				try {
					keyInt = Integer.parseInt(key);
				} catch (NumberFormatException e) {
					Bukkit.getLogger().warning("[GemCrush] NumberFormatException while getting the rewards from config!");
					continue;
				}
				if (onGameEnd.isSet(key + ".money") && (onGameEnd.isDouble(key + ".money") || onGameEnd.isInt(key + ".money"))) {
					prices.put(keyInt, onGameEnd.getDouble(key + ".money"));
				} else {
					prices.put(keyInt, 0.);
				}
				
				if (onGameEnd.isSet(key + ".broadcast") && onGameEnd.isList(key + ".broadcast")) {
					broadcasts.put(keyInt, onGameEnd.getStringList(key + ".broadcast"));
				} else {
					broadcasts.put(keyInt, null);
				}
				
				if (onGameEnd.isSet(key + ".messages") && onGameEnd.isList(key + ".messages")) {
					messages.put(keyInt, onGameEnd.getStringList(key + ".messages"));
				} else {
					messages.put(keyInt, null);
				}
				
				if (onGameEnd.isSet(key + ".commands") && onGameEnd.isList(key + ".commands")) {
					commands.put(keyInt, onGameEnd.getStringList(key + ".commands"));
				} else {
					commands.put(keyInt, null);
				}
			}
			
			if(Main.debug){
				Bukkit.getConsoleSender().sendMessage("Testing onGameEnd: ");
				
				Bukkit.getConsoleSender().sendMessage("pay: " + pay + "  sendMe: " + sendMessages + "   sendB: " + sendBroadcasts + "    dispatch: " + dispatchCommands);
				for (int i : prices.keySet()) {
					
					Bukkit.getConsoleSender().sendMessage("Over: " + i + "    reward: " + prices.get(i));
					Bukkit.getConsoleSender().sendMessage("    broadcasts: " + broadcasts.get(i));
					Bukkit.getConsoleSender().sendMessage("    messages: " + messages.get(i));
					Bukkit.getConsoleSender().sendMessage("    commands: " + commands.get(i));
				}
				
				Bukkit.getConsoleSender().sendMessage(" ");
				Bukkit.getConsoleSender().sendMessage("Run some tests: ");
				Random rand = new Random();
				for (int i = 0; i < 10; i++) {
					int score = rand.nextInt(600);
					Bukkit.getConsoleSender().sendMessage("Random score: " + score);
					Bukkit.getConsoleSender().sendMessage("Key: " + getKey(score));
				}
				Bukkit.getConsoleSender().sendMessage("Random score: " + 100);
				Bukkit.getConsoleSender().sendMessage("Key: " + getKey(100));
			}
			
		}
	}
	
	
	@EventHandler
	public void onInvClick(InventoryClickEvent e){
		if(!isIngame(e.getWhoClicked().getUniqueId()) || e.getClickedInventory() == null || e.getCurrentItem() == null || !(e.getWhoClicked() instanceof Player)){
			return;
		}
		
		// player is inGame, clicked inside an inventory and the clicked item is not null
		
		// cancel event and return if it's not a right/left click
		e.setCancelled(true);
		if(!e.getAction().equals(InventoryAction.PICKUP_ALL) && !e.getAction().equals(InventoryAction.PICKUP_HALF)) {
			return;
		}
		
		// check whether the clicked inventory is the top inventory
		if(e.getRawSlot() != e.getSlot()){
			return;
		}
		
		// get Player and Game objects
		Player player = (Player) e.getWhoClicked();
		Game game = getGame(player.getUniqueId());
		if(game == null){
			clicks.remove(player.getUniqueId());
			player.closeInventory();
			games.remove(game);
			return;
		}
		int slot = e.getSlot();
		
		// switch with getState
		if(game.getState() == null) return;
		switch(game.getState()){
			
			
			case PLAY:
				if(this.clicks.containsKey(player.getUniqueId())){
					int oldSlot = clicks.get(player.getUniqueId());
					if(slot == oldSlot + 1 || slot == oldSlot - 1 || slot == oldSlot + 9 || slot == oldSlot - 9){
						if(Main.debug)player.sendMessage("Switching Gems " + slot + " and " + oldSlot);
						if(game.switchGems(slot < oldSlot ? slot : oldSlot, slot > oldSlot ? slot : oldSlot)){
							clicks.remove(player.getUniqueId());
						}
					} else if(slot == oldSlot){
						break;
					} else {
						clicks.put(player.getUniqueId(), slot);
						game.shine(slot, true);
						game.shine(oldSlot, false);
						if(Main.debug)player.sendMessage("overwritten click in " + oldSlot + " with click in " + slot);
					}
				} else {
					if(Main.debug)player.sendMessage("saved first click in slot " + slot);
					this.clicks.put(player.getUniqueId(), slot);
					game.shine(slot, true);
				}
				if(Main.debug)player.sendMessage("saved click: " + clicks.get(player.getUniqueId()));
				if(Main.debug)player.sendMessage("Columns:");
				if(Main.debug)player.sendMessage(game.scanColumns().toString());
				if(Main.debug)player.sendMessage("Rows:");
				if(Main.debug)player.sendMessage(game.scanRows().toString());
				break;
			
			case FILLING:
				break;
					
		
			default:
				break;
			
		}
	}
	

	private Game getGame(UUID uuid) {
		for (Game game : games) {
			if (isPlayer(uuid, game)) {
				return game;
			}
		}
		return null;
	}

	@EventHandler
	public void onInvClose(InventoryCloseEvent e){
		if(!isIngame(e.getPlayer().getUniqueId())){
			return;
		}
		if(Main.debug)e.getPlayer().sendMessage("Inventory was closed");//XXX
		getGame(e.getPlayer().getUniqueId()).shutDown();
		removeGame(getGame(e.getPlayer().getUniqueId()));
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e){
		if(!isIngame(e.getPlayer().getUniqueId())){
			return;
		}
		removeGame(getGame(e.getPlayer().getUniqueId()));
	}
	
	
	public void startGame(UUID playerUUID){
		games.add(new Game(plugin, playerUUID));
	}
	
	private boolean isIngame(UUID uuid){
		for(Game game : games){
			if(isPlayer(uuid, game)){
				return true;
			}
		}
		return false;
	}
	
	private boolean isPlayer(UUID uuid, Game game){
		return game.getUUID().equals(uuid);
	}

	void removeGame(Game game) {
		clicks.remove(game.getUUID());
		game.shutDown();
		games.remove(game);
	}
	
	private int getKey(int score){
		int distance = -1;
		for(int key : prices.keySet()) {
			if((score - key) >= 0 && (distance < 0 || distance > (score - key))){
				distance = score - key;
			}
		}
		if(distance > -1)
			return score - distance;
		return -1;
	}
	
	void onGameEnd(int score, Player player){
		onGameEnd(score, player, true, true, true, true);
	}
	
	private void onGameEnd(int score, Player player, boolean payOut, boolean sendMessages, boolean dispatchCommands, boolean sendBroadcasts){
		plugin.setStatistics(player.getUniqueId(), score);
		int key = getKey(score);
		if(Main.debug) Bukkit.getConsoleSender().sendMessage("Key in onGameEnd: " + key);
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("pay: " + payOut + "  sendMe: " + sendMessages + "   sendB: " + sendBroadcasts + "    dispatch: " + dispatchCommands);
		
		if(payOut && this.pay){
			double reward = prices.get(key);
			if(Main.debug) Bukkit.getConsoleSender().sendMessage("Reward is: " + reward);
			if(plugin.getEconEnabled() && reward > 0){
				Main.econ.depositPlayer(player, reward);
				player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_WITH_PAY.replaceAll("%score%", score +"").replaceAll("%reward%", reward + "")));
			} else {
				player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
			}
		} else {
			player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
		}
		
		if(sendMessages && this.sendMessages && messages.get(key) != null && messages.get(key).size() > 0){
			for(String message : messages.get(key)){
				player.sendMessage(chatColor(Main.prefix + " " + message.replaceAll("%player%", player.getName()).replaceAll("%score%", score + "")));
			}
		}
		
		if(dispatchCommands && this.dispatchCommands && commands.get(key) != null && commands.get(key).size() > 0){
			for(String cmd : commands.get(key)){
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%player%", player.getName()).replaceAll("%score%", score + ""));
			}
		}
		
		if(sendBroadcasts && this.sendBroadcasts && broadcasts.get(key) != null && broadcasts.get(key).size() > 0){
			for(String broadcast: broadcasts.get(key)){
				Bukkit.broadcastMessage(chatColor(Main.prefix + " " + broadcast.replaceAll("%player%", player.getName()).replaceAll("%score%", score + "")));
			}
		}
		
	}
	
	private String chatColor(String message){
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	
	
	public void shutDown(){
		for (Game game : games){
			removeGame(game);
		}
	}
}
