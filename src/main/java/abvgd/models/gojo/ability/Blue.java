package abvgd.models.gojo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

public class Blue extends ActiveAbility {

    private static final Color BLUE_COLOR = Color.fromRGB(30, 80, 255);

    public Blue() {
        super(new JJKAbilityInfo(
                "§9§lBlue",
                Material.BLUE_STAINED_GLASS,
                300,
                320,
                0.0,
                false
        ));
    }

    // ──────────────────────────────────────────
    // КАСТ
    // ──────────────────────────────────────────

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        JJKFunc.freezePlayer(player, 20);
        new BukkitRunnable() {
            int timer = 0;
            final int castTicks = 40;

            @Override
            public void run() {
                if (!player.isOnline()) { this.cancel(); return; }

                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                double offset = (double) timer / castTicks * 2.5;
                Location orbLoc = eye.clone().add(dir.clone().multiply(offset));

                renderCastAura(player, orbLoc);
                renderGrowingCore(orbLoc, (double) timer / castTicks * 0.8);

                if (timer == 0) {
                    // Голос / звук активации из ресурспака (замени на свой ключ)
                    world.playSound(eye, "jjk.gojo.blue_activate", SoundCategory.PLAYERS, 3f, 1f);
                    world.playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
                    world.playSound(eye, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 0.5f);
                }
                if (timer % 10 == 0) {
                    world.playSound(orbLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                            1.0f, 0.5f + (float) timer / castTicks);
                }

                if (timer >= castTicks) {
                    this.cancel();
                    launchBlue(player, orbLoc);
                }
                timer++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    // ──────────────────────────────────────────
    // ПОЛЁТ ШАРа
    // ──────────────────────────────────────────

    private void launchBlue(Player player, Location startLoc) {
        World world = startLoc.getWorld();
        Vector direction = player.getEyeLocation().getDirection().normalize();

        final double maxDistance = 6.0;
        final double speed = 1.2;

        world.playSound(startLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0f, 0.6f);

        new BukkitRunnable() {
            Location current = startLoc.clone();
            double traveled = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Частицы хвоста во время полёта
                renderFlightTrail(current, ticks);

                // Двигаем дисплей
                current.add(direction.clone().multiply(speed));
                traveled += speed;

                // Проверка попадания в блок
                boolean hitBlock = current.getBlock().getType().isSolid();

                if (traveled >= maxDistance || hitBlock) {
                    this.cancel();

                    // Звук приземления
                    world.playSound(current, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 2.5f, 0.4f);
                    world.playSound(current, Sound.BLOCK_CONDUIT_ACTIVATE, 2.0f, 0.5f);
                    world.spawnParticle(Particle.FLASH, current, 3, 0.3, 0.3, 0.3, 0);

                    activateSingularity(player, current.clone());
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    // ──────────────────────────────────────────
    // СИНГУЛЯРНОСТЬ (точка притяжения)
    // ──────────────────────────────────────────

    private void activateSingularity(Player player, Location center) {
        World world = center.getWorld();
        final int duration = 100; // 5 секунд

        new BukkitRunnable() {
            int tick = 0;
            double size = 0.5; // Шар растёт

            @Override
            public void run() {
                if (tick >= duration || !player.isOnline()) {
                    finishSingularity(center);
                    this.cancel();
                    return;
                }

                // Шар плавно вырастает до 4.5
                if (size < 4.5) size += 0.08;

                // Звук пульсации
                if (tick % 6 == 0) {
                    world.playSound(center, Sound.BLOCK_CONDUIT_AMBIENT, 1.0f, 0.7f);
                }
                // Звук нарастания каждые 2 секунды
                if (tick % 40 == 0) {
                    world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.5f);
                }

                renderSingularity(center, size, tick);
                applyGravity(player, center, size);

                // Лёгкое разрушение блоков
                if (tick % 20 == 0) {
                    JJKFunc.fxDestruction(center, (int) size, 0.2, -0.4);
                }

                tick++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    // ──────────────────────────────────────────
    // ПРИТЯЖЕНИЕ И УРОН
    // ──────────────────────────────────────────

    private void applyGravity(Player shooter, Location center, double size) {
        double pullRadius = size + 5.0;

        for (Entity e : center.getWorld().getNearbyEntities(
                center, pullRadius, pullRadius, pullRadius)) {
            if (!(e instanceof LivingEntity victim) || e.equals(shooter)) continue;

            Vector toCenter = center.toVector().subtract(victim.getLocation().toVector());
            double dist = toCenter.length();
            if (dist < 0.3) continue;

            double force = (size * 0.5) / Math.max(1.0, dist * 0.3);
            victim.setVelocity(toCenter.normalize().multiply(force));

            if (dist < size) {
                JJKDamage.causeAbilityDamage(victim, shooter, 4);
                PlayerManager.get(shooter).addMastery(0.08);
                victim.getWorld().spawnParticle(
                        Particle.REVERSE_PORTAL,
                        victim.getLocation().add(0, 1, 0),
                        5, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    // ──────────────────────────────────────────
    // ВИЗУАЛЬНЫЕ ЭФФЕКТЫ
    // ──────────────────────────────────────────

    private void renderCastAura(Player player, Location orbLoc) {
        Location loc = player.getLocation().add(0, 1, 0);
        for (int i = 0; i < 3; i++) {
            Vector off = Vector.getRandom()
                    .subtract(new Vector(0.5, 0.5, 0.5)).multiply(1.5);
            Location start = loc.clone().add(off);
            Vector toOrb = orbLoc.toVector().subtract(start.toVector())
                    .normalize().multiply(0.3);
            player.getWorld().spawnParticle(Particle.DUST, start, 1, 0, 0, 0,
                    new Particle.DustOptions(BLUE_COLOR, 0.8f));
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, start,
                    0, toOrb.getX(), toOrb.getY(), toOrb.getZ(), 1);
        }
    }

    private void renderGrowingCore(Location loc, double size) {
        loc.getWorld().spawnParticle(Particle.DUST, loc, 10,
                0.05, 0.05, 0.05,
                new Particle.DustOptions(BLUE_COLOR, (float) size * 2.5f));
    }

    private void renderFlightTrail(Location loc, int ticks) {
        World world = loc.getWorld();
        // Ядро
        world.spawnParticle(Particle.DUST, loc, 12, 0.15, 0.15, 0.15,
                new Particle.DustOptions(BLUE_COLOR, 1.8f));
        // Хвост — обратный портал
        world.spawnParticle(Particle.REVERSE_PORTAL, loc, 8, 0.2, 0.2, 0.2, 0.05);
        // Кольцо из белых звёзд по кругу
        for (int i = 0; i < 6; i++) {
            double angle = (ticks * 0.4) + (i * Math.PI / 3);
            Location ring = loc.clone().add(
                    Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
            world.spawnParticle(Particle.END_ROD, ring, 1, 0, 0, 0, 0.01);
        }
    }

    private void renderSingularity(Location center, double size, int ticks) {
        World world = center.getWorld();

        // Основные частицы орба
        world.spawnParticle(Particle.DUST, center, 20, 0.2, 0.2, 0.2,
                new Particle.DustOptions(BLUE_COLOR, (float) size * 1.3f));

        // Всасывающие частицы
        world.spawnParticle(Particle.REVERSE_PORTAL, center,
                18, size, size, size, 0.1);

        // Вращающиеся кольца
        for (int i = 0; i < 3; i++) {
            double angle = (ticks * 0.8) + (i * (Math.PI * 2 / 3));
            Location ring = center.clone().add(
                    Math.cos(angle) * size,
                    Math.sin(angle * 0.5) * size,
                    Math.sin(angle) * size);
            world.spawnParticle(Particle.WITCH, ring, 1, 0, 0, 0, 0);
        }

        // Внешний ореол END_ROD для красоты
        if (ticks % 2 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (ticks * 0.3) + (i * Math.PI / 4);
                double r = size + 0.8;
                Location halo = center.clone().add(
                        Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                world.spawnParticle(Particle.END_ROD, halo, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    private void finishSingularity(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.5f);
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 0.8f);
        world.spawnParticle(Particle.FLASH, loc, 5, 0.5, 0.5, 0.5, 0.01);
        for (int i = 0; i < 30; i++) {
            world.spawnParticle(Particle.DUST, loc, 10, 1.5, 1.5, 1.5,
                    new Particle.DustOptions(BLUE_COLOR, 2f));
        }
    }
}