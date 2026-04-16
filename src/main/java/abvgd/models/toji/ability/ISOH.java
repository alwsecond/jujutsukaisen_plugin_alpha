package abvgd.models.toji.ability;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class ISOH extends ActiveAbility {

    public ISOH() {
        super(new JJKAbilityInfo(
                "Inverted Spear of Heaven",
                Material.IRON_SWORD, // Рекомендую CustomModelData
                0,    // Не тратит ПЭ
                160,  // Кулдаун 8 сек
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection();
        // 5,5 blocks
        RayTraceResult ray = world.rayTraceEntities(player.getEyeLocation(), dir, 5.5, 0.5, (e) -> !e.equals(player));

        // Визуал взмаха (Серый/Стальной)
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        Location effectLoc = player.getEyeLocation().add(dir.multiply(2));
        world.spawnParticle(Particle.DUST, effectLoc, 10, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.GRAY, 1.5f));

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {
            // 1. ЗВУК РАЗБИТОГО СТЕКЛА (Развеивание техники)
            world.playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            world.playSound(victim.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);

            // 2. ЭФФЕКТЫ
            world.spawnParticle(Particle.FLASH, victim.getEyeLocation(), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.CRIT, victim.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.2);

            // 3. НАЛОЖЕНИЕ ВЫГОРАНИЯ (10 секунд = 200 тиков)
            if (victim instanceof Player victimPlayer) {
                JJKPlayer jjkVictim = PlayerManager.get(victimPlayer);
                if (jjkVictim != null) {
                    jjkVictim.setBurnout(200);
                    victimPlayer.sendMessage("§c§l[!] §fНебесное копье развеяло вашу технику!");
                }
            }

            // 4. УРОН
            victim.setNoDamageTicks(0);
            JJKDamage.causeAbilityDamage(victim, player, 10.0);
        }
    }
}
