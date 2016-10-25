package me.nikl.gemcrush.nms;

import me.nikl.gemcrush.gems.Gem;
import me.nikl.gemcrush.gems.NormalGem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public interface InvTitle {

	
	void updateTitle(Player player, String newTitle);
	
	ItemStack removeGlow(ItemStack item);
	
	ItemStack addGlow(ItemStack item);
	
	/*
	ItemStack addGlow(NormalGem gem);
	
	ItemStack removeGlow(NormalGem gem);
	
	ItemStack addGlow(Gem gem);
	
	ItemStack removeGlow(Gem gem);
	*/
}
