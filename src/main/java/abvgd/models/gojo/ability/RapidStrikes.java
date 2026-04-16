package abvgd.models.gojo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RapidStrikes extends ActiveAbility {

    public RapidStrikes() {
        super(new JJKAbilityInfo(
                "§7§lRapid Infinity Strikes",
                Material.IRON_NUGGET,
                0,      // energyCost
                10,     // cooldownTicks (для теста)
                0,      // mastery
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        Location startLoc = player.getLocation();
        World world = player.getWorld();

        // 1. ФИКСАЦИЯ И ПОДГОТОВКА
        // Годжо замирает на мгновение для серии ударов
        JJKFunc.freezePlayer(player, 10);
        world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.5f, 1.5f);

        new BukkitRunnable() {
            int strikes = 0;
            final int maxStrikes = 6; // Количество ударов

            @Override
            public void run() {
                if (strikes >= maxStrikes || !player.isOnline()) {
                    // Финальный мощный удар
                    world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                    this.cancel();
                    return;
                }

                // 2. ПОИСК ЦЕЛИ ПЕРЕД СОБОЙ
                Vector direction = player.getLocation().getDirection();
                Location punchLoc = player.getEyeLocation().add(direction.multiply(2.0));

                // 3. ВИЗУАЛ УДАРА (Пар и Криты)
                world.spawnParticle(Particle.CLOUD, punchLoc, 10, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.CRIT, punchLoc, 15, 0.3, 0.3, 0.3, 0.2);
                world.spawnParticle(Particle.SWEEP_ATTACK, punchLoc, 1, 0, 0, 0, 0);

                // 4. ЗВУКОВОЙ ДИЗАЙН (Те самые "плевки" и удары)
                world.playSound(punchLoc, Sound.ENTITY_LLAMA_SPIT, 1.2f, 1.5f + (strikes * 0.1f));
                world.playSound(punchLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f + (strikes * 0.2f));
                if (strikes % 2 == 0) world.playSound(punchLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.2f);

                // 5. УРОН ПО ОБЛАСТИ
                for (Entity entity : world.getNearbyEntities(punchLoc, 2.0, 2.0, 2.0)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !entity.hasMetadata("Summon")) {

                        // Легкое подкидывание при каждом ударе, чтобы цель "болталась" в воздухе
                        victim.setNoDamageTicks(0);
                        victim.setVelocity(new Vector(0, 0.1, 0));
                        JJKDamage.causeAbilityDamage(victim, player, 2.0);

                        // Эффект крови/частиц на жертве
                        world.spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1,
                                Material.REDSTONE_BLOCK.createBlockData());
                    }
                }

                strikes++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 2L); // Удары каждые 2 тика (очень быстро)
    }
}
