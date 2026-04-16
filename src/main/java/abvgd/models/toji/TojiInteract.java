package abvgd.models.toji;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class TojiInteract extends InteractAbility {

    public TojiInteract() {
        super(new JJKAbilityInfo(
                "§8§lTripwire Strike",
                Material.FLINT,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        // Поиск цели в упор (т.к. мы уже за спиной после рывков)
        RayTraceResult ray = world.rayTraceEntities(start, direction, 3.5, 1.2, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {
            Location vLoc = victim.getLocation();

            // 1. ЭФФЕКТ ПОДНОЖКИ (Паралич и падение)
            // Ставим Slowness 255 на полсекунды (эффект того, что ноги подкосились)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 255, false, false, false));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 15, 200, false, false, false));

            // Звуки удара по ногам и хруста
            world.playSound(vLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
            world.playSound(vLoc, Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.8f);

            // 2. УРОН (через твой класс)
            JJKDamage.causeAbilityDamage(victim, player, 5.0);

            // 3. ВИЗУАЛ: Пыль у ног жертвы
            world.spawnParticle(Particle.CLOUD, vLoc, 10, 0.4, 0.1, 0.4, 0.05);
            world.spawnParticle(Particle.SWEEP_ATTACK, vLoc.add(0, 0.2, 0), 1, 0, 0, 0, 0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 2, false, false, true));
            // 4. ПАФОСНЫЙ ОТСКОК ТОДЗИ (на 6 блоков назад)
            // Рассчитываем вектор назад от направления взгляда
            Vector retreatDir = direction.clone().multiply(-1.2).setY(0.5); // Сильный импульс назад и вверх
            player.setVelocity(retreatDir);

            // Звук быстрого перемещения при отскоке
            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.2f);

            // След из пыли при отскоке
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (t > 5 || !player.isOnline()) { this.cancel(); return; }
                    world.spawnParticle(Particle.WHITE_ASH, player.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                    t++;
                }
            }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);

            player.sendActionBar("§8§lDISTANCE CREATED");
        } else {
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }
    }
}
