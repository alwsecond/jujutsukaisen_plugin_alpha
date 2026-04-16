package abvgd.models.kashimo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ElectricField extends ActiveAbility {

    public ElectricField() {
        super(new JJKAbilityInfo(
                "Electric Field",
                Material.DAYLIGHT_DETECTOR, // Похоже на технический прибор
                40, 60, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation();

        // Звук удара об землю и гул энергии
        world.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.5f);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 2f);

        // Создаем визуальный "купол" из частиц на 4 секунды
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 80 || !player.isOnline()) {
                    world.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 2f);
                    this.cancel();
                    return;
                }

                // Визуал круга на земле (кольцо из искр)
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
                    double x = Math.cos(i) * 5;
                    double z = Math.sin(i) * 5;
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);
                    if (ticks % 20 == 0) world.spawnParticle(Particle.GLOW, particleLoc, 1, 0, 0, 0, 0.05);
                }

                // Логика урона и замедления
                if (ticks % 15 == 0) { // Каждые полсекунды
                    for (Entity e : world.getNearbyEntities(center, 5, 3, 5)) {
                        if (e instanceof LivingEntity target && e != player) {
                            // Наносим небольшой урон через JJKDamage
                            JJKDamage.causeAbilityDamage(target, player, 2.0);

                            // Электрический шок (замедление и "тряска")
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2));
                            world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                            world.playSound(target.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 0.5f, 1.8f);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
