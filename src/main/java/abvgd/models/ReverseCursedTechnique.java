package abvgd.models;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ReverseCursedTechnique extends ActiveAbility {

    public ReverseCursedTechnique() {
        super(new JJKAbilityInfo(
                "RCT: Healing",
                Material.WHITE_DYE,
                500, 600, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
        world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline() || t > 10) {
                    this.cancel();
                    return;
                }

                // 2. АККУРАТНЫЕ ЭФФЕКТЫ (Белые искры и дымка)
                Location pLoc = player.getLocation().add(0, 1, 0);

                // Только белые частицы (END_ROD и CLOUD)
                world.spawnParticle(Particle.END_ROD, pLoc, 3, 0.4, 0.6, 0.4, 0.01);
                if (t % 2 == 0) {
                    world.spawnParticle(Particle.CLOUD, pLoc, 2, 0.3, 0.5, 0.3, 0.01);
                }

                // 3. МОМЕНТ ИСЦЕЛЕНИЯ (на середине анимации)
                if (t == 5) {
                    double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    double currentHealth = player.getHealth();
                    double healAmount = maxHealth * 0.4;

                    player.setHealth(Math.min(maxHealth, currentHealth + healAmount));

                    // ЗВУК УСПЕШНОГО ИСЦЕЛЕНИЯ (Чистый и звонкий)
                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 2.0f);
                    world.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 0.8f, 2.0f);

                    player.sendMessage("§f§lRCT §8» §aИсцелено");
                }

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }
}
