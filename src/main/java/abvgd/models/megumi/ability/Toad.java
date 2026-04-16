package abvgd.models.megumi.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Toad extends ActiveAbility {

    public Toad() {
        super(new JJKAbilityInfo(
                "Toad",
                Material.SLIME_BALL,
                0, 10, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // 1. ИЩЕМ ЦЕЛЬ (кого будем ловить языком)
        var ray = world.rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 12, 1.0, e -> e != player);
        if (ray == null || ray.getHitEntity() == null || !(ray.getHitEntity() instanceof LivingEntity victim)) {
            player.sendMessage("§cЖаба не видит цели!");
            return;
        }

        // 2. ПРИЗЫВ ЖАБЫ (рядом с Мегуми)
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(1.5));
        Frog toad = world.spawn(spawnLoc, Frog.class, f -> {
            f.setCustomName("§2Toad");
            f.setMetadata("Summon", new FixedMetadataValue(JJKPlugin.getInstance(), true));
            f.setAI(false); // Зафиксируем её
        });

        world.spawnParticle(Particle.SQUID_INK, spawnLoc, 30, 0.5, 0.2, 0.5, 0.05);
        world.playSound(spawnLoc, Sound.ENTITY_FROG_AMBIENT, 1.5f, 0.5f);

        // 3. АНИМАЦИЯ ЯЗЫКА И ПРИТЯГИВАНИЕ
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!toad.isValid() || !victim.isValid() || ticks > 20) {
                    toad.remove();
                    this.cancel();
                    return;
                }

                // Визуал языка (частицы слизи)
                Location start = toad.getEyeLocation();
                Location end = victim.getEyeLocation();
                Vector step = end.toVector().subtract(start.toVector()).multiply(0.1);

                for (int i = 0; i < 10; i++) {
                    world.spawnParticle(Particle.ITEM_SLIME, start.add(step), 1, 0, 0, 0, 0);
                }

                // В момент "захвата" (на 5-й тик) тянем цель к Мегуми
                if (ticks == 5) {
                    world.playSound(victim.getLocation(), Sound.ENTITY_FROG_EAT, 1f, 1f);

                    // Вычисляем вектор притягивания к Мегуми
                    Vector pull = player.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize();
                    // Притягиваем с небольшим подбросом
                    victim.setVelocity(pull.multiply(2.0).setY(0.4));

                    // Урон и замедление (липкий язык)
                    JJKDamage.causeAbilityDamage(victim, player, 4.0);
                    victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 4));
                }

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
