package abvgd.models.jogo;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class JogoDash extends DashAbility {
    public JogoDash() {
        super(new JJKAbilityInfo("Flame Boost", Material.BLAZE_POWDER, 0, 12, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector direction = player.getLocation().getDirection().normalize();

        // Звук реактивного двигателя
        world.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 1.5f);

        player.setVelocity(direction.multiply(1.7).setY(0.25));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 6) { this.cancel(); return; }

                // Огненный шлейф
                Location loc = player.getLocation().add(0, 0.5, 0);
                world.spawnParticle(Particle.FLAME, loc, 15, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.LAVA, loc, 3, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.SMOKE, loc, 5, 0.1, 0.1, 0.1, 0.02);

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
