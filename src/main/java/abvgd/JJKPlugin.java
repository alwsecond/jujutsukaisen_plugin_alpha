package abvgd;

import abvgd.core.JJKDomain;
import abvgd.utils.JJKCommands;
import abvgd.utils.JJKListener;
import abvgd.utils.JJKTicker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class JJKPlugin extends JavaPlugin {
    public static List<JJKDomain> activeDomains = new ArrayList<>();
    private static JJKPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        getCommand("jjk").setExecutor(new JJKCommands());
        getServer().getPluginManager().registerEvents(new JJKListener(), this);
        getLogger().info("JJK Plugin!");
        new JJKTicker().runTaskTimer(this, 0, 2);
    }

    public static JJKPlugin getInstance() {
        return instance;
    }
}