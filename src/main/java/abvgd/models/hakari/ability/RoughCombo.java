package abvgd.models.hakari.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;

import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Comparator;

public class RoughCombo extends ActiveAbility {

    public RoughCombo() {
        super(new JJKAbilityInfo("Rough Combo",
                Material.LIME_DYE,
                0,
                40,
                0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        // Ищем цель строго перед собой (угол обзора около 90 градусов)
        LivingEntity target = getTargetInFront(player, 6);

        if (target == null) return;

        new BukkitRunnable() {
            int punch = 0;

            @Override
            public void run() {
                if (punch >= 3 || !target.isValid() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // 1. ПОВОРОТ К ЦЕЛИ (чтобы не бить спиной)
                Location pLoc = player.getLocation();
                Location tLoc = target.getLocation();
                Vector directionToTarget = tLoc.toVector().subtract(pLoc.toVector()).normalize();

                // Плавно корректируем взгляд игрока на цель
                Location newLook = player.getLocation().setDirection(directionToTarget);
                player.teleport(newLook);

                // 2. СБЛИЖЕНИЕ
                Vector dash = directionToTarget.clone().multiply(0.5);
                if (pLoc.distance(tLoc) > 1.5) {
                    player.setVelocity(dash.setY(0.1));
                }

                // 3. УДАР И ВИЗУАЛ
                Location hitPoint = target.getLocation().add(0, 1.2, 0);
                drawRoughEffect(hitPoint, world);

                world.playSound(hitPoint, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 1.6f + (punch * 0.1f));
                world.playSound(hitPoint, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.1f, 1.1f);

                double damage = (punch == 2) ? 9.0 : 4.5;
                JJKDamage.causeAbilityDamage(target, player, damage);

                if (punch == 2) {
                    Vector kick = directionToTarget.clone().setY(0).normalize().multiply(1.5).setY(0.4);
                    target.setVelocity(kick);
                    world.spawnParticle(Particle.FLASH, hitPoint, 1);
                }

                punch++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 5L);
    }

    // --- УЛУЧШЕННЫЙ ПОИСК ЦЕЛИ ПЕРЕД СОБОЙ ---
    private LivingEntity getTargetInFront(Player player, double range) {
        LivingEntity bestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        Vector direction = player.getLocation().getDirection().normalize();

        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity victim && e != player) {
                // Вектор от игрока к мобу
                Vector toEntity = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

                // Вычисляем косинус угла между взглядом и мобом (Скалярное произведение)
                // 1.0 = смотрят точно в цель, 0.0 = перпендикулярно, -1.0 = спиной
                double dot = direction.dot(toEntity);

                if (dot > 0.6) { // Примерно 50-60 градусов перед игроком
                    double dist = player.getLocation().distanceSquared(victim.getLocation());
                    if (dist < closestDistance) {
                        closestDistance = dist;
                        bestTarget = victim;
                    }
                }
            }
        }
        return bestTarget;
    }

    private void drawRoughEffect(Location loc, World world) {
        world.spawnParticle(Particle.DUST, loc, 15, 0.2, 0.3, 0.2,
                new Particle.DustOptions(Color.fromRGB(173, 255, 47), 1.3f));
        world.spawnParticle(Particle.SCRAPE, loc, 8, 0.2, 0.2, 0.2, 0.1);
    }
}