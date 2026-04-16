package abvgd.models.naoya.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

public class VerticalProjection extends ActiveAbility {

    private final Color frameColor = Color.fromRGB(255, 255, 255);

    public VerticalProjection() {
        super(new JJKAbilityInfo(
                "§e§lVertical Projection",
                Material.FEATHER,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection();

        RayTraceResult ray = world.rayTraceEntities(player.getEyeLocation(), direction, 12, 1.2, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {

            // 1. СТАРТ: ВЗРЫВ СКОРОСТИ
            spawnGhostFrame(startLoc, 3); // Тройной послеобраз
            world.playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5f, 2.0f);
            world.playSound(startLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);

            victim.setVelocity(new Vector(0, 1.6, 0));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255, false, false));

            // 2. ТЕЛЕПОРТАЦИЯ (ПЕРЕХВАТ)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isValid() || !player.isOnline()) return;

                    Location topLoc = victim.getLocation().add(player.getLocation().getDirection().multiply(-2.0)).add(0, 3.5, 0);
                    topLoc.setDirection(victim.getLocation().subtract(topLoc).toVector());

                    // Эффект "Схлопывания" кадра перед ТП
                    world.spawnParticle(Particle.INSTANT_EFFECT, player.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.5);

                    player.teleport(topLoc);

                    // Эффект "Проявления" после ТП
                    world.spawnParticle(Particle.FLASH, topLoc.clone().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.WHITE_ASH, topLoc, 30, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(topLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.5f);
                    world.playSound(topLoc, Sound.BLOCK_GLASS_PLACE, 2.0f, 2.0f);

                    // 3. ФИНАЛЬНЫЙ УДАР (ДИАГОНАЛЬНЫЙ КРАШ)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!victim.isValid() || !player.isOnline()) return;

                            Vector smashVector = player.getLocation().getDirection().normalize().multiply(2.0).setY(-3.5);
                            victim.setVelocity(smashVector);

                            // ЗВУКОВОЙ ТЕРРОР
                            world.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 1.8f);
                            world.playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                            world.playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);

                            // ВИЗУАЛ (МАКСИМАЛЬНЫЙ ЭПИК)
                            Location vLoc = victim.getLocation().add(0, 1, 0);
                            world.spawnParticle(Particle.EGG_CRACK, vLoc, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.BLOCK, vLoc, 80, 0.5, 0.5, 0.5, 0.2, Material.WHITE_STAINED_GLASS.createBlockData());
                            world.spawnParticle(Particle.SWEEP_ATTACK, vLoc, 5, 0.5, 0.5, 0.5, 0);

                            JJKDamage.causeAbilityDamage(victim, player, 16.0);

                            // ДРИФТ-ОТЛЕТ
                            Vector recoil = player.getLocation().getDirection().multiply(-1.5).setY(0.6);
                            player.setVelocity(recoil);
                            world.spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);

                            player.sendActionBar("§e§l24 FPS: DIMENSIONAL SHATTER");
                        }
                    }.runTaskLater(JJKPlugin.getInstance(), 4L);
                }
            }.runTaskLater(JJKPlugin.getInstance(), 10L);
        }
    }

    private void spawnGhostFrame(Location loc, int amount) {
        World world = loc.getWorld();
        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life > 20) { this.cancel(); return; }

                // Создаем несколько фантомов с небольшим смещением (эффект шлейфа кадров)
                for (int i = 0; i < amount; i++) {
                    double offset = i * 0.1;
                    for (double y = 0; y < 1.8; y += 0.4) {
                        world.spawnParticle(Particle.DUST, loc.clone().add(offset, y, offset), 2, 0.1, 0.1, 0.1,
                                new Particle.DustOptions(frameColor, 1.2f));
                    }
                }
                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}