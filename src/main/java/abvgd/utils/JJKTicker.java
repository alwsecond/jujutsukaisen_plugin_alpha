package abvgd.utils;

import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class JJKTicker extends BukkitRunnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            JJKPlayer jjkPlayer = PlayerManager.get(player);
            if (jjkPlayer.getModel() == null) continue;

            jjkPlayer.tickRegeneration();
            JJKHUD.update(player, jjkPlayer);
        }
    }
}
