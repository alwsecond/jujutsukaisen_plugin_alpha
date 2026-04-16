package abvgd.models.mahito.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class BodyRepel extends ActiveAbility {

    public BodyRepel() {
        super(new JJKAbilityInfo(
                "Body Repel",
                Material.NETHER_WART,
                0, 20, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        // 1. СОЗДАЕМ "ГОЛОВУ" ЧЕРВЯ
        BlockDisplay wormHead = world.spawn(start, BlockDisplay.class, b -> {
            b.setBlock(Material.NETHER_WART_BLOCK.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(0.6f, 0.6f, 0.8f); // Удлиненная форма
            b.setTransformation(t);
            b.setInterpolationDuration(1);
            b.setInterpolationDelay(0);
        });

        world.playSound(start, Sound.ENTITY_SLIME_JUMP, 1f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            Location current = start.clone();

            @Override
            public void run() {
                // Если врезался или улетел далеко
                if (ticks > 30 || !current.getBlock().getType().isAir() || !wormHead.isValid()) {
                    explode(current, player, wormHead);
                    this.cancel();
                    return;
                }

                // 2. ДВИЖЕНИЕ И ИЗВИВАНИЕ
                // Добавляем небольшое колебание (как будто червь ползет в воздухе)
                double wave = Math.sin(ticks * 0.5) * 0.2;
                current.add(direction.clone().multiply(1.2)).add(0, wave, 0);

                wormHead.teleport(current);

                // Вращаем голову, чтобы она смотрела по направлению полета
                float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
                float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
                wormHead.setRotation(yaw, pitch);

                // Хвост из частиц
                world.spawnParticle(Particle.BLOCK, current, 3, 0.1, 0.1, 0.1, Material.NETHER_WART_BLOCK.createBlockData());
                world.spawnParticle(Particle.SQUID_INK, current, 2, 0.05, 0.05, 0.05, 0.01);

                // Проверка попадания
                for (Entity e : world.getNearbyEntities(current, 1.05, 1.05, 1.05)) {
                    if (e instanceof LivingEntity target && e != player && !e.hasMetadata("MahitoClone")) {
                        explode(current, player, wormHead);
                        this.cancel();
                        return;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void explode(Location loc, Player caster, BlockDisplay head) {
        if (head != null) head.remove();

        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);

        // Взрыв плоти
        world.spawnParticle(Particle.BLOCK, loc, 80, 1, 1, 1, Material.NETHER_WART_BLOCK.createBlockData());
        world.spawnParticle(Particle.EXPLOSION, loc, 2);

        for (Entity e : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (e instanceof LivingEntity victim && e != caster && !e.hasMetadata("MahitoClone")) {
                JJKDamage.causeAbilityDamage(victim, caster, 14.0);

                // Сильное отталкивание (Repel)
                Vector push = victim.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.0).setY(0.6);
                victim.setVelocity(push);

                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA, 100, 1));
            }
        }
    }
}
