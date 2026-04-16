package abvgd.models.megumi;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MegumiInteract extends InteractAbility {

    public MegumiInteract() {
        super(new JJKAbilityInfo(
                "Shadow Stealth",
                Material.COAL,
                0, 0, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        int duration = 20; // в тиках конечно же. А как иначе?
        World world = player.getWorld();
        Location lockLoc = player.getLocation(); // Точка, где он "упал" в тень

        // 1. СТАРТ: Резкое погружение
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 2, false, false, false)); // Resistance 3 (уровень 2 в коде)
        JJKFunc.freezePlayer(player, duration); // ну типо шобы не двигался
        // Звуки: Всплеск + Глухой удар
        world.playSound(lockLoc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 0.4f);
        world.playSound(lockLoc, Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.5f);
        world.playSound(lockLoc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.5f);

        // Визуальный всплеск при входе
        world.spawnParticle(Particle.SQUID_INK, lockLoc.clone().add(0, 0.1, 0), 50, 0.5, 0.2, 0.5, 0.1);

        new BukkitRunnable() {
            int ticks = 0;
            final double radius = 2.5;

            @Override
            public void run() {
                if (ticks > duration || !player.isOnline() || player.getLocation().distance(lockLoc) > 1.8) {
                    // ВЫХОД: Всплеск чернил
                    world.spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 0.1, 0), 30, 0.3, 0.5, 0.3, 0.1);
                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.7f, 0.6f);

                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    player.removePotionEffect(PotionEffectType.RESISTANCE);
                    this.cancel();
                    return;
                }

                // 2. ВИЗУАЛ ЛУЖИ (фиксированная на месте нырка)
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * radius;
                    Location shadowLoc = lockLoc.clone().add(Math.cos(angle) * r, 0.05, Math.sin(angle) * r);

                    world.spawnParticle(Particle.SQUID_INK, shadowLoc, 1, 0, 0, 0, 0);

                    if (Math.random() > 0.9) {
                        world.spawnParticle(Particle.SMOKE, shadowLoc, 1, 0, 0.1, 0, 0.02);
                    }
                }

                // 3. ЛОГИКА ВРАГОВ (кто наступил в лужу — замедляется)
                for (Entity entity : world.getNearbyEntities(lockLoc, radius, 2, radius)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !entity.hasMetadata("Summon")) {
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 2, false, false, false));
                        world.spawnParticle(Particle.SQUID_INK, victim.getLocation(), 2, 0.1, 0, 0.1, 0.01);
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 2L);
    }
}