package abvgd.models.toji.ability;

import abvgd.JJKPlugin;
import abvgd.core.types.ActiveAbility;
import abvgd.core.JJKAbilityInfo;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import org.bukkit.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PhantomDash extends ActiveAbility {

    public PhantomDash() {
        super(new JJKAbilityInfo(
                "PhantomDash",
                Material.NETHERITE_HOE,
                0,
                11, // КД в тиках плагина (обычно сек * 20)
                0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double maxDist = 16.0;

        // 1. ПРОВЕРКА ЦЕЛЕЙ (RayTrace logic)
        List<LivingEntity> victims = findVictimsInLine(player, startLoc, direction, maxDist);

        // --- КРИТИЧЕСКОЕ ИЗМЕНЕНИЕ: ОТМЕНА ПРИ ПРОМАХЕ ---
        if (victims.isEmpty()) {
            player.sendActionBar("§8[§c!§8] §7Цель не зафиксирована — техника отменена");
            // Проигрываем звук "неудачи"
            world.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);

            // Сбрасываем КД (если ваш плагин ставит его сразу при касте)
            PlayerManager.get(player).setCooldown(this, 0);
            return;
        }

        // 2. БЕЗОПАСНАЯ ТОЧКА ТЕЛЕПОРТАЦИИ (Фикс застревания)
        Location targetLoc = calculateSafeLocation(player, direction, maxDist);

        // --- ФАЗА 1: КОНЦЕНТРАЦИЯ (0.5 сек) ---
        // Игрок замирает только если цель НАЙДЕНА
        JJKFunc.freezePlayer(player, 10);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 10, false, false));

        world.playSound(startLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.5f);
        world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.2f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { this.cancel(); return; }

                if (t >= 10) {
                    executePhantomDash(player, targetLoc, victims, direction);
                    this.cancel();
                    return;
                }

                // Визуал: Искажение вокруг Тодзи
                world.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 10, 0.2, 0.5, 0.2, 0.1);
                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void executePhantomDash(Player player, Location targetLoc, List<LivingEntity> victims, Vector dir) {
        World world = player.getWorld();
        Location oldLoc = player.getLocation();

        // 1. DISAPPEARANCE
        world.spawnParticle(Particle.LARGE_SMOKE, oldLoc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        player.teleport(targetLoc);

        // Brief invisibility for the "phantom" effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 5, 1));

        // Dash Sounds
        world.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.8f);
        world.playSound(targetLoc, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.5f);

        // Render the "Ink" dash line
        renderDashLine(oldLoc, targetLoc, dir);

        // --- PHASE 2: THE SUSPENSE (Tension sounds) ---
        for (LivingEntity victim : victims) {
            world.playSound(victim.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.5f);
        }

        // --- PHASE 3: THE DELAYED EXECUTION (1.0 sec later) ---
        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity victim : victims) {
                    if (!victim.isValid() || victim.isDead()) continue;

                    // Trigger the visual and damage
                    drawBloodCross(victim.getLocation().add(0, 1.2, 0));
                    applyDamageEffect(player, victim);
                }
            }
        }.runTaskLater(JJKPlugin.getInstance(), 20); // 20 ticks = 1 second
    }

    private void drawBloodCross(Location center) {
        World world = center.getWorld();
        // High-contrast blood colors
        Particle.DustOptions darkRed = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 2.0f);
        Particle.DustOptions brightRed = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.2f);

        double size = 1.0; // Half-length of the cross lines

        // Draw the X shape
        for (double i = -size; i <= size; i += 0.1) {
            // Diagonal 1: \
            Location p1 = center.clone().add(i, i, 0);
            // Diagonal 2: /
            Location p2 = center.clone().add(i, -i, 0);

            world.spawnParticle(Particle.DUST, p1, 1, 0, 0, 0, (Math.random() > 0.5 ? darkRed : brightRed));
            world.spawnParticle(Particle.DUST, p2, 1, 0, 0, 0, (Math.random() > 0.5 ? darkRed : brightRed));
        }

        // Add a "Slash" impact flash and blood spray
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.BLOCK, center, 40, 0.2, 0.2, 0.2, Material.REDSTONE_BLOCK.createBlockData());

        // Sudden metal "Clang" sound at the moment of impact
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
    }

    private void applyDamageEffect(Player attacker, LivingEntity victim) {
        // High damage and "Shatter" sounds
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 1.8f);

        JJKDamage.causeAbilityDamage(victim, attacker, 35.0);
        victim.setNoDamageTicks(0);

        // Stun the victim slightly
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 5));
        victim.setVelocity(new Vector(0, 0.1, 0)); // Slight flinch
    }

    private Location calculateSafeLocation(Player player, Vector dir, double maxDist) {
        World world = player.getWorld();

        // 1. Получаем направление только в горизонтальной плоскости (X, Z)
        // Это не даст "втыкаться" в пол, если игрок смотрит вниз
        Vector horizontalDir = dir.clone().setY(0).normalize();

        // 2. RayTrace блоков (используем оригинальный dir, чтобы стены все еще детектились корректно)
        RayTraceResult ray = world.rayTraceBlocks(player.getEyeLocation(), dir, maxDist, FluidCollisionMode.NEVER, true);

        double actualDist = (ray != null) ? ray.getHitPosition().distance(player.getEyeLocation().toVector()) - 1.5 : maxDist;
        if (actualDist < 1) actualDist = 1;

        // 3. Базовая точка приземления (берем ноги игрока + горизонтальный сдвиг)
        Location target = player.getLocation().add(horizontalDir.multiply(actualDist));

        // 4. ФИКС: Убеждаемся, что мы не телепортируемся "в блок" из-за рельефа
        // Проверяем 3 блока вверх, чтобы найти свободное место (ноги и голова)
        for (int i = 0; i < 3; i++) {
            if (target.getBlock().getType().isSolid() || target.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                target.add(0, 1, 0); // Поднимаем точку, если там блок
            } else {
                break;
            }
        }

        // 5. ФИКС: Проверка на "воздух" под ногами (чтобы не зависнуть в блоке наполовину)
        // Если под нами воздух, спускаемся до ближайшего твердого блока
        while (target.getBlockY() > world.getMinHeight() &&
                target.clone().add(0, -1, 0).getBlock().getType().isAir() &&
                target.getBlockY() > (player.getLocation().getBlockY() - 3)) {
            target.subtract(0, 1, 0);
        }

        // Сохраняем направление взгляда игрока
        target.setDirection(dir);
        return target;
    }

    private List<LivingEntity> findVictimsInLine(Player player, Location start, Vector dir, double range) {
        List<LivingEntity> targets = new ArrayList<>();
        // Используем встроенный RayTrace для точности
        Collection<Entity> nearby = start.getWorld().getNearbyEntities(start.clone().add(dir.clone().multiply(range/2)), range, range, range);

        for (Entity e : nearby) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                // Проверяем, находится ли сущность близко к линии рывка (дистанция до луча < 2.0)
                Vector toEntity = le.getLocation().toVector().subtract(start.toVector());
                double dot = toEntity.dot(dir);
                if (dot > 0 && dot < range) {
                    Vector projection = dir.clone().multiply(dot);
                    if (toEntity.distance(projection) < 2.2) {
                        targets.add(le);
                    }
                }
            }
        }
        return targets;
    }

    private void renderDashLine(Location from, Location to, Vector dir) {
        double dist = from.distance(to);
        for (double i = 0; i < dist; i += 0.5) {
            Location point = from.clone().add(dir.clone().multiply(i)).add(0, 1, 0);
            from.getWorld().spawnParticle(Particle.WITCH, point, 2, 0.05, 0.05, 0.05, 0.01);
            if (i % 2 == 0) {
                from.getWorld().spawnParticle(Particle.SQUID_INK, point, 3, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
}