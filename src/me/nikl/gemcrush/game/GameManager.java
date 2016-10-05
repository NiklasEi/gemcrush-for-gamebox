package me.nikl.gemcrush.game;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.nikl.gemcrush.Main;

public class GameManager implements Listener{

	private Main plugin;
	private Set<Game> games;
	private Map<UUID, Integer> clicks;
	//private Language lang;
	
	public GameManager(Main plugin){
		this.plugin = plugin;
		this.games = new HashSet<>();
		this.clicks = new HashMap<>();
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
						player.sendMessage("Switching Gems " + slot + " and " + oldSlot);
						game.switchGems(slot < oldSlot ? slot : oldSlot, slot > oldSlot ? oldSlot : slot);
					} else if(slot == oldSlot){
						break;
					} else {
						clicks.put(player.getUniqueId(), slot);
						player.sendMessage("overwritten click in " + oldSlot + " with click in " + slot);
					}
				} else {
					player.sendMessage("saved first click in slot " + slot);
					this.clicks.put(player.getUniqueId(), slot);
				}
				player.sendMessage("saved click: " + clicks.get(player.getUniqueId()));
				player.sendMessage("Columns:");
				player.sendMessage(game.scanColumns().toString());
				player.sendMessage("Rows:");
				player.sendMessage(game.scanRows().toString());
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
		games.remove(game);
	}
	
	String chatColor(String message){
		return ChatColor.translateAlternateColorCodes('&', message);
	}
}
