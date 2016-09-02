package me.nikl.gemcrush.game;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import me.nikl.gemcrush.Language;
import me.nikl.gemcrush.Main;


public class Game extends BukkitRunnable{
	// items that make up the game
	private ItemStack gem1, gem2, gem3,gem4;
	// the four grids the players play on

	private GameState state;
	private GameManager manager;
	// UUIDs of the first and second player
	private UUID playerUUID;
	// first is the player that invited, second is the one that excepted
	private Player player;
	// save the config
	private FileConfiguration config;
	// language class
	private Language lang;
	// inventory
	private Inventory inv;
	
	// current inventorytitle
	private String title;
	
	private Main plugin;
	
	public Game(Main plugin, UUID playerUUID){
		this.plugin = plugin;
		this.lang = plugin.lang;
		this.config = plugin.getConfig();
		this.manager = plugin.getManager();
		this.playerUUID = playerUUID;
		this.player = Bukkit.getPlayer(playerUUID);
		if(player == null){
			manager.removeGame(this);
			return;
		}
		if(config == null){
			Bukkit.getConsoleSender().sendMessage(Main.prefix + " Failed to load config!");
			Bukkit.getPluginManager().disablePlugin(plugin);
		}
		if(!getMaterials()){
			setDefaultMaterials();
		}
		this.title = lang.TITLE_SET_SHIP_1.replaceAll("%count%", "");
		this.inv = Bukkit.getServer().createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', title));
		
		// this basically starts the game
		this.state = GameState.FILLING;
		player.openInventory(this.inv);
		this.runTaskTimer(Main.getPlugin(Main.class), 0, 5);
	}



	public void fillUp() {
		if(hasToBeFilled()){
			this.state = GameState.FILLING;
		}
	}

	private boolean hasToBeFilled() {
		for(int slot = 0 ; slot < 54 ; slot++){
			if(this.inv.getItem(slot) == null) return true;
		}
		return false;
	}


	public GameState getState() {
		return state;
	}

	public void setState(GameState state) {
		this.state = state;
		switch (state){
		
		
		default:
			break;
		}
	}

	public UUID getUUID() {
		return playerUUID;
	}



	public void won(boolean isFirst) {
		if(plugin.getEconEnabled()){
			Main.econ.depositPlayer(player, plugin.getReward());
			player.sendMessage(manager.chatColor(Main.prefix + lang.GAME_WON_MONEY.replaceAll("%reward%", plugin.getReward()+"")));
		} else {
			player.sendMessage(manager.chatColor(Main.prefix + lang.GAME_WON));
		}

	}
	
	public void onGameEnd(String winner, String looser){
		String path = "onGameEnd.dispatchCommands";
		if(config.getBoolean(path + ".enabled")){
			List<String> cmdList = config.getStringList(path + ".commands");
			if(cmdList != null && !cmdList.isEmpty()){
				for(String cmd : cmdList){
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%winner%", winner).replaceAll("%looser%", looser));
				}
			}
		}
		path = "onGameEnd.broadcast";
		if(config.getBoolean(path + ".enabled")){
			List<String> broadcastList = config.getStringList(path + ".messages");
			if(broadcastList != null && !broadcastList.isEmpty()){
				for(String message : broadcastList){
					Bukkit.broadcastMessage(chatColor(Main.prefix + " " + message.replaceAll("%winner%", winner).replaceAll("%looser%", looser)));
				}
			}
		}
	}


	private String chatColor(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	/***
	 * Get the materials and displaynames for the gems from the config file
	 * 
	 * @return worked
	 */
	private boolean getMaterials() {
		boolean worked = true;

	    Material mat = null;
	    int data = 0;
	    for(String key : Arrays.asList("gem1", "gem2", "gem3", "gem4")){

		    if(!config.isSet("items." + key + ".material")) return false;
		    if(!config.isSet("items." + key + ".displayName") || !config.isString("items." + key + ".displayName")) return false;
		   
	    	String value = config.getString("items." + key + ".material");
		    String[] obj = value.split(":");
		    String name = config.getString("items." + key + ".displayName");
	
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
		    if(mat == null) return false;
		    if(key.equals("gem1")){
				this.gem1 = new ItemStack(mat, 1);
				if (obj.length == 2) gem1.setDurability((short) data);
				ItemMeta meta = gem1.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				meta.setDisplayName(name);
				gem1.setItemMeta(meta);

		    } else if(key.equals("gem2")){
		    	this.gem2 = new ItemStack(mat, 1);
		    	if (obj.length == 2) gem2.setDurability((short) data);
		    	ItemMeta meta = gem2.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		    	meta.setDisplayName(name);
		    	gem2.setItemMeta(meta);
		    	
		    } else if(key.equals("gem3")){
				this.gem3 = new ItemStack(mat, 1);
				if (obj.length == 2) gem3.setDurability((short) data);
				ItemMeta meta = gem3.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				meta.setDisplayName(name);
				gem3.setItemMeta(meta);
		    	
		    } else if(key.equals("gem4")){
				this.gem4 = new ItemStack(mat, 1);
				if (obj.length == 2) gem4.setDurability((short) data);
				ItemMeta meta = gem4.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				meta.setDisplayName(name);
				gem4.setItemMeta(meta);
		    }
	    }
		return worked;
	}
	
	private void setDefaultMaterials() {
		Bukkit.getConsoleSender().sendMessage(plugin.chatColor(Main.prefix+" &4Failed to load materials from config"));
		Bukkit.getConsoleSender().sendMessage(plugin.chatColor(Main.prefix+" &4Using default materials"));
		
		this.gem1 = new ItemStack(Material.DIAMOND);
		ItemMeta metaGem1 = gem1.getItemMeta();
		metaGem1.setDisplayName("Diamond");
		metaGem1.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		gem1.setItemMeta(metaGem1);
		gem1.setAmount(1);

		this.gem2 = new ItemStack(Material.EMERALD);
		ItemMeta metaGem2 = gem2.getItemMeta();
		metaGem2.setDisplayName("Emerald");
		metaGem2.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		gem2.setItemMeta(metaGem2);
		gem2.setAmount(1);
		
		this.gem3 = new ItemStack(Material.BLAZE_POWDER);
		ItemMeta metaGem3 = gem3.getItemMeta();
		metaGem3.setDisplayName("Stardust");
		metaGem3.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		gem3.setItemMeta(metaGem3);
		gem3.setAmount(1);
		
		this.gem4 = new ItemStack(Material.NETHER_STAR);
		ItemMeta metaGem4 = gem4.getItemMeta();
		metaGem4.setDisplayName("Star");
		metaGem4.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		gem4.setItemMeta(metaGem4);
		gem4.setAmount(1);
	}


	public boolean isInventory(int hashCode) {
		return (this.inv.hashCode() == hashCode);
	}


	@Override
	public void run() {
		if(!(this.state == GameState.FILLING)) return;
		for(int column = 8 ; column > -1 ; column--){
			for(int row = 5; row > -1 ; row --){
				int slot = row*9 + column;
				if(this.inv.getItem(slot) == null){
					if(row == 0){
						spawnGem(slot);
						break;
					} else {
						if(this.inv.getItem(slot-9) != null){
							this.inv.setItem(slot, this.inv.getItem(slot-9));
							this.inv.setItem(slot-9, new ItemStack(Material.AIR, 1));
							break;
						} else {
							continue;
						}
					}
				}
			}
		}
		if(!hasToBeFilled()){
			this.state = GameState.PLAY;
		}
		
	}


	private void spawnGem(int slot) {
		double random = new Random().nextDouble();
		if(random >= 0.75){
			this.inv.setItem(slot, gem1);
		} else if (random >= 0.5) {
			this.inv.setItem(slot, gem2);
		} else if (random >= 0.25) {
			this.inv.setItem(slot, gem3);
		} else {
			this.inv.setItem(slot, gem4);
		}		
	}



	public void remove(int slot) {
		this.inv.getItem(slot).addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 0);		
	}
	
}
