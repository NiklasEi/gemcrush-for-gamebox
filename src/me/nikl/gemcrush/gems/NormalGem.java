package me.nikl.gemcrush.gems;

import org.bukkit.Material;

/**
 * Created by niklas on 10/3/16.
 */
public class NormalGem extends Gem{
	
	
	public NormalGem(Material material, String name){
		super(material, name);
	}
	
	public NormalGem(Material material, String name, short dur){
		super(material, name, dur);
	}
	
	public NormalGem(NormalGem copyFrom){
		this.item = copyFrom.item;
		this.name = copyFrom.name;
		this.lore = copyFrom.lore;
	}
	
	
	@Override
	public void onBreak() {
		
	}
	
	public String getName(){
		return this.name;
	}
}
