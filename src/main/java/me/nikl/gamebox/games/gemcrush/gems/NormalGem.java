package me.nikl.gamebox.games.gemcrush.gems;

import org.bukkit.Material;

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
        super(copyFrom.getItem().getType(), copyFrom.name, copyFrom.getItem().getDurability(), copyFrom.lore);
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
