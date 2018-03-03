package me.nikl.gamebox.games.gemcrush.gems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * Created by niklas on 10/23/16.
 *
 */
public class Bomb extends Gem {
	
	public Bomb(String displayName, ArrayList<String> lore, int pointsOnBreak){
		super(Material.TNT, "Bomb");
		ItemMeta meta = item.getItemMeta();
		if(displayName != null) {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
		} else {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Bomb"));
		}
		if(lore != null && !lore.isEmpty()) {
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		this.pointsOnBreak = pointsOnBreak;
	}
	
	@Override
	public void onBreak() {
		
	}
	
	public String getName(){
		return "Bomb";
	}
}
