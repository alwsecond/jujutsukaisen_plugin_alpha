package abvgd.models.toji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class InvisibleThreat extends ActiveAbility {

    public InvisibleThreat() {
        super(new JJKAbilityInfo(
                "§8§lInvisible Threat",
                Material.FLINT,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.5f);

        new BukkitRunnable() {
            int dashCount = 0;

            @Override
            public void run() {
                // --- ФИНАЛЬНЫЙ РЫВОК ЗА СПИНУ ---
                if (dashCount >= 10 || !player.isOnline()) {
                    finishBehindTarget(player);
                    this.cancel();
                    return;
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 8, 2, false, false, true));
                // --- ПРОМЕЖУТОЧНЫЕ ХАОТИЧНЫЕ РЫВКИ ---
                Location start = player.getLocation();
                double angle = Math.random() * Math.PI * 2;
                Vector randomDir = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(4);

                RayTraceResult ray = world.rayTraceBlocks(player.getEyeLocation(), randomDir, 4, FluidCollisionMode.NEVER, true);
                Location target = (ray != null && ray.getHitBlock() != null)
                        ? ray.getHitPosition().toLocation(world).subtract(randomDir.clone().normalize().multiply(0.5))
                        : player.getLocation().add(randomDir);

                spawnQuickGhost(start);
                world.spawnParticle(Particle.CLOUD, start.add(0, 0.5, 0), 5, 0.2, 0.1, 0.2, 0.05);
                world.playSound(start, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8f, 2.0f);

                player.teleport(target);
                dashCount++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 2L);
    }

    private void finishBehindTarget(Player player) {
        World world = player.getWorld();
        // Ищем ближайшую жертву в радиусе 15 блоков
        LivingEntity victim = player.getNearbyEntities(15, 15, 15).stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(player))
                .map(e -> (LivingEntity) e)
                .findFirst().orElse(null);

        if (victim != null) {
            // 1. ПОЗИЦИОНИРОВАНИЕ
            Vector behindDir = victim.getLocation().getDirection().normalize().multiply(-1.2);
            Location behindLoc = victim.getLocation().add(behindDir);
            behindLoc.setDirection(victim.getLocation().toVector().subtract(behindLoc.toVector()).normalize());

            spawnQuickGhost(player.getLocation());
            player.teleport(behindLoc);

            // 2. УРОН И ОТТАЛКИВАНИЕ (в спину)
            // Наносим урон через твой JJKDamage (6.0 = 3 сердца)
            JJKDamage.causeAbilityDamage(victim, player, 6.0);

            // Толкаем жертву ВПЕРЕД (в ту сторону, куда она смотрела)
            Vector push = victim.getLocation().getDirection().normalize().multiply(0.8).setY(0.2);
            victim.setVelocity(push);

            // 3. ЭФФЕКТЫ "УДАРА ТЕНИ"
            world.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
            world.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.8f);

            world.spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.1,
                    Material.REDSTONE_BLOCK.createBlockData()); // Кровавые искры

            player.sendActionBar("§8§lCHECKMATE");
        }
    }

    private void spawnQuickGhost(Location loc) {
        World world = loc.getWorld();
        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life > 6) { this.cancel(); return; }
                for (double y = 0; y < 1.8; y += 0.6) {
                    world.spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 2, 0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.fromRGB(35, 35, 35), 1.1f));
                }
                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
