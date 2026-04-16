package abvgd.models.yuji;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class YujiDash extends DashAbility {

    public YujiDash() {
        super(new JJKAbilityInfo(
                "§6§lPhysical Burst",
                Material.LEATHER_BOOTS,
                0,
                0, // КД 0 для тестов
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();

        // 1. ВИЗУАЛ СТАРТА (Сила Итадори)
        // Звук тяжелого рывка и крошащегося камня
        world.playSound(start, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.5f);
        world.playSound(start, Sound.ENTITY_HORSE_GALLOP, 1.2f, 0.5f);

        // Частицы пыли и блоков из-под ног
        world.spawnParticle(Particle.BLOCK, start, 25, 0.4, 0.1, 0.4, 0.15,
                start.getBlock().getRelative(0, -1, 0).getType().createBlockData());
        world.spawnParticle(Particle.CLOUD, start, 10, 0.3, 0.1, 0.3, 0.05);

        // 2. ИМПУЛЬС
        player.setVelocity(direction.multiply(1.5).setY(0.20));

        // 3. ЭФФЕКТЫ ВО ВРЕМЯ ПОЛЕТА
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 8 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 0.8, 0);

                // Трейл: На Юдзи фокусируется проклятая энергия (синие искры)
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.CRIT, loc, 3, 0.2, 0.2, 0.2, 0.1);

                // Если Юдзи задевает кого-то — он их просто расталкивает плечом
                player.getNearbyEntities(1.2, 1.2, 1.2).forEach(entity -> {
                    if (entity instanceof LivingEntity victim && !entity.equals(player)) {
                        Vector push = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5);
                        victim.setVelocity(push.setY(0.2));
                    }
                });

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
