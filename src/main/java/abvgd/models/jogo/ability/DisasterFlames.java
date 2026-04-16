package abvgd.models.jogo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class DisasterFlames extends ActiveAbility {

    public DisasterFlames() {
        super(new JJKAbilityInfo(
                "Disaster Flames",
                Material.BLAZE_ROD,
                45, 90, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // Замораживаем игрока на время выпуска потока (2 сек = 40 тиков)
        JJKFunc.freezePlayer(player, 40);

        // Звуки мощного огнемета
        world.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 40 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                Location origin = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
                Vector direction = player.getLocation().getDirection().normalize();

                // Периодический звук "рева" пламени
                if (ticks % 5 == 0) {
                    world.playSound(origin, Sound.ENTITY_GHAST_SHOOT, 0.7f, 0.5f);
                }

                // Рисуем конус пламени длиной 10 блоков
                for (double d = 1; d <= 10; d += 0.5) {
                    // Конус расширяется с расстоянием
                    double spread = d * 0.15;
                    Location point = origin.clone().add(direction.clone().multiply(d));

                    // Основные частицы огня
                    world.spawnParticle(Particle.FLAME, point, 5, spread, spread, spread, 0.1);
                    world.spawnParticle(Particle.LAVA, point, 1, spread, spread, spread, 0.05);

                    if (ticks % 4 == 0) {
                        world.spawnParticle(Particle.LARGE_SMOKE, point, 2, spread, spread, spread, 0.02);
                    }

                    // Проверка урона по пути луча
                    for (Entity entity : world.getNearbyEntities(point, spread + 0.5, spread + 0.5, spread + 0.5)) {
                        if (entity instanceof LivingEntity target && entity != player) {
                            // Наносим урон через JJKDamage
                            JJKDamage.causeAbilityDamage(target, player, 4.0); // Урон за каждый тик попадания
                            target.setFireTicks(60); // Обновляем поджог

                            // Небольшое отталкивание назад от напора пламени
                            target.setVelocity(direction.clone().multiply(0.2).setY(0.1));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
