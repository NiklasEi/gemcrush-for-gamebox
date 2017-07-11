package me.nikl.gemcrush.nms;

import net.minecraft.server.v1_12_R1.ChatMessage;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.PacketPlayOutOpenWindow;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by niklas on 11/19/16.
 *
 *
 */
public class Update_1_12_R1 implements InvTitle{

	@Override
	public void updateTitle(Player player, String newTitle) {
		EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
		PacketPlayOutOpenWindow packet = new PacketPlayOutOpenWindow(entityPlayer.activeContainer.windowId,
				"minecraft:chest", new ChatMessage(ChatColor.translateAlternateColorCodes('&',newTitle)),
				player.getOpenInventory().getTopInventory().getSize());
		entityPlayer.playerConnection.sendPacket(packet);
		entityPlayer.updateInventory(entityPlayer.activeContainer);
	}

	@Override
	public org.bukkit.inventory.ItemStack removeGlow(org.bukkit.inventory.ItemStack item) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
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
	public org.bukkit.inventory.ItemStack addGlow(org.bukkit.inventory.ItemStack item){
		if(item == null) return null;
		item.addUnsafeEnchantment(Enchantment.LUCK, 1);
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		return item;
	}
}
