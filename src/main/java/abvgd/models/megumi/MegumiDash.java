package abvgd.models.megumi;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MegumiDash extends DashAbility {

    public MegumiDash() {
        super(new JJKAbilityInfo(
                "§8§lShadow Step",
                Material.COAL,
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
        Vector dir = startLoc.getDirection().setY(0).normalize();

        // ФИЗИКА (На 10 блоков)
        player.setVelocity(dir.multiply(1.5).setY(0.25));
        player.setFallDistance(0);

        // ЗВУК: Глухой "всплеск" в тени
        world.playSound(startLoc, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.5f);
        world.playSound(startLoc, Sound.WEATHER_RAIN_ABOVE, 0.5f, 0.7f);

        // ТРЕЙЛ: Погружение в тени (длится чуть дольше из-за дистанции)
        for (int i = 0; i <= 8; i++) {
            Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), () -> {
                if (player.isOnline()) {
                    Location trailLoc = player.getLocation().add(0, 0.5, 0);

                    // Основная тень (крупный дым)
                    world.spawnParticle(Particle.LARGE_SMOKE, trailLoc, 10, 0.2, 0.2, 0.2, 0.02);

                    // Чернильные всплески (частицы разрушения черного бетона)
                    world.spawnParticle(Particle.BLOCK, trailLoc, 15, 0.3, 0.3, 0.3, 0.05,
                            Material.BLACK_CONCRETE.createBlockData());

                    // Эффект "пустоты" (частицы портала, но темные)
                    world.spawnParticle(Particle.REVERSE_PORTAL, trailLoc, 5, 0.1, 0.1, 0.1, 0.1);

                    // Звук легкого шелеста теней в пути
                    if (Bukkit.getCurrentTick() % 2 == 0) {
                        world.playSound(trailLoc, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.5f);
                    }
                }
            }, i);
        }
    }
}
