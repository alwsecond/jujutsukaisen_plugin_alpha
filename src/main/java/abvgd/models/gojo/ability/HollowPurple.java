package abvgd.models.gojo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HollowPurple extends ActiveAbility {
    private final Color blueColor = Color.fromRGB(50, 50, 255);
    private final Color redColor = Color.fromRGB(255, 0, 0);
    private final Color purpleColor = Color.fromRGB(160, 32, 240);

    public HollowPurple() {
        super(new JJKAbilityInfo(
                "§d§lHollow Purple",
                Material.PURPLE_STAINED_GLASS,
                100,
                12,
                0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        final int castTicks = 40; // Чуть увеличил для красоты спирали (2.5 секунды)
        JJKFunc.freezePlayer(player, castTicks);
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || tick > castTicks) {
                    this.cancel();
                    if (player.isOnline()) {
                        // Вспышка в момент слияния перед выстрелом
                        Location eye = player.getEyeLocation();
                        Location center = eye.clone().add(eye.getDirection().normalize().multiply(2.5));
                        world.spawnParticle(Particle.DUST, center, 100, 0.5, 0.5, 0.5, new Particle.DustOptions(purpleColor, 3.0f));
                        launchPurple(player);
                    }
                    return;
                }

                if (tick == 0) {playCustomSound("jjk.gojo.purple", player, 0.5f, 1.0f);}

                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                Location center = eye.clone().add(dir.multiply(2.5)); // Точка слияния чуть дальше от лица

                // Вектора для построения плоскости вращения
                Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                if (side.length() == 0) side = new Vector(1, 0, 0);
                Vector up = side.clone().crossProduct(dir).normalize();

                double progress = (double) tick / castTicks; // от 0.0 до 1.0

                // Спиральная математика: радиус уменьшается, скорость вращения экспоненциально растет
                double distance = 2.5 * (1.0 - progress);
                double angle = tick * 0.4 * (1.0 + progress * 3.0); // Ускоряющееся вращение

                // Вычисляем позиции двух сфер
                Vector offsetBlue = side.clone().multiply(Math.cos(angle)).add(up.clone().multiply(Math.sin(angle))).multiply(distance);
                Vector offsetRed = side.clone().multiply(Math.cos(angle + Math.PI)).add(up.clone().multiply(Math.sin(angle + Math.PI))).multiply(distance);

                Location blueLoc = center.clone().add(offsetBlue);
                Location redLoc = center.clone().add(offsetRed);

                // Отрисовка сфер
                renderOrb(blueLoc, blueColor);
                renderOrb(redLoc, redColor);

                // --- ЗВУК "КВАЗАРА" (УСКОРЯЮЩЕЕСЯ ТИКАНЬЕ) ---
                // Задержка между тиками: начинается с 10, заканчивается 1
                int tickGap = (int) Math.max(1, 10 - (progress * 9));
                if (tick % tickGap == 0) {
                    float pitch = 0.5f + (float) (progress * 1.5); // От низкого гула до звонкого щелчка
                    world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, pitch);
                    world.playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, pitch);
                }

                tick++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void renderOrb(Location loc, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 2.0f);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.05, 0.05, 0.05, dust);
        // Легкий след от спирали без мусорных частиц
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.02, 0.02, 0.02, 0.01);
    }

    private void launchPurple(Player player) {
        Location current = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().normalize().multiply(2.5));
        Vector direction = player.getEyeLocation().getDirection().normalize();
        World world = player.getWorld();

        world.playSound(current, Sound.ENTITY_WARDEN_SONIC_BOOM, 4.0f, 0.8f);
        world.playSound(current, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 3.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 10; // Дальность
            final Map<UUID, Long> damageCooldowns = new HashMap<>();

            @Override
            public void run() {
                if (ticks > maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 3; i++) {
                    RayTraceResult ray = world.rayTraceBlocks(current, direction, 0.8, FluidCollisionMode.NEVER, true);
                    if (ray != null && ray.getHitBlock() != null) {
                        performFinalImpact(current);
                        this.cancel();
                        return;
                    }

                    current.add(direction.clone().multiply(1.5));

                    renderProjectile(current);
                    eraseBlocksSimple(current, 4.5);
                    applyDamage(player, current, damageCooldowns);
                }

                // Глубокий звук при полете снаряда
                if (ticks % 3 == 0) {
                    world.playSound(current, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 2.0f);
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void renderProjectile(Location loc) {
        // Концентрированный, плотный шар Фиолетового (без лишних ведьмовых частиц)
        Particle.DustOptions core = new Particle.DustOptions(Color.fromRGB(230, 180, 230), 4.0f); // Темное ядро
        Particle.DustOptions aura = new Particle.DustOptions(purpleColor, 2.5f); // Яркая аура

        loc.getWorld().spawnParticle(Particle.DUST, loc, 20, 0.3, 0.3, 0.3, core);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 40, 1.2, 1.2, 1.2, aura);

        // Редкий всплеск энергии для эффекта нестабильности
        if (Math.random() > 0.8) {
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 5, 1.0, 1.0, 1.0, 0.05);
        }
    }

    private void eraseBlocksSimple(Location loc, double radius) {
        int r = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Location target = loc.clone().add(x, y, z);
                    if (target.distanceSquared(loc) <= radiusSquared) {
                        Block b = target.getBlock();
                        if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK && b.getType() != Material.OBSIDIAN) {
                            b.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    private void applyDamage(Player shooter, Location loc, Map<UUID, Long> cooldowns) {
        long now = System.currentTimeMillis();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 4.5, 4.5, 4.5)) {
            if (e instanceof LivingEntity victim && !e.equals(shooter)) {
                UUID id = victim.getUniqueId();
                if (!cooldowns.containsKey(id) || now - cooldowns.get(id) > 500) {
                    JJKDamage.causeAbilityDamage(victim, shooter, 80.0);
                    cooldowns.put(id, now);
                }
            }
        }
    }

    private void performFinalImpact(Location loc) {
        World world = loc.getWorld();
        // Чистый и красивый эффект взрыва 1.21 (без огня, только мощная волна)
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        world.spawnParticle(Particle.SONIC_BOOM, loc, 1); // Очень красивый эффект расхождения пространства

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.6f);
        world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.5f);

        eraseBlocksSimple(loc, 5.0); // Финальная дыра в стене
    }
}