package abvgd.models.kashimo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ThunderStrike extends ActiveAbility {

    public ThunderStrike() {
        super(new JJKAbilityInfo(
                "Thunder Strike",
                Material.GOLDEN_SWORD,
                35, 60, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // Звук начала зарядки
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 1f, 2f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Если за 2 секунды (40 тиков) никого не коснулись — заряд гаснет
                if (ticks > 40 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // 1. ВИЗУАЛ ЗАРЯЖЕННОЙ РУКИ
                Location hand = getRightHandLocation(player);
                world.spawnParticle(Particle.ELECTRIC_SPARK, hand, 3, 0.05, 0.05, 0.05, 0.2);
                if (ticks % 4 == 0) world.spawnParticle(Particle.FLASH, hand, 1, 0, 0, 0, 0);

                // 2. ПОИСК ЦЕЛИ ВПЛОТНУЮ (радиус 1.5-2 блока)
                for (Entity entity : world.getNearbyEntities(hand, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        // СРАБОТАЛО! Наносим удар
                        triggerImpact(player, target);

                        this.cancel(); // Останавливаем зарядку
                        return;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void triggerImpact(Player attacker, LivingEntity victim) {
        World world = victim.getWorld();
        Location loc = victim.getLocation().add(0, 1, 0);

        // Звуки мощного разряда
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 1.5f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1f, 1.8f);

        // Эффект взрыва из партиклов (очень плотный)
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 0.5, 0.5, 0.5, 0.6);
        world.spawnParticle(Particle.EXPLOSION, loc, 2);
        world.spawnParticle(Particle.FLASH, loc, 3, 0.2, 0.2, 0.2, 0);

        // УРОН
        JJKDamage.causeAbilityDamage(victim, attacker, 15.0);

        // МОЩНОЕ ОТКИДЫВАНИЕ (Repel)
        Vector push = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .normalize()
                .multiply(2.5) // Сила толчка
                .setY(0.7);    // Подброс вверх
        victim.setVelocity(push);

        // Эффект паралича (Slowness 10 на 1.5 сек)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 9));
    }

    private Location getRightHandLocation(Player p) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        return loc.add(dir.multiply(0.7)).add(side.multiply(0.4)).subtract(0, 0.3, 0);
    }
}