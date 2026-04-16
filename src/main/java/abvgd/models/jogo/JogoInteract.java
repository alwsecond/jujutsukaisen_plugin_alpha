package abvgd.models.jogo;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class JogoInteract extends InteractAbility {
    public JogoInteract() {
        super(new JJKAbilityInfo("Heat Wave", Material.MAGMA_CREAM, 0, 8, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        world.spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 50, 2, 0.5, 2, 0.2);

        for (Entity e : world.getNearbyEntities(loc, 4, 3, 4)) {
            if (e instanceof LivingEntity target && e != player) {
                target.setFireTicks(20); // Поджигает на 3 сек
                Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.4);
                target.setVelocity(push);

                world.playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
            }
        }
    }
}
