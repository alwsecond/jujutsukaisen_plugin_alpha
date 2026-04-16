package abvgd.models.gojo.ability;

import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKFunc;

import java.util.HashSet;
import java.util.Set;

public class Red extends ActiveAbility {

    public Red() {
        super(new JJKAbilityInfo(
                "§c§lRed",
                Material.RED_STAINED_GLASS,
                220,
                200,
                0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        Location startLoc = player.getEyeLocation();
        JJKFunc.freezePlayer(player, 15);

        player.getWorld().playSound(startLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);
        player.getWorld().playSound(startLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.5f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 15) {
                    launchRedProjectile(player);
                    this.cancel();
                    return;
                }

                Location finger = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5));
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = 1.5 - (t * 0.1);
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    Location pLoc = finger.clone().add(x, (Math.random() - 0.5) * 2, z);
                    Vector dir = finger.toVector().subtract(pLoc.toVector()).normalize().multiply(0.2);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.0f));
                    player.getWorld().spawnParticle(Particle.END_ROD, pLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.1);
                }
                player.getWorld().spawnParticle(Particle.DUST, finger, 5, 0.05, 0.05, 0.05, new Particle.DustOptions(Color.MAROON, 2.0f));
                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void launchRedProjectile(Player player) {
        final Location currentLoc = player.getEyeLocation().add(player.getLocation().getDirection());
        final Vector shootDirection = player.getLocation().getDirection().normalize();
        final Vector velocity = shootDirection.clone().multiply(2.2);
        final Set<LivingEntity> capturedEntities = new HashSet<>(); // Храним тех, кого «тащим»

        final boolean[] exploded = {false};

        player.getWorld().playSound(currentLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.2f);
        player.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20;

            @Override
            public void run() {
                if (exploded[0] || ticks >= maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                RayTraceResult ray = currentLoc.getWorld().rayTraceBlocks(currentLoc, shootDirection, velocity.length(), FluidCollisionMode.NEVER, true);

                if (ray != null && ray.getHitBlock() != null) {
                    currentLoc.add(shootDirection.clone().multiply(ray.getHitPosition().distance(currentLoc.toVector())));
                    exploded[0] = true;

                    // Эффект впечатывания: при взрыве наносим доп. урон тем, кого притащили к стене
                    for (LivingEntity victim : capturedEntities) {
                        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
                        JJKDamage.causeAbilityDamage(victim, player, 10.0); // Бонусный урон за удар об стену
                    }

                    explodeRed(currentLoc, player, shootDirection);
                    this.cancel();
                    return;
                }

                currentLoc.add(velocity);
                ticks++;

                // Визуал
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 15, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.RED, 2.0f));
                if (ticks % 3 == 0) currentLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, currentLoc, 1, 0, 0, 0, 0);

                // Обработка сущностей
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 2.5, 2.5, 2.5)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player)) {

                        // Эффект «впечатывания»: постоянно сетаем вектору скорость снаряда
                        // Добавляем небольшой подъем (0.1), чтобы цель не цеплялась за пол при полете
                        victim.setVelocity(velocity.clone().add(new Vector(0, 0.05, 0)));

                        if (!capturedEntities.contains(victim)) {
                            JJKDamage.causeAbilityDamage(victim, player, 13.0);
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 49, false, false));
                            PlayerManager.get(player).addMastery(1);
                            capturedEntities.add(victim);
                            victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 3);
                        }
                    }
                }
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void explodeRed(Location loc, Player shooter, Vector impactDir) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 10, 0.5, 0.5, 0.5, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 2.0f, 0.8f);

        // Разрушение блоков
        JJKFunc.fxDestruction(loc, 3, 0.4, 0.7);

        // Урон и откидывание
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 6.0, 6.0, 6.0)) {
            if (entity instanceof LivingEntity victim && !entity.equals(shooter)) {

                // ПРОСТО УРОН
                JJKDamage.causeAbilityDamage(victim, shooter, 20.0);
                PlayerManager.get(shooter).addMastery(6);

                victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30, 1));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5));

                // Откидывание
                Vector blastDir = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
                Vector finalPush = impactDir.clone().add(blastDir).normalize().multiply(4.0);
                victim.setVelocity(finalPush);
            }
        }
    }
}