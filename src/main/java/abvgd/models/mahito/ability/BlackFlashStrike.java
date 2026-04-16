package abvgd.models.mahito.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class BlackFlashStrike extends ActiveAbility {

    public BlackFlashStrike() {
        super(new JJKAbilityInfo(
                "Black Flash",
                Material.BLACK_DYE,
                0, 100, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // 1. ПОДГОТОВКА: Кулак загорается фиолетовым и черным
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.1f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 3, false));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Эффект горения руки (1.25 сек или до удара)
                if (ticks > 25 || player.hasMetadata("BlackFlashReady")) {
                    player.removeMetadata("BlackFlashActive", JJKPlugin.getInstance());
                    this.cancel();
                    return;
                }

                // Визуал кулака: Смесь фиолетового огня и черного дыма
                Location hand = getRightHandLocation(player);
                world.spawnParticle(Particle.DUST, hand, 3, 0.05, 0.05, 0.05,
                        new Particle.DustOptions(Color.fromRGB(160, 32, 240), 1.0f)); // Фиолетовый
                world.spawnParticle(Particle.SQUID_INK, hand, 2, 0.02, 0.02, 0.02, 0.01); // Черный
                world.spawnParticle(Particle.WITCH, hand, 1, 0.1, 0.1, 0.1, 0);

                player.setMetadata("BlackFlashActive", new FixedMetadataValue(JJKPlugin.getInstance(), true));
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private Location getRightHandLocation(Player p) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        return loc.add(dir.multiply(0.5)).add(side.multiply(0.4)).subtract(0, 0.3, 0);
    }
}