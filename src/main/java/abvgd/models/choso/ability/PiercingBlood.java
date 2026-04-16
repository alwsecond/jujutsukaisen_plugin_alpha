package abvgd.models.choso.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PiercingBlood extends ActiveAbility {

    public PiercingBlood() {
        super(new JJKAbilityInfo(
                "Piercing Blood",
                Material.NETHER_WART,
                45, 80, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation();

        // Проверяем, активен ли бафф от Interact (Flowing Red Scale)
        int chargeTime = player.hasMetadata("FlowingRedScale") ? 10 : 30;

        JJKFunc.freezePlayer(player, chargeTime);

        world.playSound(startLoc, Sound.ITEM_BOTTLE_FILL, 1f, 0.5f);
        world.playSound(startLoc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1.5f, 0.1f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location hand = getCenterHandLocation(player);

                // Визуал: Кровь стягивается в одну точку (Красная пыль)
                Particle.DustOptions bloodDust = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f);
                world.spawnParticle(Particle.DUST, hand, 8, 0.6, 0.6, 0.6, -0.15, bloodDust);
                world.spawnParticle(Particle.DRIPPING_DRIPSTONE_WATER, hand, 3, 0.2, 0.2, 0.2);

                if (ticks >= chargeTime) {
                    firePiercingBeam(player);
                    this.cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void firePiercingBeam(Player player) {
        World world = player.getWorld();
        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection().normalize();

        // Звук сверхзвукового выстрела жидкости
        world.playSound(origin, Sound.ENTITY_ARROW_SHOOT, 1f, 0.5f);
        world.playSound(origin, Sound.ITEM_TRIDENT_THROW, 1.5f, 1.8f);

        // Луч летит на 30 блоков
        for (double d = 0; d < 30; d += 0.5) {
            Location point = origin.clone().add(direction.clone().multiply(d));

            // Визуал луча: Тонкая, ярко-красная струя
            Particle.DustOptions beamDust = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 0.8f);
            world.spawnParticle(Particle.DUST, point, 3, 0.01, 0.01, 0.01, beamDust);

            // Эффект брызг по пути
            if (d % 2 == 0) {
                world.spawnParticle(Particle.BLOCK, point, 2, 0.05, 0.05, 0.05, Material.REDSTONE_BLOCK.createBlockData());
            }

            // Проверка попадания
            for (Entity entity : world.getNearbyEntities(point, 0.8, 0.8, 0.8)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    // Огромный урон (Пронзающая кровь очень смертоносна)
                    JJKDamage.causeAbilityDamage(target, player, 18.0);

                    // Эффект кровотечения (Иссушение на 3 сек)
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));

                    // Визуал пробития (брызги за спиной)
                    world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 40, 0.3, 0.3, 0.3, Material.NETHER_WART_BLOCK.createBlockData());
                    world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
                    return;
                }
            }
            if (point.getBlock().getType().isSolid()) break;
        }
    }

    private Location getCenterHandLocation(Player p) {
        return p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.8)).subtract(0, 0.2, 0);
    }
}
