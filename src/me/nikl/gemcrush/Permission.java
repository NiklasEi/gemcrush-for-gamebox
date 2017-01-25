package me.nikl.gemcrush;

import org.bukkit.entity.Player;

/**
 * Created by niklas on 1/5/17.
 */
public enum Permission {
	ADMIN("gemcrush.admin"), PLAY("gemcrush.play"), RELOAD("gemcrush.reload"), TOP("gemcrush.top"),
		BYPASS("gemcrush.bypass");
	
	String perm;
	
	Permission(String perm){
		this.perm = perm;
	}
	
	boolean has(Player player){
		return player.hasPermission(perm);
	}
}
