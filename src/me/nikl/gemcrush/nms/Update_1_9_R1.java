package me.nikl.gemcrush.nms;

import net.minecraft.server.v1_9_R1.ChatMessage;
import net.minecraft.server.v1_9_R1.EntityPlayer;
import net.minecraft.server.v1_9_R1.PacketPlayOutOpenWindow;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;


public class Update_1_9_R1 implements InvTitle{

	@Override
	public void updateTitle(Player player, String newTitle) {
		EntityPlayer ep = ((CraftPlayer)player).getHandle();
		PacketPlayOutOpenWindow packet = new PacketPlayOutOpenWindow(ep.activeContainer.windowId, "minecraft:chest", new ChatMessage(newTitle), player.getOpenInventory().getTopInventory().getSize());
		ep.playerConnection.sendPacket(packet);
		ep.updateInventory(ep.activeContainer);
	}

}
