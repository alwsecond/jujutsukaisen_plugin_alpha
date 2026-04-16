package abvgd.models.kashimo;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class KashimoInteract extends InteractAbility {

    public KashimoInteract() {
        super(new JJKAbilityInfo(
                "Voltage Overload",
                Material.LIGHTNING_ROD,
                0, 10, 0, false // КД 10 секунд
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // Звук накапливающегося электричества
        world.playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1f, 2f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2f);

        // Даем эффект "Заряжен" (через метадату, чтобы другие способности видели это)
        player.setMetadata("IsCharged", new FixedMetadataValue(JJKPlugin.getInstance(), true));

        // Визуальный эффект вокруг игрока на 3 секунды
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || !player.isOnline()) { // 3 секунды (60 тиков)
                    player.removeMetadata("IsCharged", JJKPlugin.getInstance());
                    this.cancel();
                    return;
                }

                // Голубые искры вокруг тела
                Location loc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 3, 0.4, 0.6, 0.4, 0.1);

                // Если он в этом режиме, он бежит быстрее
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5, 2, false, false));

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
