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
	
	private Map<Integer, Integer> prices;
	//private Language lang;
	
	public GameManager(Main plugin){
		this.plugin = plugin;
		this.games = new HashSet<>();
		this.clicks = new HashMap<>();
		if(plugin.getConfig().isConfigurationSection("economy.reward")) {
			prices = new HashMap<>();
			ConfigurationSection priceSec = plugin.getConfig().getConfigurationSection("economy.reward");
			for(String key : priceSec.getKeys(false)){
				int keyInt;
				try{
					keyInt = Integer.parseInt(key);
				} catch (NumberFormatException e){
					Bukkit.getConsoleSender().sendMessage(Main.prefix + " NumberFormatException while getting the rewards from config!");
					continue;
				}
				prices.put(keyInt, priceSec.getInt(key));
			}
			
			// Debug output XXX
			Bukkit.getConsoleSender().sendMessage("Testing price List: ");
			for(int i : prices.keySet()){
				
				Bukkit.getConsoleSender().sendMessage("Over: " + i + "    Price: " + prices.get(i));
			}
			
		} else {
			prices = null;
		}
		//this.lang = plugin.lang;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
		
		int slot = e.getSlot();
		
		// switch with gamemode
		switch(game.getState()){
			
			
			case PLAY:
				if(this.clicks.containsKey(player.getUniqueId())){
					int oldSlot = clicks.get(player.getUniqueId());
					if(slot == oldSlot + 1 || slot == oldSlot - 1 || slot == oldSlot + 9 || slot == oldSlot - 9){
						//player.sendMessage("Switching Gems " + slot + " and " + oldSlot);
						if(game.switchGems(slot < oldSlot ? slot : oldSlot, slot > oldSlot ? slot : oldSlot)){
							clicks.remove(player.getUniqueId());
						}
					} else if(slot == oldSlot){
						break;
					} else {
						clicks.put(player.getUniqueId(), slot);
						//player.sendMessage("overwritten click in " + oldSlot + " with click in " + slot);
					}
				} else {
					//player.sendMessage("saved first click in slot " + slot);
					this.clicks.put(player.getUniqueId(), slot);
				}
				//player.sendMessage("saved click: " + clicks.get(player.getUniqueId()));
				//player.sendMessage("Columns:");
				//player.sendMessage(game.scanColumns().toString());
				//player.sendMessage("Rows:");
				//player.sendMessage(game.scanRows().toString());
				break;
			
			case FILLING:
				break;
					
		
			default:
				break;
			
		}
	}
	

	private Game getGame(UUID uuid) {
		for(Iterator<Game> gameI = games.iterator(); gameI.hasNext();){
			Game game = gameI.next();
			if(isPlayer(uuid, game)){
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
		//e.getPlayer().sendMessage("Inventory was closed");//XXX
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
	
	public Main getPlugin(){
		return this.plugin;
	}
	
	public boolean isIngame(UUID uuid){
		for(Game game : games){
			if(isPlayer(uuid, game)){
				return true;
			}
		}
		return false;
	}
	
	public boolean isPlayer(UUID uuid, Game game){
		if(game.getUUID().equals(uuid)){
			return true;
		}
		return false;
	}

	public void removeGame(Game game) {
		clicks.remove(game.getUUID());
		game.shutDown();
		games.remove(game);
	}
	
	public int getReward(int score){
		if(prices == null || prices.size() == 0) return 0;
		int price = 0;
		for(int key : prices.keySet()) {
			while (score > key){
				price = prices.get(key);
			}
		}
		return price;
	}
	
	String chatColor(String message){
		return ChatColor.translateAlternateColorCodes('&', message);
	}
}
