package me.nikl.gamebox.games.gemcrush.gems;

import org.bukkit.Material;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Niklas Eicker
 *
 * NormalGem class
 */
public class NormalGem extends Gem {
    double possibility = 1.;

    public NormalGem(Material material, String name) {
        super(material, name);
    }

    public NormalGem(Material material, String name, short dur) {
        super(material, name, dur);
    }

    public NormalGem(NormalGem copyFrom) {
        super(copyFrom.getItem().getType(), copyFrom.name);
        ItemMeta meta = item.getItemMeta();
        ItemMeta oldMeta = copyFrom.getItem().getItemMeta();
        if (meta instanceof Damageable && oldMeta instanceof Damageable) {
            ((Damageable) meta).setDamage(((Damageable) oldMeta).getDamage());
        }
        meta.setLore(oldMeta.getLore());
        item.setItemMeta(meta);
        this.possibility = copyFrom.possibility;
        this.pointsOnBreak = copyFrom.pointsOnBreak;
    }

    public double getPossibility() {
        return this.possibility;
    }

    public void setPossibility(double possibility) {
        this.possibility = possibility;
    }
}
