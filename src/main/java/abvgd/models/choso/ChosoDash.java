package abvgd.models.choso;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChosoDash extends DashAbility {

    public ChosoDash() {
        super(new JJKAbilityInfo(
                "Blood Burst",
                Material.REDSTONE,
                0, 15, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();

        // Звук: Всплеск жидкости и резкий рывок
        world.playSound(start, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1f, 0.5f);
        world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);

        // Импульс
        player.setVelocity(direction.multiply(1.8).setY(0.2));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 8) {
                    this.cancel();
                    return;
                }

                // Визуал: Шлейф из капель крови
                Location loc = player.getLocation().add(0, 1, 0);

                // Используем частицы DUST темно-красного цвета
                Particle.DustOptions bloodDust = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.5f);
                world.spawnParticle(Particle.DUST, loc, 10, 0.2, 0.3, 0.2, bloodDust);

                // Добавляем немного капель
                world.spawnParticle(Particle.DRIPPING_DRIPSTONE_WATER, loc, 5, 0.1, 0.1, 0.1);

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
