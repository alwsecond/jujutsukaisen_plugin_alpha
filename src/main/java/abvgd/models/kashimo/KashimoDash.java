package abvgd.models.kashimo;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class KashimoDash extends DashAbility {

    public KashimoDash() {
        super(new JJKAbilityInfo(
                "Lightning Flash",
                Material.LIGHTNING_ROD, // Громоотвод как иконка
                0, 15, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();

        // Звук раската грома и искр
        world.playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
        world.playSound(start, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 1.5f);

        // Импульс (множитель 2.2 примерно даст 10-12 блоков в зависимости от трения)
        player.setVelocity(direction.multiply(2.2).setY(0.1));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 6) { // Чуть короче по времени, чем у Махито, для резкости
                    this.cancel();
                    return;
                }

                // Визуал: Шлейф из синих/голубых молний
                Location loc = player.getLocation().add(0, 1, 0);

                // Основные искры
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.2, 0.2, 0.2, 0.5);
                // Вспышки
                world.spawnParticle(Particle.FIREWORK, loc, 5, 0.1, 0.1, 0.1, 0.05);
                // Эффект "облака" после разряда
                world.spawnParticle(Particle.CLOUD, loc, 2, 0.05, 0.05, 0.05, 0.01);

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}