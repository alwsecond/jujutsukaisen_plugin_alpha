package abvgd.models.kashimo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class MagneticPull extends ActiveAbility {

    public MagneticPull() {
        super(new JJKAbilityInfo(
                "Magnetic Pull <interact>",
                Material.CHAIN,
                30, 40, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        // Луч (визуальная нить притяжения)
        BlockDisplay beam = world.spawn(start, BlockDisplay.class, b -> {
            b.setBlock(Material.LIGHT_BLUE_STAINED_GLASS.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(0.05f, 0.05f, 1.0f);
            b.setTransformation(t);
        });

        world.playSound(start, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 2f);

        new BukkitRunnable() {
            int ticks = 0;
            Location current = start.clone();
            boolean hit = false;

            @Override
            public void run() {
                if (ticks > 25 || hit || !player.isOnline()) {
                    beam.remove();
                    this.cancel();
                    return;
                }

                current.add(direction.clone().multiply(1.5));
                beam.teleport(current);
                world.spawnParticle(Particle.ELECTRIC_SPARK, current, 2, 0.05, 0.05, 0.05, 0.01);

                // 1. ПРОВЕРКА НА БЛОКИ (Притяжение к стене/полу)
                if (current.getBlock().getType().isSolid()) {
                    hit = true;
                    pullPlayerToTarget(player, current); // Притягиваемся к точке на блоке
                    return;
                }

                // 2. ПРОВЕРКА НА СУЩЕСТВ (Притяжение к врагу + Разряд)
                for (Entity e : world.getNearbyEntities(current, 1.3, 1.3, 1.3)) {
                    if (e instanceof LivingEntity target && e != player) {
                        hit = true;
                        pullPlayerToTarget(player, target.getLocation());

                        // Логика разряда при столкновении
                        new BukkitRunnable() {
                            int checkTicks = 0;
                            @Override
                            public void run() {
                                if (player.getLocation().distance(target.getLocation()) < 2.2) {
                                    if (player.hasMetadata("IsCharged")) {
                                        performPiercingDischarge(player, target);
                                        player.removeMetadata("IsCharged", JJKPlugin.getInstance());
                                    } else {
                                        JJKDamage.causeAbilityDamage(target, player, 4.0);
                                        world.playSound(target.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 1f, 1.2f);
                                    }
                                    this.cancel();
                                    return;
                                }
                                if (checkTicks > 20) this.cancel();
                                checkTicks++;
                            }
                        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
                        break;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void pullPlayerToTarget(Player player, Location targetLoc) {
        Vector pull = targetLoc.toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.7);
        player.setVelocity(pull.setY(0.4));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.8f);
    }

    private void performPiercingDischarge(Player player, LivingEntity target) {
        World world = player.getWorld();
        // Берем направление взгляда игрока, чтобы линия шла точно по вектору удара
        Vector dir = player.getLocation().getDirection().normalize();

        // Начальная точка — чуть позади цели, чтобы линия "пронзала" её
        Location hitPoint = target.getLocation().add(0, 1.1, 0);
        Location lineStart = hitPoint.clone().subtract(dir.clone().multiply(1.0)); // 1 блок перед целью

        // Звук резкого электрического пробития
        world.playSound(hitPoint, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 2.0f);
        world.playSound(hitPoint, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.5f);
        world.playSound(hitPoint, Sound.ITEM_TRIDENT_HIT, 1.0f, 0.5f);

        // РИСУЕМ ПРЯМУЮ ЛИНИЮ (3 блока длиной)
        // Шаг 0.05 сделает линию практически сплошной, без дырок между партиклами
        for (double d = 0; d <= 3.0; d += 0.05) {
            Location particleLoc = lineStart.clone().add(dir.clone().multiply(d));

            // Сама игла (голубые искры)
            world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);

            // Добавляем эффект "сердечника" (белые точки внутри для яркости)
            if (d % 0.2 == 0) {
                world.spawnParticle(Particle.FIREWORK, particleLoc, 1, 0, 0, 0, 0.02);
            }
        }

        // Вспышка в центре попадания
        world.spawnParticle(Particle.FLASH, hitPoint, 1, 0, 0, 0, 0);

        // Урон и эффекты
        JJKDamage.causeAbilityDamage(target, player, 16.0);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9)); // Паралич

        // Небольшой импульс назад для цели (эффект удара)
        target.setVelocity(dir.multiply(0.5).setY(0.2));
    }
}
