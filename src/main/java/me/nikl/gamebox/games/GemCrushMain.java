package me.nikl.gamebox.games;

import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.Module;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Niklas Eicker
 */
public class GemCrushMain extends JavaPlugin {
    public static final String GEM_CRUSH = "gemcrush";
    private GameBox gameBox;

    @Override
    public void onEnable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("GameBox");
        if (plugin == null || !plugin.isEnabled()) {
            getLogger().warning(" GameBox was not found! Disabling GemCrush...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        gameBox = (GameBox) plugin;
        new Module(gameBox, GEM_CRUSH
                , "me.nikl.gamebox.games.gemcrush.GemCrush"
                , this, GEM_CRUSH, "gc");
    }
}
