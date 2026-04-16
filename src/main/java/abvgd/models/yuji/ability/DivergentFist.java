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

public class DivergentFist extends ActiveAbility {

    private final Color yujiBlue = Color.fromRGB(0, 191, 255);

    public DivergentFist() {
        super(new JJKAbilityInfo(
                "§b§lDivergent Fist",
                Material.PRISMARINE_SHARD,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // --- 1. СТАДИЯ ЗАРЯДКИ (Шлейф за рукой) ---
        // Звук накопления энергии (втягивание воздуха)
        world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.8f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 5 || !player.isOnline()) {
                    executeStrike(player); // Переход к самому удару
                    this.cancel();
                    return;
                }

                // Голубой шлейф за правой рукой (примерное смещение)
                Location hand = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
                Vector side = player.getLocation().getDirection().rotateAroundY(Math.toRadians(-40)).multiply(0.4);
                Location particleLoc = hand.add(side).subtract(0, 0.3, 0);

                world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.05, 0.05, 0.05, 0.02);
                world.spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(yujiBlue, 1.2f));

                if (t % 2 == 0) world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void executeStrike(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        RayTraceResult ray = world.rayTraceEntities(start, direction, 4, 1.0, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {

            // --- ПЕРВЫЙ УДАР (КОНТАКТ) ---
            world.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.6f);
            world.spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.2);

            JJKDamage.causeAbilityDamage(victim, player, 5.0);

            // --- ВТОРОЙ УДАР (РЕЗОНАНС) ---
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isValid()) return;

                    // Звук "схлопывания"
                    world.playSound(victim.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.8f, 1.6f);
                    world.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.8f);

                    Location vLoc = victim.getLocation().add(0, 1, 0);

                    // Эпичные голубые эффекты
                    world.spawnParticle(Particle.SOUL, vLoc, 8, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, vLoc, 25, 0.5, 0.5, 0.5, 0.03);
                    world.spawnParticle(Particle.FLASH, vLoc, 1, 0, 0, 0, 0); // Вспышка в центре

                    JJKDamage.causeAbilityDamage(victim, player, 9.0);

                    // Толчок проклятой энергией
                    victim.setVelocity(direction.clone().multiply(1.0).setY(0.3));

                    player.sendActionBar("§b§lDIVERGENT IMPACT!");
                }
            }.runTaskLater(JJKPlugin.getInstance(), 10L);

        } else {
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }
    }
}
