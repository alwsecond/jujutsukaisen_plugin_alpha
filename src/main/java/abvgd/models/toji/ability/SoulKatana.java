package abvgd.models.toji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SoulKatana extends ActiveAbility {

    private final Color soulBlack = Color.fromRGB(20, 20, 20);
    private final Color soulPurple = Color.fromRGB(138, 43, 226);

    public SoulKatana() {
        super(new JJKAbilityInfo(
                "Soul Split Katana",
                Material.NETHERITE_SWORD,
                0,
                120, // Кулдаун 6 сек
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        Location center = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();

        // Звук тяжелого разреза
        player.getWorld().playSound(center, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);

        // --- ВИЗУАЛ: ГОРИЗОНТАЛЬНАЯ ДУГА 100 ГРАДУСОВ ---
        renderSoulSweep(player, direction);

        // --- ЛОГИКА УРОНА ---
        double radius = 7.0;
        for (Entity entity : player.getNearbyEntities(radius, 3, radius)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player)) {

                Vector toVictim = victim.getLocation().toVector().subtract(center.toVector()).setY(0).normalize();
                double angle = Math.toDegrees(direction.angle(toVictim));

                if (angle <= 50) { // 50 влево + 50 вправо = 100 градусов

                    // УНИКАЛЬНЫЙ ЭФФЕКТ: Удар по душе (отнимает Макс. ХП на 15 секунд)
                    if (victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
                        double currentMax = victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                        double newMax = Math.max(2.0, currentMax - 4.0); // Отрезает 2 сердца

                        victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(newMax);

                        // Возвращаем ХП через 15 секунд
                        Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), () -> {
                            if (victim.isValid()) {
                                victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(currentMax);
                            }
                        }, 300L);
                    }

                    // Обычный урон
                    victim.setNoDamageTicks(0);
                    JJKDamage.causeAbilityDamage(victim, player, 12.0);

                    // Частицы крови души
                    victim.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 15, 0.2, 0.5, 0.2, new Particle.DustOptions(soulPurple, 1.5f));
                }
            }
        }
    }

    private void renderSoulSweep(Player player, Vector dir) {
        Location eye = player.getEyeLocation().subtract(0, 0.4, 0);
        for (double a = -50; a <= 50; a += 5) {
            double rad = Math.toRadians(a);
            // Поворачиваем вектор направления взгляда
            Vector rotated = dir.clone();
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double x = rotated.getX() * cos - rotated.getZ() * sin;
            double z = rotated.getX() * sin + rotated.getZ() * cos;
            rotated.setX(x).setZ(z);

            Location particleLoc = eye.clone().add(rotated.multiply(4.0));
            player.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, new Particle.DustOptions(soulBlack, 2.0f));
            player.getWorld().spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);

            if (a % 10 == 0) {
                player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(soulPurple, 1.2f));
            }
        }
    }
}
