package abvgd.models.sukuna;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SukunaDash extends DashAbility {

    private final Color sukunaRed = Color.fromRGB(130, 0, 0);

    public SukunaDash() {
        super(new JJKAbilityInfo(
                "§4§lPhantom Flicker",
                Material.NETHERITE_BOOTS,
                0,
                0,
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();

        // 1. Создаем послеобраз на старте
        spawnAfterimage(startLoc);

        // 2. Баффы скорости и инвиза
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 15, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 4, false, false, false));

        world.playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
        world.playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.2f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline() || t >= 15) {
                    // --- ЭФФЕКТ ВЫХОДА (как в оригинале) ---
                    if (player.isOnline()) {
                        Location endLoc = player.getLocation().add(0, 1, 0);
                        world.playSound(endLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
                        world.spawnParticle(Particle.WHITE_SMOKE, endLoc, 15, 0.2, 0.5, 0.2, 0.05);
                        world.spawnParticle(Particle.SWEEP_ATTACK, endLoc, 1, 0, 0, 0, 0); // Добавил разрез на выходе
                    }
                    this.cancel();
                    return;
                }

                // ФИКС ПРЫЖКА
                if (player.getVelocity().getY() > 0) {
                    player.setVelocity(player.getVelocity().setY(-0.1));
                }

                Vector back = player.getLocation().getDirection().normalize().multiply(-0.5);
                Location trailLoc = player.getLocation().add(back).add(0, 0.8, 0);

                world.spawnParticle(Particle.DUST, trailLoc, 3, 0.1, 0.2, 0.1, new Particle.DustOptions(sukunaRed, 1.7f));
                world.spawnParticle(Particle.SQUID_INK, trailLoc, 1, 0.02, 0.02, 0.02, 0.01);

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void spawnAfterimage(Location loc) {
        World world = loc.getWorld();
        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life > 20) { this.cancel(); return; }
                for (double y = 0; y < 1.8; y += 0.4) {
                    world.spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 4, 0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.BLACK, 1.1f));
                }
                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }
}