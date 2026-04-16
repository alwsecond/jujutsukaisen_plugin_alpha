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

public class ElectricalDischarge extends ActiveAbility {

    public ElectricalDischarge() {
        super(new JJKAbilityInfo(
                "Electrical Discharge",
                Material.LIGHTNING_ROD,
                25, 30, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        // 1. СОЗДАЕМ ЭЛЕКТРИЧЕСКИЙ СНАРЯД (Громоотвод)
        BlockDisplay bolt = world.spawn(start, BlockDisplay.class, b -> {
            b.setBlock(Material.LIGHTNING_ROD.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(0.4f, 0.4f, 1.2f); // Тонкий и длинный как игла
            b.setTransformation(t);
            b.setInterpolationDuration(1);
            b.setInterpolationDelay(0);
        });

        world.playSound(start, Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);
        world.playSound(start, Sound.ENTITY_CREEPER_PRIMED, 0.5f, 2f);

        new BukkitRunnable() {
            int ticks = 0;
            Location current = start.clone();

            @Override
            public void run() {
                if (ticks > 40 || !current.getBlock().getType().isAir() || !bolt.isValid()) {
                    trigger(current, player, bolt);
                    this.cancel();
                    return;
                }

                // Прямолинейное быстрое движение
                current.add(direction.clone().multiply(1.5));
                bolt.teleport(current);

                // Вращение снаряда по направлению полета
                float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
                float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
                bolt.setRotation(yaw, pitch);

                // Эффекты искр в полете
                world.spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.1, 0.1, 0.1, 0.2);
                world.spawnParticle(Particle.GLOW, current, 1, 0, 0, 0, 0);

                // Проверка попадания
                for (Entity e : world.getNearbyEntities(current, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity target && e != player) {
                        trigger(current, player, bolt);
                        this.cancel();
                        return;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void trigger(Location loc, Player caster, BlockDisplay bolt) {
        if (bolt != null) bolt.remove();

        World world = loc.getWorld();

        // Звук короткого замыкания и электрического взрыва
        world.playSound(loc, Sound.BLOCK_BEEHIVE_WORK, 2f, 1.2f);
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.5f);
        world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);

        // Визуал: Сферический разряд тока вместо молнии
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 50, 0.5, 0.5, 0.5, 0.3); // Искры во все стороны
        world.spawnParticle(Particle.GLOW, loc, 10, 0.8, 0.8, 0.8, 0.1);           // Мягкое свечение

        for (Entity e : world.getNearbyEntities(loc, 4, 4, 4)) {
            if (e instanceof LivingEntity victim && e != caster) {
                JJKDamage.causeAbilityDamage(victim, caster, 12.0);

                // Эффект "шока": частицы прямо на теле жертвы
                new BukkitRunnable() {
                    int shockTicks = 0;
                    @Override
                    public void run() {
                        if (shockTicks > 10 || victim.isDead()) {
                            this.cancel();
                            return;
                        }
                        // Искры бегающие по телу
                        world.spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);
                        shockTicks++;
                    }
                }.runTaskTimer(JJKPlugin.getInstance(), 0L, 2L);

                // Вместо откидывания — легкое подергивание (стан)
                victim.setVelocity(new Vector(0, 0.1, 0));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9, false, false));
            }
        }
    }
}
