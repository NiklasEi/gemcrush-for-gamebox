package me.nikl.gemcrush.game;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

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
	//private Language lang;
	
	public GameManager(Main plugin){
		this.plugin = plugin;
		this.games = new HashSet<Game>();
		//this.lang = plugin.lang;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onInvClick(InventoryClickEvent e){
		if(!isIngame(e.getWhoClicked().getUniqueId()) || e.getClickedInventory() == null || e.getCurrentItem() == null || !(e.getWhoClicked() instanceof Player)){
			return;
		}
		
		// cancel event and return if it's not a right/left click
		e.setCancelled(true);
		if(!e.getAction().equals(InventoryAction.PICKUP_ALL) && !e.getAction().equals(InventoryAction.PICKUP_HALF)){
			return;
		}
		
		// get Player and Game objects
		Player player = (Player) e.getWhoClicked();
		Game game = getGame(player.getUniqueId());
		
		// check hashcode of the inventory against hascode of the games inventory
		if(!game.isInventory(e.getClickedInventory().hashCode())){
			Bukkit.getConsoleSender().sendMessage("not current inv."); // XXX
			return;
		}
		int slot = e.getSlot();
		
		// switch with gamemode
		switch(game.getState()){
		
		case FILLING:
			player.sendMessage("filling");
			break;
			
		case PLAY:
			game.remove(slot);
			game.fillUp();
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
		games.remove(game);		
	}
	
	String chatColor(String message){
		return ChatColor.translateAlternateColorCodes('&', message);
	}
}
