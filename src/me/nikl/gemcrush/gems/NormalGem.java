package me.nikl.gemcrush.gems;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Created by niklas on 10/3/16.
 *
 * NormalGem class
 */
public class NormalGem extends Gem{
	
	double possibility = 1.;
	
	
	public NormalGem(Material material, String name){
		super(material, name);
	}
	
	public NormalGem(Material material, String name, short dur){
		super(material, name, dur);
	}
	
	public NormalGem(NormalGem copyFrom){
		super(copyFrom.getItem().getType(), copyFrom.name, copyFrom.getItem().getDurability(), copyFrom.lore);
		this.possibility = copyFrom.possibility;
		this.pointsOnBreak = copyFrom.pointsOnBreak;
	}
	
	
	public NormalGem(ItemStack item, NormalGem gem){
		super(item.getType(), gem.getName(), item.getDurability(), item.getItemMeta().getLore());
		
		this.possibility = gem.possibility;
	}
	
	
	@Override
	public void onBreak() {
		
	}
	
	public void setPossibility(double possibility){
		this.possibility = possibility;
	}
	
	public double getPossibility(){
		return this.possibility;
	}
}
