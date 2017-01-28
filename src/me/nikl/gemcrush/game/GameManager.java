package me.nikl.gemcrush.game;

import me.nikl.gemcrush.Main;
import me.nikl.gemcrush.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.*;
import java.util.logging.Level;

public class GameManager implements Listener{

	private Main plugin;
	private Set<Game> games;
	private Map<UUID, Integer> clicks;
	
	private Map<Integer, Double> prices;
	private Map<Integer, List<String>> commands;
	private Map<Integer, List<String>> broadcasts;
	private Map<Integer, List<String>> messages;
	private Map<Integer, List<ItemStack>> items;
	private Map<String, ItemStack> itemRewards;
	
	private boolean pay, sendMessages, sendBroadcasts, dispatchCommands, rewardBypass, giveItems;
	
	private int moneyTimeframe, itemRewardTimeframe;
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
		if(!plugin.getConfig().isConfigurationSection("onGameEnd.scoreIntervals")) return;
		ConfigurationSection onGameEnd = plugin.getConfig().getConfigurationSection("onGameEnd");
		// default is true
		rewardBypass = !onGameEnd.isBoolean("restrictions.playersWithBypassDontGetRewards") || onGameEnd.getBoolean("restrictions.playersWithBypassDontGetRewards");
		prices = new HashMap<>();
		commands = new HashMap<>();
		broadcasts = new HashMap<>();
		messages = new HashMap<>();
		itemRewards = new HashMap<>();
		items = new HashMap<>();
		pay = onGameEnd.getBoolean("pay");
		sendMessages = onGameEnd.getBoolean("sendMessages");
		sendBroadcasts = onGameEnd.getBoolean("sendBroadcasts");
		dispatchCommands = onGameEnd.getBoolean("dispatchCommands");
		giveItems = onGameEnd.getBoolean("giveItems");
		
		moneyTimeframe = onGameEnd.getInt("restrictions.timeIntervals.money", 0);
		itemRewardTimeframe = onGameEnd.getInt("restrictions.timeIntervals.items", 0);
		
		if(onGameEnd.isConfigurationSection("itemRewards")){
			ConfigurationSection itemRewards = onGameEnd.getConfigurationSection("itemRewards");
			for(String key: itemRewards.getKeys(false)){
				MaterialData mat = getMaterial(itemRewards.getString(key + ".material"));
				if(mat == null){
					Bukkit.getLogger().log(Level.WARNING, "Material of " + key + " from onGameEnd could not be loaded!");
					continue;
				}
				ItemStack item = mat.toItemStack();
				if(itemRewards.isInt(key + ".count")) item.setAmount(itemRewards.getInt(key + ".count"));
				if(itemRewards.isList(key + ".lore")){
					List<String> lore = new ArrayList<>(itemRewards.getStringList(key + ".lore"));
					for(int i = 0; i < lore.size(); i++){
						lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
					}
					ItemMeta meta = item.getItemMeta();
					meta.setLore(lore);
					item.setItemMeta(meta);
				}
				if(itemRewards.isString(key + ".displayName")){
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemRewards.getString(key + ".displayName")));
					item.setItemMeta(meta);
				}
				this.itemRewards.put(key, item);
			}
		}
		
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
			
			
			if (onGameEnd.isSet(key + ".items") && (onGameEnd.isList(key + ".items") )) {
				ArrayList<String> itemList = new ArrayList<>(onGameEnd.getStringList(key + ".items"));
				ArrayList<ItemStack> itemStackList = new ArrayList<>();
				boolean found = false;
				for(String itemString: itemList){
					if(itemRewards.get(itemString) != null){
						itemStackList.add(itemRewards.get(itemString));
						found = true;
						continue;
					}
					Bukkit.getLogger().log(Level.WARNING, "Item " + itemString + " from 'onGameEnd." + key + ".items' was not defined in 'onGameEnd.itemRewards'");
				}
				
				if(found){
					items.put(keyInt, itemStackList);
				} else {
					items.put(keyInt, null);
				}
			} else {
				items.put(keyInt, null);
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
	
	private MaterialData getMaterial(String matString){
		if(matString==null) return null;
		Material mat = null;
		byte data = 0;
		String[] obj = matString.split(":");
		
		if (obj.length == 2) {
			try {
				mat = Material.matchMaterial(obj[0]);
			} catch (Exception e) {
				// material name doesn't exist
			}
			
			try {
				data = Integer.valueOf(obj[1]).byteValue();
			} catch (NumberFormatException e) {
				// data not a number
			}
		} else {
			try {
				mat = Material.matchMaterial(matString);
			} catch (Exception e) {
				// material name doesn't exist
			}
		}
		if(mat == null) return null;
		@SuppressWarnings("deprecation") MaterialData toReturn = new MaterialData(mat, data);
		return toReturn;
	}
	
	@EventHandler
	public void onInvClick(InventoryClickEvent e){
		if(!isIngame(e.getWhoClicked().getUniqueId()) || e.getInventory() == null || e.getCurrentItem() == null || !(e.getWhoClicked() instanceof Player)){
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
							if(Main.playSounds)player.playSound(player.getLocation(), Sounds.NOTE_BASS.bukkitSound(), 10f, 1f);
						} else {
							if(Main.playSounds)player.playSound(player.getLocation(), Sounds.VILLAGER_HIT.bukkitSound(), 10f, 1f);
							//if(Main.playSounds)player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10f, 1f);
							
						}
					} else if(slot == oldSlot){
						break;
					} else {
						clicks.put(player.getUniqueId(), slot);
						game.shine(slot, true);
						game.shine(oldSlot, false);
						if(Main.playSounds)player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10f, 1f);
						if(Main.debug)player.sendMessage("overwritten click in " + oldSlot + " with click in " + slot);
					}
				} else {
					if(Main.debug)player.sendMessage("saved first click in slot " + slot);
					if(Main.playSounds)player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10f, 1f);
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
		onGameEnd(score, player, true, true, true, true, true);
	}
	
	void onGameEnd(int score, Player player, boolean payOut, boolean sendMessages, boolean dispatchCommands, boolean sendBroadcasts, boolean giveItems){
		plugin.setStatistics(player.getUniqueId(), score);
		int key = getKey(score);
		if(Main.debug) Bukkit.getConsoleSender().sendMessage("Key in onGameEnd: " + key);
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("pay: " + payOut + "  sendMe: " + sendMessages + "   sendB: " + sendBroadcasts + "    dispatch: " + dispatchCommands);
		
		
		giveItems:
		if(giveItems && this.giveItems &&(!player.hasPermission("gemcrush.bypass") || rewardBypass)){
			if(itemRewardTimeframe > 0){
				long timeStamp = plugin.getTimestamp(player.getUniqueId(), "itemReward");
				if(System.currentTimeMillis() - timeStamp < (itemRewardTimeframe*60000)){
					long diff = System.currentTimeMillis() - timeStamp;
					int min = (int)(diff/1000.)/60;
					int sec = (int)(diff/1000.)%60;
					if((this.items.get(key) != null && !this.items.get(key).isEmpty()))player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_REWARD_COOLDOWN_ITEMS.replaceAll("%min%", String.valueOf(min)).replaceAll("%sec%", String.valueOf(sec))));
					break giveItems;
				}
			}
			if(this.items.get(key) == null) break giveItems;
			for(ItemStack item : this.items.get(key)){
				player.getInventory().addItem(item);
			}
			plugin.setTimestamp(player.getUniqueId(), "itemReward");
		}
		
		if(sendMessages && this.sendMessages && messages.get(key) != null && messages.get(key).size() > 0){
			for(String message : messages.get(key)){
				player.sendMessage(chatColor(Main.prefix + " " + message.replaceAll("%player%", player.getName()).replaceAll("%score%", score + "")));
			}
		}
		
		
		payMoney:
		if(payOut && this.pay && plugin.getEconEnabled() && (!player.hasPermission("gemcrush.bypass") || rewardBypass)){
			if(moneyTimeframe > 0){
				long timeStamp = plugin.getTimestamp(player.getUniqueId(), "moneyReward");
				if(System.currentTimeMillis() - timeStamp < (moneyTimeframe*60000)){
					long diff = System.currentTimeMillis() - timeStamp;
					int min = (int)(diff/1000.)/60;
					int sec = (int)(diff/1000.)%60;
					if(prices.get(key) > 0)player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_REWARD_COOLDOWN_MONEY.replaceAll("%min%", String.valueOf(min)).replaceAll("%sec%", String.valueOf(sec))));
					player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
					break payMoney;
				}
			}
			double reward = prices.get(key);
			if(Main.debug) Bukkit.getConsoleSender().sendMessage("Reward is: " + reward);
			if(reward > 0){
				plugin.setTimestamp(player.getUniqueId(), "moneyReward");
				Main.econ.depositPlayer(player, reward);
				player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_WITH_PAY.replaceAll("%score%", score +"").replaceAll("%reward%", reward + "")));
			} else {
				player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
			}
		} else {
			player.sendMessage(chatColor(Main.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
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
