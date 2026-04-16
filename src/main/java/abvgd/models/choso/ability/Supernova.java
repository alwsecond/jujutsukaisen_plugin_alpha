package abvgd.models.choso.ability;

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

import java.util.ArrayList;
import java.util.List;

public class Supernova extends ActiveAbility {

    public Supernova() {
        super(new JJKAbilityInfo(
                "Supernova",
                Material.RED_CANDLE,
                50, 100, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location center = player.getEyeLocation();

        // 1. СОЗДАЕМ МИНИ-СФЕРЫ КРОВИ
        List<BlockDisplay> orbs = new ArrayList<>();
        for (int i = 0; i < 10; i++) { // Увеличил до 10 для густоты
            double angle = i * (Math.PI * 2 / 10);
            Location spawnLoc = center.clone().add(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);

            BlockDisplay orb = world.spawn(spawnLoc, BlockDisplay.class, b -> {
                b.setBlock(Material.REDSTONE_BLOCK.createBlockData());
                Transformation t = b.getTransformation();
                t.getScale().set(0.15f, 0.15f, 0.15f); // Совсем крошечные сферы
                b.setTransformation(t);
                b.setInterpolationDuration(5);
            });
            orbs.add(orb);
        }

        world.playSound(center, Sound.ITEM_BOTTLE_FILL, 1f, 0.8f);

        // 2. БЫСТРЫЙ ХАОТИЧНЫЙ ВЗРЫВ
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks == 10) {
                    // Рандомный взрыв каждой сферы
                    for (BlockDisplay orb : orbs) {
                        // Небольшая задержка перед разлетом каждой сферы для хаоса
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                triggerChaoticExplosion(player, orb);
                            }
                        }.runTaskLater(JJKPlugin.getInstance(), (long) (Math.random() * 5));
                    }
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void triggerChaoticExplosion(Player player, BlockDisplay orb) {
        World world = orb.getWorld();
        Location loc = orb.getLocation();
        orb.remove();

        // Звук ускоренного взрыва фейерверка
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.4f, 1.5f + (float)Math.random());
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.2f, 0.5f);

        // Эффект взрыва в точке
        world.spawnParticle(Particle.BLOCK, loc, 20, 0.1, 0.1, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        world.spawnParticle(Particle.FLASH, loc, 1);

        // УРОН В РАДИУСЕ 8 БЛОКОВ
        for (Entity e : world.getNearbyEntities(loc, 8, 4, 8)) {
            if (e instanceof LivingEntity target && e != player) {
                // Прямой урон + эффект кровотечения
                JJKDamage.causeAbilityDamage(target, player, 7.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));

                // Визуал попадания на цели
                world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, Material.NETHER_WART_BLOCK.createBlockData());

                // Откидывание от центра взрыва
                Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2).setY(0.3);
                target.setVelocity(push);
            }
        }
    }
}
