package me.nikl.gemcrush.game;

import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.data.SaveType;
import me.nikl.gamebox.data.Statistics;
import me.nikl.gamebox.game.IGameManager;
import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;
import me.nikl.gemcrush.Sounds;
import me.nikl.gemcrush.gems.Gem;
import me.nikl.gemcrush.gems.NormalGem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.*;
import java.util.logging.Level;

public class GameManager implements IGameManager{

	private Main plugin;
	private Set<Game> games;
	private Map<UUID, Integer> clicks;
	
	private Map<Integer, Double> prices;
	private Map<Integer, List<String>> commands;
	private Map<Integer, List<String>> broadcasts;
	private Map<Integer, List<String>> messages;
	private Map<Integer, List<ItemStack>> items;
	private Map<String, ItemStack> itemRewards;


	private Map<String,GameRules> gameTypes;


	private Statistics statistics;

	// map with all gems
	private Map<String, Gem> gems = new HashMap<>();

	private float volume;
	
	private boolean pay, sendMessages, sendBroadcasts, dispatchCommands, rewardBypass, giveItems;
	
	public GameManager(Main plugin){
		this.plugin = plugin;
		this.statistics = plugin.gameBox.getStatistics();
		this.games = new HashSet<>();
		this.clicks = new HashMap<>();
		this.volume = (float) plugin.getConfig().getDouble("game.soundVolume", 0.5);


		getOnGameEnd();



		if(!loadGems()){
			Bukkit.getLogger().log(Level.SEVERE, " problem while loading the gems from the configuration file");
		}

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



	private boolean loadGems() {
		boolean worked = true;

		Material mat = null;
		int data = 0;
		int index = 0;

		if (!plugin.getConfig().isConfigurationSection("normalGems")) {
			Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Outdated configuration file! Game cannot be started.");
			return false;
		}

		ConfigurationSection section = plugin.getConfig().getConfigurationSection("normalGems");

		for (String key : section.getKeys(false)) {
			if (Main.debug) Bukkit.getConsoleSender().sendMessage("getting " + key);
			if (!section.isSet(key + ".material")) {
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + key);
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Skipping the gem. Is the material set?");
				continue;
			}
			if (!section.isSet(key + ".displayName") || !section.isString(key + ".displayName")) {
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + key);
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Skipping the gem. Is the displayName set?");
				continue;
			}

			String value = section.getString(key + ".material");
			String[] obj = value.split(":");
			String name = chatColor(section.getString(key + ".displayName"));

			if (obj.length == 2) {
				try {
					mat = Material.matchMaterial(obj[0]);
				} catch (Exception e) {
					worked = false; // material name doesn't exist
				}

				try {
					data = Integer.valueOf(obj[1]);
				} catch (NumberFormatException e) {
					worked = false; // data not a number
				}
			} else {
				try {
					mat = Material.matchMaterial(value);
				} catch (Exception e) {
					worked = false; // material name doesn't exist
				}
			}
			if (mat == null) {
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Problem in: " + "normalGems." + key);
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "The material is not valid! Maybe your minecraft version is too old for it?");
				Bukkit.getLogger().log(Level.SEVERE, "[GemCrush] " + "Change the material or delete the gem. Skipping...");
				continue;
			}

			if (Main.debug) {
				Bukkit.getConsoleSender().sendMessage("saving gem " + name + " as " + index);
			}

			if (obj.length == 1) {
				this.gems.put(Integer.toString(index), new NormalGem(mat, name));
			} else {
				this.gems.put(Integer.toString(index), new NormalGem(mat, name, (short) data));
			}
			if (section.isSet(key + ".pointsOnBreak") && section.isInt(key + ".pointsOnBreak")) {
				this.gems.get(Integer.toString(index)).setPointsOnBreak(section.getInt(key + ".pointsOnBreak"));
			}
			if (section.isSet(key + ".probability") && (section.isDouble(key + ".probability") || section.isInt(key + ".probability"))) {
				if (Main.debug)
					Bukkit.getConsoleSender().sendMessage("set probability of " + name + " to " + section.getDouble(key + ".probability"));
				((NormalGem) this.gems.get(Integer.toString(index))).setPossibility(section.getDouble(key + ".probability"));
			}
			index++;
		}
		return worked;
	}



	private Game getGame(UUID uuid) {
		for (Game game : games) {
			if (isPlayer(uuid, game)) {
				return game;
			}
		}
		return null;
	}

	/*
	// not needed since the inventory gets closed before a player leaves => there will be an InventoryCloseEvent
	@EventHandler
	public void onLeave(PlayerQuitEvent e){
		if(!isIngame(e.getPlayer().getUniqueId())){
			return;
		}
		removeGame(getGame(e.getPlayer().getUniqueId()));
	}*/
	
	private boolean isPlayer(UUID uuid, Game game){
		return game.getUUID().equals(uuid);
	}

	void removeGame(UUID uuid){
		Game game = getGame(uuid);
		if(game != null){
			removeGame(getGame(uuid));
		} else if(Main.debug){
			Bukkit.getConsoleSender().sendMessage(" game was already closed O.o");
		}
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
		int key = getKey(score);
		if(Main.debug) Bukkit.getConsoleSender().sendMessage("Key in onGameEnd: " + key);
		if(Main.debug)Bukkit.getConsoleSender().sendMessage("pay: " + payOut + "  sendMe: " + sendMessages + "   sendB: " + sendBroadcasts + "    dispatch: " + dispatchCommands);

		
		giveItems:
		if(giveItems && this.giveItems &&(!player.hasPermission("gemcrush.bypass") || rewardBypass)){

			if(this.items.get(key) == null) break giveItems;
			for(ItemStack item : this.items.get(key)){
				player.getInventory().addItem(item);
			}
		}
		
		if(sendMessages && this.sendMessages && messages.get(key) != null && messages.get(key).size() > 0){
			for(String message : messages.get(key)){
				player.sendMessage(chatColor(Language.prefix + " " + message.replaceAll("%player%", player.getName()).replaceAll("%score%", score + "")));
			}
		}
		
		
		payMoney:
		if(payOut && this.pay && plugin.getEconEnabled() && (!player.hasPermission("gamebox.bypass." + Main.gameID) && (!player.hasPermission("gamebox.bypass")) || rewardBypass)){
			double reward = prices.get(key);
			if(Main.debug) Bukkit.getConsoleSender().sendMessage("Reward is: " + reward);
			if(reward > 0){
				Main.econ.depositPlayer(player, reward);
				player.sendMessage(chatColor(Language.prefix + plugin.lang.GAME_FINISHED_WITH_PAY.replaceAll("%score%", score +"").replaceAll("%reward%", reward + "")));
			} else {
				player.sendMessage(chatColor(Language.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
			}
		} else {
			player.sendMessage(chatColor(Language.prefix + plugin.lang.GAME_FINISHED_NO_PAY.replaceAll("%score%", score +"")));
		}
		
		
		if(dispatchCommands && this.dispatchCommands && commands.get(key) != null && commands.get(key).size() > 0){
			for(String cmd : commands.get(key)){
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%player%", player.getName()).replaceAll("%score%", score + ""));
			}
		}
		
		if(sendBroadcasts && this.sendBroadcasts && broadcasts.get(key) != null && broadcasts.get(key).size() > 0){
			for(String broadcast: broadcasts.get(key)){
				Bukkit.broadcastMessage(chatColor(Language.prefix + " " + broadcast.replaceAll("%player%", player.getName()).replaceAll("%score%", score + "")));
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


	@Override
	public boolean onInventoryClick(InventoryClickEvent event) {

		if(!isInGame(event.getWhoClicked().getUniqueId()) || event.getInventory() == null || event.getCurrentItem() == null || !(event.getWhoClicked() instanceof Player)){
			return true;
		}

		// player is inGame, clicked inside an inventory and the clicked item is not null

		// cancel event and return if it's not a right/left click
		event.setCancelled(true);
		if(!event.getAction().equals(InventoryAction.PICKUP_ALL) && !event.getAction().equals(InventoryAction.PICKUP_HALF)) {
			return true;
		}

		// check whether the clicked inventory is the top inventory
		if(event.getRawSlot() != event.getSlot()){
			return true;
		}

		// get Player and Game objects
		Player player = (Player) event.getWhoClicked();
		Game game = getGame(player.getUniqueId());
		if(game == null){
			clicks.remove(player.getUniqueId());
			player.closeInventory();
			games.remove(game);
			return true;
		}
		int slot = event.getSlot();

		// switch with getState
		if(game.getState() == null) return true;

		switch(game.getState()){


			case PLAY:
				if(this.clicks.containsKey(player.getUniqueId())){
					int oldSlot = clicks.get(player.getUniqueId());
					if(slot == oldSlot + 1 || slot == oldSlot - 1 || slot == oldSlot + 9 || slot == oldSlot - 9){
						if(Main.debug)player.sendMessage("Switching Gems " + slot + " and " + oldSlot);
						if(game.switchGems(slot < oldSlot ? slot : oldSlot, slot > oldSlot ? slot : oldSlot)){
							clicks.remove(player.getUniqueId());
							if(game.isPlaySounds())player.playSound(player.getLocation(), Sounds.NOTE_BASS.bukkitSound(), volume, 1f);
						} else {
							if(game.isPlaySounds())player.playSound(player.getLocation(), Sounds.VILLAGER_HIT.bukkitSound(), volume, 1f);
							//if(Main.playSounds)player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), volume, 1f);

						}
					} else if(slot == oldSlot){
						break;
					} else {
						clicks.put(player.getUniqueId(), slot);
						game.shine(slot, true);
						game.shine(oldSlot, false);
						if(game.isPlaySounds())player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), volume, 1f);
						if(Main.debug)player.sendMessage("overwritten click in " + oldSlot + " with click in " + slot);
					}
				} else {
					if(Main.debug)player.sendMessage("saved first click in slot " + slot);
					if(game.isPlaySounds())player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), volume, 1f);
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
		return true;
	}


	@Override
	public boolean onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {
		if(!isInGame(inventoryCloseEvent.getPlayer().getUniqueId())){
			return true;
		}
		if(Main.debug)inventoryCloseEvent.getPlayer().sendMessage("Inventory was closed");//XXX
		getGame(inventoryCloseEvent.getPlayer().getUniqueId()).shutDown();
		removeGame(getGame(inventoryCloseEvent.getPlayer().getUniqueId()));
		return true;
	}

	@Override
	public boolean isInGame(UUID uuid) {
		for(Game game : games){
			if(isPlayer(uuid, game)){
				return true;
			}
		}
		return false;
	}

	@Override
	public int startGame(Player[] players, boolean playSounds, String... strings) {
		if(strings == null || strings.length < 1) {
			new Exception("No arguments to start a game").printStackTrace();
			Bukkit.getLogger().log(Level.WARNING, " Error while starting a game");
			return GameBox.GAME_NOT_STARTED_ERROR;
		} else if(strings.length == 1){
			// strings[0] should be a registered game type
			for(String id : gameTypes.keySet()){
				if(!id.equalsIgnoreCase(strings[0])) continue;
				GameRules rules = gameTypes.get(id);
				if(!pay(players, rules.getCost())){
					return GameBox.GAME_NOT_ENOUGH_MONEY;
				}
				games.add(new Game(plugin, players[0].getUniqueId(), rules.getMoves(), rules.isBombs(), rules.getNumberOfGemTypes(), gems, (playSounds && Main.playSounds), rules));
				return GameBox.GAME_STARTED;
			}

		}
		Bukkit.getLogger().log(Level.WARNING, "not supported number of arguments to start a game");
		return GameBox.GAME_NOT_STARTED_ERROR;
	}

	@Override
	public void removeFromGame(UUID uuid) {
		removeGame(uuid);
	}

	private boolean pay(Player[] player, double cost) {
		if (plugin.getEconEnabled() && !player[0].hasPermission("gamebox.bypass." + Main.gameID) && !player[0].hasPermission("gamebox.bypass") && cost > 0.0) {
			if (Main.econ.getBalance(player[0]) >= cost) {
				Main.econ.withdrawPlayer(player[0], cost);
				player[0].sendMessage(plugin.chatColor(Language.prefix + plugin.lang.GAME_PAYED.replaceAll("%cost%", String.valueOf(cost))));
				return true;
			} else {
				player[0].sendMessage(plugin.chatColor(Language.prefix + plugin.lang.GAME_NOT_ENOUGH_MONEY));
				return false;
			}
		} else {
			return true;
		}
	}

	public void setGameTypes(Map<String,GameRules> gameTypes) {
		this.gameTypes = gameTypes;
	}

	public void saveStats(UUID uniqueId, int points, String key) {
		statistics.addStatistics(uniqueId, Main.gameID, key, (double) points, SaveType.SCORE);
	}
}
