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

public class EmberInsects extends ActiveAbility {

    public EmberInsects() {
        super(new JJKAbilityInfo(
                "Ember Insects",
                Material.FIRE_CHARGE,
                40, 100, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // Джого выпускает 3-х жуков по очереди

        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnInsect(player);
                    world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.5f, 0.5f);
                }
            }.runTaskLater(JJKPlugin.getInstance(), i * 5L); // Вылетают с небольшой задержкой
        }
    }

    private void spawnInsect(Player caster) {
        World world = caster.getWorld();
        Location start = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(1));

        new BukkitRunnable() {
            Location current = start.clone();
            int life = 0;
            Entity target = null;

            @Override
            public void run() {
                // Ищем цель в радиусе 15 блоков, если её еще нет
                if (target == null || target.isDead() || !target.isValid()) {
                    target = findNearestTarget(current, caster, 15);
                }

                // Логика полета: самонаведение
                Vector moveDir;
                if (target != null) {
                    moveDir = target.getLocation().add(0, 1, 0).toVector().subtract(current.toVector()).normalize().multiply(0.7);
                } else {
                    moveDir = caster.getLocation().getDirection().multiply(0.5); // Летит просто вперед, если нет цели
                }

                current.add(moveDir);

                // ВИЗУАЛ ЖУКА
                world.spawnParticle(Particle.SMALL_FLAME, current, 5, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.SMOKE, current, 2, 0.05, 0.05, 0.05, 0.01);

                // ПРОВЕРКА ДЕТОНАЦИИ
                if (target != null && current.distance(target.getLocation().add(0, 1, 0)) < 1.2) {
                    explode(current, caster);
                    this.cancel();
                    return;
                }

                // Взрыв при столкновении с блоком или по истечении времени
                if (current.getBlock().getType().isSolid() || life > 60) {
                    explode(current, caster);
                    this.cancel();
                    return;
                }

                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void explode(Location loc, Player caster) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION, loc, 1);
        world.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.2);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);

        for (Entity e : world.getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof LivingEntity victim && e != caster) {
                JJKDamage.causeAbilityDamage(victim, caster, 6.0);
                victim.setFireTicks(40); // Поджигает на 2 сек
            }
        }
    }

    private Entity findNearestTarget(Location loc, Player caster, double range) {
        Entity nearest = null;
        double minDist = range;
        for (Entity e : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (e instanceof LivingEntity && e != caster) {
                double dist = e.getLocation().distance(loc);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = e;
                }
            }
        }
        return nearest;
    }
}
