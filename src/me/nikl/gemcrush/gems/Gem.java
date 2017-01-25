package me.nikl.gemcrush.gems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by niklas on 10/3/16.
 *
 * Abstract class for all Gems
 */
public abstract class Gem{
	ArrayList<String> lore;
	String name;
	int pointsOnBreak;
	ItemStack item;
	
	Gem() {
	}
	
	public abstract void onBreak();
	
	Gem(Material material, String name){
		this.item = new ItemStack(material, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		//meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		if(lore != null && lore.size() > 0){
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		this.name = name;
		
		this.pointsOnBreak = 10;
		
		//shine(false);
	}
	
	Gem(ItemStack item, Gem gem){
		this(item.getType(), gem.getName(), item.getDurability(), item.getItemMeta().getLore());
		
		this.pointsOnBreak = gem.getPointsOnBreak();
	}
	
	
	Gem(Material material, String name, short durability){
		this(material, name);
		item.setDurability(durability);
		this.name = name;
		
		this.pointsOnBreak = 10;
	}
	
	
	Gem(Material material, String name, short durability, List<String> lore) {
		this(material, name, durability);
		if(lore != null && !lore.isEmpty())
			this.setLore(new ArrayList(lore));
	}
	
	public Gem(Gem copyFrom){
		this(copyFrom.getItem().getType(), copyFrom.getName(), copyFrom.getItem().getDurability(), copyFrom.getItem().getItemMeta().getLore());
		this.lore = copyFrom.lore;
		this.pointsOnBreak = copyFrom.pointsOnBreak;
		//this.shine(false);
	}
	
	public void setLore(ArrayList lore){
		ItemMeta meta = item.getItemMeta();
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	
	public String getName(){
		return this.name;
	}
	
	public int getPointsOnBreak(){
		return this.pointsOnBreak;
	}
	
	public void setPointsOnBreak(int pointsOnBreak){
		this.pointsOnBreak = pointsOnBreak;
	}
	
	public ItemStack getItem(){
		return item;
	}
	
	public void setItem(ItemStack item){
		this.item = item;
	}
}
