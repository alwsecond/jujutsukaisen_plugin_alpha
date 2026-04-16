package abvgd.models.yuji.ability;

import abvgd.core.types.ActiveAbility;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SoulHit extends ActiveAbility {

    public SoulHit() {
        super(new JJKAbilityInfo(
                "§f§lSoul Seizer",
                Material.LIGHT_BLUE_DYE,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // --- 1. ЗАРЯД БЕЛОГО КУЛАКА ---
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 2.0f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 6 || !player.isOnline()) {
                    executeSoulHit(player);
                    this.cancel();
                    return;
                }

                // Визуал белого кулака
                Location hand = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6));
                Vector side = player.getLocation().getDirection().rotateAroundY(Math.toRadians(-40)).multiply(0.4);
                Location pLoc = hand.add(side).subtract(0, 0.2, 0);

                world.spawnParticle(Particle.END_ROD, pLoc, 2, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.DUST, pLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.WHITE, 1.5f));

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void executeSoulHit(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();

        RayTraceResult ray = world.rayTraceEntities(start, dir, 3.5, 1.2, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {
            // --- ЭФФЕКТЫ УДАРА ---
            // Звук трескающейся души (стекло + камень)
            world.playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 1.8f);
            world.playSound(victim.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.5f);
            world.playSound(victim.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.0f, 1.8f);

            world.spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1, 0), 2, 0, 0, 0, 0);

            // --- МЕХАНИКА ПОВРЕЖДЕНИЯ ДУШИ ---
            AttributeInstance maxHealth = victim.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double originalValue = maxHealth.getBaseValue();
                double reduction = originalValue * 0.15;
                maxHealth.setBaseValue(Math.max(2.0, originalValue - reduction));

                // Свечение и визуальная нестабильность
                victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false));

                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (t >= 100 || !victim.isValid() || !player.isOnline()) {
                            if (victim.isValid()) maxHealth.setBaseValue(originalValue);
                            this.cancel();
                            return;
                        }

                        // Белые партиклы нестабильности (FIREWORK + END_ROD)
                        Location vLoc = victim.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.FIREWORK, vLoc, 2, 0.3, 0.5, 0.3, 0.02);
                        world.spawnParticle(Particle.END_ROD, vLoc, 1, 0.2, 0.4, 0.2, 0.01);

                        if (t % 15 == 0) {
                            world.playSound(vLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.5f);
                        }
                        t++;
                    }
                }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
            }

            JJKDamage.causeAbilityDamage(victim, player, 7.0);
            player.sendActionBar("§f§lSOUL SHATTERED");
        }
    }
}
