package abvgd.utils;

import abvgd.manage.JJKPlayer;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class BlackFlashMechanic {

    private static final Color BLACK = Color.fromRGB(0, 0, 0);
    private static final Color RED = Color.fromRGB(255, 0, 0);

    public static void trigger(EntityDamageByEntityEvent event, Player attacker, JJKPlayer jjkPlayer) {
        LivingEntity victim = (LivingEntity) event.getEntity();
        World world = victim.getWorld();
        Location loc = victim.getLocation().add(0, 1, 0);

        double newDamage = event.getDamage() * 4.0;
        event.setDamage(newDamage);

        // 2. УСИЛЕННОЕ ОТБРАСЫВАНИЕ
        Vector launchVec = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .normalize()
                .multiply(6.0)
                .setY(0.8);
        victim.setVelocity(launchVec);

        // 3. ВОСПОЛНЕНИЕ ЭНЕРГИИ (7% от максимума)
        if (jjkPlayer.getModel() != null) {
            double regen = jjkPlayer.getModel().getMaxEnergy() * 0.07;
            jjkPlayer.addEnergy(regen);
            jjkPlayer.addMastery(3);
        }

        // 4. ЗВУКИ И ЭФФЕКТЫ (оставляем)
        world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f);

        for (int i = 0; i < 12; i++) {
            Vector v = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(2.0);
            drawLightningBranch(loc, v);
        }
        world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
    }

    private static void drawLightningBranch(Location start, Vector direction) {
        Particle.DustOptions black = new Particle.DustOptions(BLACK, 2.5f);
        Particle.DustOptions red = new Particle.DustOptions(RED, 2.0f);
        for (double d = 0; d < 3.0; d += 0.2) {
            Location p = start.clone().add(direction.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, (Math.random() > 0.4 ? black : red));
        }
    }
}
