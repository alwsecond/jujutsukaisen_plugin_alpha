package abvgd.models.yuji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SoulScissors extends ActiveAbility {

    private final Color neonBlue = Color.fromRGB(0, 255, 255);
    private final Color deepBlue = Color.fromRGB(0, 100, 255);

    public SoulScissors() {
        super(new JJKAbilityInfo(
                "§b§lScissors",
                Material.SHEARS,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        RayTraceResult ray = world.rayTraceEntities(start, direction, 5.0, 1.2, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {

            // Звук начала отрисовки (высокочастотный звон)
            world.playSound(victim.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.8f);

            new BukkitRunnable() {
                double progress = 0.0; // Прогресс отрисовки от 0 до 1.5 блоков в каждую сторону
                final Location center = victim.getLocation().add(0, 1.1, 0);
                final Vector side = player.getLocation().getDirection().rotateAroundY(Math.toRadians(90)).normalize();

                @Override
                public void run() {
                    // Анимация отрисовки (из центра в стороны)
                    if (progress < 1.5) {
                        // Рисуем точки на текущей дистанции 'progress'
                        for (int sideSign : new int[]{1, -1}) {
                            Vector currentOffset = side.clone().multiply(progress * sideSign);

                            // Верхняя и нижняя линии
                            Location p1 = center.clone().add(currentOffset).add(0, 0.2, 0);
                            Location p2 = center.clone().add(currentOffset).add(0, -0.2, 0);

                            // Эффект "горения" проклятой энергии в точках отрисовки
                            world.spawnParticle(Particle.DUST, p1, 3, 0.02, 0.02, 0.02, new Particle.DustOptions(neonBlue, 1.2f));
                            world.spawnParticle(Particle.DUST, p2, 3, 0.02, 0.02, 0.02, new Particle.DustOptions(deepBlue, 1.2f));

                            if (progress > 0.5) {
                                world.spawnParticle(Particle.ELECTRIC_SPARK, p1, 1, 0, 0, 0, 0);
                            }
                        }

                        // Звук "скольжения" лезвия
                        if (progress == 0.0) world.playSound(center, Sound.ITEM_TRIDENT_THROW, 0.5f, 2.0f);

                        progress += 0.25; // Скорость отрисовки
                    } else {
                        // После того как линии отрисованы — финальный удар
                        executeShatter(player, victim, center, side);
                        this.cancel();
                    }
                }
            }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L); // Отрисовка каждый тик

        } else {
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }
    }

    private void executeShatter(Player player, LivingEntity victim, Location center, Vector side) {
        World world = victim.getWorld();

        // ФИНАЛЬНЫЙ ЭФФЕКТ: Схлопывание линий
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.8f);
        world.playSound(center, Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.2f, 0.6f);
        world.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 2.0f);

        // Весь разрез вспыхивает вдоль линий
        for (double i = -1.5; i <= 1.5; i += 0.3) {
            Location p = center.clone().add(side.clone().multiply(i));
            world.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0.1, 0.1, 0.1, 0);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, 0.1, 0.1, 0.1, 0.02);
        }

        world.spawnParticle(Particle.FLASH, center, 2, 0.1, 0.1, 0.1, 0);

        JJKDamage.causeAbilityDamage(victim, player, 10.0);
        player.sendActionBar("§b§lSCISSORS: SNAP!");
    }
}