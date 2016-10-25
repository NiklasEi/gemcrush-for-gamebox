package me.nikl.gemcrush.nms;

import me.nikl.gemcrush.gems.Gem;
import me.nikl.gemcrush.gems.NormalGem;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Update_1_8_R2 implements InvTitle{

	@Override
	public void updateTitle(Player player, String newTitle) {
		EntityPlayer ep = ((CraftPlayer)player).getHandle();
		PacketPlayOutOpenWindow packet = new PacketPlayOutOpenWindow(ep.activeContainer.windowId, "minecraft:chest", new ChatMessage(newTitle), player.getOpenInventory().getTopInventory().getSize());
		ep.playerConnection.sendPacket(packet);
		ep.updateInventory(ep.activeContainer);
	}
	
	@Override
	public ItemStack removeGlow(ItemStack item) {
		net.minecraft.server.v1_8_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = null;
		if (nmsStack.hasTag()) {
			tag = nmsStack.getTag();
			tag.remove("ench");
			nmsStack.setTag(tag);
			return CraftItemStack.asCraftMirror(nmsStack);
		}
		return item;
	}
	
	
	@Override
	public ItemStack addGlow(ItemStack item){
		net.minecraft.server.v1_8_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = null;
		if (!nmsStack.hasTag()) {
			tag = new NBTTagCompound();
			nmsStack.setTag(tag);
		}
		if (tag == null) tag = nmsStack.getTag();
		NBTTagList ench = new NBTTagList();
		tag.set("ench", ench);
		nmsStack.setTag(tag);
		return CraftItemStack.asCraftMirror(nmsStack);
	}
	/*
	
	@Override
	public ItemStack addGlow(NormalGem gem) {
		net.minecraft.server.v1_8_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(gem);
		NBTTagCompound tag = null;
		if (!nmsStack.hasTag()) {
			tag = new NBTTagCompound();
			nmsStack.setTag(tag);
		}
		if (tag == null) tag = nmsStack.getTag();
		NBTTagList ench = new NBTTagList();
		tag.set("ench", ench);
		nmsStack.setTag(tag);
		return new NormalGem(CraftItemStack.asCraftMirror(nmsStack), gem);
	}
	
	
	@Override
	public ItemStack removeGlow(NormalGem gem) {
		net.minecraft.server.v1_8_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(gem);
		NBTTagCompound tag = null;
		if (nmsStack.hasTag()) {
			tag = nmsStack.getTag();
			tag.remove("ench");
			nmsStack.setTag(tag);
			return new NormalGem(CraftItemStack.asCraftMirror(nmsStack), gem);
		}
		return gem;
	}
	
	@Override
	public ItemStack addGlow(Gem gem) {
		return null;
	}
	
	@Override
	public ItemStack removeGlow(Gem gem) {
		return null;
	}
	*/
}
