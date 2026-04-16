package abvgd.models.hakari.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Comparator;

public class TrainDoors extends ActiveAbility {

    public TrainDoors() {
        super(new JJKAbilityInfo("Train Doors", Material.IRON_DOOR, 0, 25, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        RayTraceResult result = world.rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(), 10, 0.5,
                e -> e instanceof LivingEntity && e != player);

        if (result == null || result.getHitEntity() == null) return;
        LivingEntity target = (LivingEntity) result.getHitEntity();

        // Центр с небольшим подъемом (1.2 вместо 1.0), чтобы двери были чуть выше
        Location center = target.getLocation().add(0, 1.2, 0);
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        // 1. МГНОВЕННЫЙ УРОН И ЗВУК
        applyImpact(center, player, target);

        // 2. ОТРИСОВКА ДВЕРЕЙ (Они просто стоят зажатыми 0.5 сек, чтобы глаз успел их увидеть)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 10) { // Видны полсекунды
                    this.cancel();
                    return;
                }

                // Рисуем две параллельные двери вплотную к цели (offset 0.4)
                double currentOffset = 0.95;

                drawParallelShutter(center.clone().add(side.clone().multiply(currentOffset)), dir);
                drawParallelShutter(center.clone().subtract(side.clone().multiply(currentOffset)), dir);

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void drawParallelShutter(Location loc, Vector dir) {
        double length = 1.6;
        double height = 2.2;

        Particle.DustOptions ironColor = new Particle.DustOptions(Color.fromRGB(190, 190, 200), 0.9f);
        Particle.DustOptions frameColor = new Particle.DustOptions(Color.fromRGB(45, 45, 50), 1.1f);

        for (double y = -height/2; y <= height/2; y += 0.3) { // Увеличил шаг для производительности
            for (double l = -length/2; l <= length/2; l += 0.3) {
                Location pLoc = loc.clone().add(dir.clone().multiply(l)).add(0, y, 0);

                boolean isEdge = Math.abs(y) > 0.8 || Math.abs(l) > 0.6;
                loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, isEdge ? frameColor : ironColor);
            }
        }
    }

    private void applyImpact(Location loc, Player caster, LivingEntity victim) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.6f);
        world.playSound(loc, Sound.BLOCK_IRON_DOOR_CLOSE, 1.8f, 0.6f);

        world.spawnParticle(Particle.FLASH, loc, 1);
        world.spawnParticle(Particle.CRIT, loc, 35, 0.3, 0.5, 0.3, 0.1);

        JJKDamage.causeAbilityDamage(victim, caster, 12.0);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 255, false, false));
        victim.setVelocity(new Vector(0, 0, 0));
    }
}