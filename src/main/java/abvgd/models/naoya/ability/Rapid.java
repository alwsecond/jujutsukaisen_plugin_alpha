package abvgd.models.naoya.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;


public class Rapid extends ActiveAbility {

    public Rapid() {
        super(new JJKAbilityInfo(
                "§e§lRapid",
                Material.END_ROD,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();

        // Поиск цели на дистанции 2 блоков
        RayTraceResult ray = world.rayTraceEntities(start, dir, 2, 1.2, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {

            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.8f);

            new BukkitRunnable() {
                int strikes = 0;

                @Override
                public void run() {
                    if (strikes >= 10 || !victim.isValid() || !player.isOnline()) {
                        // --- ФИНАЛЬНЫЙ УДАР (ОТТАЛКИВАНИЕ) ---
                        if (victim.isValid()) {
                            // Сильный импульс от игрока
                            Vector push = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                            victim.setVelocity(push.setY(0.4));

                            // Эффекты финала
                            world.playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                            world.playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 1.0f);
                            world.spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
                            world.spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                        }
                        this.cancel();
                        return;
                    }

                    // --- СЕРИЯ УДАРОВ ---
                    Location vLoc = victim.getLocation().add(0, 1, 0);

                    // Частицы удара в случайных точках тела
                    double ox = (Math.random() - 0.5) * 0.7;
                    double oy = (Math.random() - 0.5) * 1.0;
                    double oz = (Math.random() - 0.5) * 0.7;
                    Location hitPoint = vLoc.clone().add(ox, oy, oz);

                    world.spawnParticle(Particle.SWEEP_ATTACK, hitPoint, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.CRIT, hitPoint, 5, 0.1, 0.1, 0.1, 0.1);

                    // Звук каждого удара с повышением тона
                    world.playSound(hitPoint, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f + (strikes * 0.08f));

                    // Урон
                    victim.setNoDamageTicks(0);
                    JJKDamage.causeAbilityDamage(victim, player, 2);

                    // Микро-фиксация цели, чтобы не улетела раньше времени
                    victim.setVelocity(new Vector(0, 0.05, 0));

                    strikes++;
                }
            }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L); // 1 тик между ударами

            player.sendActionBar("§e§lRAPID STRIKES");
        } else {
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 2.0f);
        }
    }
}
