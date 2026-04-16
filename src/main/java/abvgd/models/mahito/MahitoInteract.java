package abvgd.models.mahito;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MahitoInteract extends InteractAbility {

    public MahitoInteract() {
        super(new JJKAbilityInfo(
                "§d§lSoul Snare",
                Material.ROTTEN_FLESH,
                0, 80, 25.0, // КД 4 сек, 25 энергии
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        // 1. Ищем цель (луч на 8-10 блоков)
        RayTraceResult ray = world.rayTraceEntities(start, direction, 10, 1.5, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {
            // 2. ЗВУКИ: Хлюпанье плоти и натяжение
            world.playSound(player.getLocation(), Sound.ENTITY_SLIME_ATTACK, 1.5f, 0.5f);
            world.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);

            // 3. ВИЗУАЛ: Щупальце из плоти (шлейф из частиц)
            Location vLoc = victim.getLocation().add(0, 1, 0);
            Location pLoc = player.getLocation().add(0, 1, 0);

            // Рисуем линию из частиц плоти от игрока к жертве
            double dist = pLoc.distance(vLoc);
            Vector link = vLoc.toVector().subtract(pLoc.toVector()).normalize();
            for (double i = 0; i < dist; i += 0.5) {
                Location point = pLoc.clone().add(link.clone().multiply(i));
                world.spawnParticle(Particle.BLOCK, point, 3, 0.1, 0.1, 0.1, 0.05, Material.NETHER_WART_BLOCK.createBlockData());
                world.spawnParticle(Particle.SQUID_INK, point, 2, 0.05, 0.05, 0.05, 0.02);
            }

            Vector pull = pLoc.toVector().subtract(vLoc.toVector()).normalize().multiply(1.4);
            pull.setY(0.3); // Немного подбрасываем, чтобы не цеплялся за блоки

            victim.setVelocity(pull);

            // Небольшой стан жертве, чтобы не сразу убежала (Замедление на 1 сек)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, false));

            player.sendActionBar("§d§lПОЙМАН!");
        } else {
            // Если промахнулся — просто звук всплеска плоти
            world.playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_STEP, 1.0f, 0.5f);
        }
    }
}
