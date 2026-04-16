package abvgd.models.mahito;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MahitoDash extends DashAbility {

    public MahitoDash() {
        super(new JJKAbilityInfo(
                "Instant Morph",
                Material.SLIME_BALL,
                0, 15, 0, false // Короткое КД для динамики
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();

        // Звук изменения формы (хлюпанье и натяжение)
        world.playSound(start, Sound.ENTITY_SLIME_JUMP, 1f, 0.5f);
        world.playSound(start, Sound.ENTITY_ELDER_GUARDIAN_FLOP, 0.5f, 0.5f);

        // Делаем игрока "невидимым" на мгновение (визуально превращаем в сгусток)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, false, false));

        // Мощный направленный импульс
        player.setVelocity(direction.multiply(1.5).setY(0.25));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 8) {
                    // Эффект появления
                    world.spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, Material.NETHER_WART_BLOCK.createBlockData());
                    world.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1f, 1f);
                    this.cancel();
                    return;
                }

                // Визуал во время рывка: шлейф из частиц плоти и чернил
                world.spawnParticle(Particle.BLOCK, player.getLocation().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, Material.NETHER_WART_BLOCK.createBlockData());
                world.spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 0.5, 0), 5, 0.1, 0.1, 0.1, 0.02);

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
