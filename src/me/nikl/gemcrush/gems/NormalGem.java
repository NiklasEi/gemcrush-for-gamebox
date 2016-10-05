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
	
	
	@Override
	public void onBreak() {
		
	}
	
	public String getName(){
		return this.name;
	}
}
