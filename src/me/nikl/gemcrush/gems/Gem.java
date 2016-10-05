package me.nikl.gemcrush.gems;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * Created by niklas on 10/3/16.
 */
public abstract class Gem {
	ItemStack item;
	int slot;
	ArrayList<String> lore;
	String name;
	
	public abstract void onBreak();
	
	public Gem(Material material, String name){
		this.item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		item.setAmount(1);
		this.name = name;
		
		shine(false);
		
		this.slot = -1;
	}
	
	
	public Gem(Material material, String name, short durability){
		this(material, name);
		this.item.setDurability(durability);
		this.name = name;
	}
	
	public Gem(Material material, String name, int slot){
		this(material, name);
		this.slot = slot;
		this.name = name;
	}
	
	public void setLore(ArrayList lore){
		ItemMeta meta = this.item.getItemMeta();
		meta.setLore(lore);
		this.item.setItemMeta(meta);
	}
	
	public ItemStack getItem(){
		return this.item;
	}
	
	
	
	public void shine(boolean shine){
		if(shine){
			this.item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 0);
		} else {
			if(!this.item.getItemMeta().hasEnchants()) return;
			for(Enchantment en : this.item.getEnchantments().keySet()){
				this.item.removeEnchantment(en);
			}
		}
	}
	
	
	public void moveTo(int to){
		this.slot = to;
	}
	
	public String getName(){
		return this.name;
	}
}
