package me.nikl.gamebox.games.gemcrush.gems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by niklas on 10/3/16.
 *
 * Abstract class for all Gems
 */
public abstract class Gem {
    ArrayList<String> lore;
    String name;
    int pointsOnBreak;
    ItemStack item;

    Gem(Material material, String name) {
        this.item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        this.name = name;
        this.pointsOnBreak = 10;
    }

    Gem(Material material, String name, short durability) {
        this(material, name);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(durability);
            item.setItemMeta(meta);
        }
        this.name = name;
        this.pointsOnBreak = 10;
    }

    Gem(Material material, String name, short durability, List<String> lore) {
        this(material, name, durability);
        if (lore != null && !lore.isEmpty())
            this.setLore(new ArrayList<>(lore));
    }

    public void setLore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public String getName() {
        return this.name;
    }

    public int getPointsOnBreak() {
        return this.pointsOnBreak;
    }

    public void setPointsOnBreak(int pointsOnBreak) {
        this.pointsOnBreak = pointsOnBreak;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }
}
