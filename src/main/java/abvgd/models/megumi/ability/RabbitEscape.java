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

import java.util.ArrayList;
import java.util.List;

public class RabbitEscape extends ActiveAbility {

    public RabbitEscape() {
        super(new JJKAbilityInfo(
                "Rabbit Escape",
                Material.RABBIT_FOOT,
                0, 10, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        List<Rabbit> rabbits = new ArrayList<>();

        // 1. ПРИЗЫВ ПЛОТНОЙ ГРУППЫ
        for (int i = 0; i < 45; i++) {
            Location spawnLoc = startLoc.clone().add((Math.random()-0.5)*3, 0, (Math.random()-0.5)*3);
            Rabbit rabbit = world.spawn(spawnLoc, Rabbit.class, r -> {
                r.setMetadata("Summon", new FixedMetadataValue(JJKPlugin.getInstance(), true));
                r.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.6);
                r.setRabbitType(Rabbit.Type.WHITE);
                r.setSilent(true); // Чтобы 40 кроликов не оглушили игрока звуком
            });
            rabbits.add(rabbit);
        }

        world.playSound(startLoc, Sound.ENTITY_RABBIT_JUMP, 1f, 1.2f);

        // 2. ЦИКЛ НАПРАВЛЕННОЙ ВОЛНЫ
        new BukkitRunnable() {
            int ticks = 0;
            final double maxDist = 10.0; // Дистанция в 10 блоков

            @Override
            public void run() {
                // Если прошли 10 блоков (скорость 0.5 * 20 тиков) или вышло время
                if (ticks > 25 || !player.isOnline()) {
                    for (Rabbit r : rabbits) if (r.isValid()) r.remove();
                    this.cancel();
                    return;
                }

                for (Rabbit r : rabbits) {
                    if (!r.isValid()) continue;

                    // Движение строго вперед без подбрасывания
                    r.setVelocity(direction.clone().multiply(0.6).setY(-0.5));

                    // Проверка столкновений
                    for (Entity e : r.getNearbyEntities(1.0, 1.0, 1.0)) {
                        if (e instanceof LivingEntity victim && e != player && !e.hasMetadata("Summon")) {

                            // СТАНИМ ЦЕЛЬ (Медлительность 10 полностью останавливает)
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5, 10, false, false));
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 1, false, false));

                            // УРОН БЕЗ ОТБРОСА
                            if (ticks % 4 == 0) {
                                JJKDamage.causeAbilityDamage(victim, player, 1.2);
                                // Визуал пыли под ногами
                                world.spawnParticle(Particle.CLOUD, victim.getLocation(), 2, 0.2, 0.1, 0.2, 0.01);
                            }

                            // Гасим инерцию жертвы, чтобы она не улетала
                            victim.setVelocity(new Vector(0, -0.1, 0));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
