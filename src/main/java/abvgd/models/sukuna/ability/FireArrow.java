package abvgd.models.sukuna.ability;

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

public class FireArrow extends ActiveAbility {

    public FireArrow() {
        super(new JJKAbilityInfo(
                "Fuga",
                Material.FLINT_AND_STEEL,
                0,
                600,    // Кулдаун 30 секунд (ультимейт)
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();

        // --- ФАЗА 1: СТОЙКА И "ОТКРОЙСЯ" ---
        JJKFunc.freezePlayer(player, 45); // Замораживаем на 2.25 секунды
        player.sendMessage("§6§l■ §4§lОТКРОЙСЯ §6§l■");

        world.playSound(startLoc, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
        world.playSound(startLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.1f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { this.cancel(); return; }

                Location hand = getHandLocation(player);

                // --- ФАЗА 2: АНИМАЦИЯ НАТЯГИВАНИЯ ---
                if (t < 40) {
                    // Стягивание огня из пространства в руку
                    for (int i = 0; i < 3; i++) {
                        Location particleStart = hand.clone().add(
                                (Math.random() - 0.5) * 3,
                                (Math.random() - 0.5) * 3,
                                (Math.random() - 0.5) * 3
                        );
                        Vector toHand = hand.toVector().subtract(particleStart.toVector()).normalize().multiply(0.25);
                        world.spawnParticle(Particle.FLAME, particleStart, 0, toHand.getX(), toHand.getY(), toHand.getZ(), 0.5);
                    }

                    // Звук накапливания энергии
                    if (t % 10 == 0) {
                        world.playSound(hand, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 0.5f + (t * 0.02f));
                    }
                }

                // Визуализация самой стрелы в руке
                if (t >= 20 && t < 40) {
                    drawFireBow(player, hand, t);
                }

                // --- ФАЗА 3: ВЫСТРЕЛ ---
                if (t == 40) {
                    launchFireArrow(player);
                    this.cancel();
                }

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void drawFireBow(Player player, Location hand, int t) {
        // Рисуем призрачную огненную тетиву
        Vector dir = player.getLocation().getDirection();
        double progress = (t - 20) / 20.0; // от 0 до 1

        for (double d = -0.5; d <= 0.5; d += 0.1) {
            Location dot = hand.clone().add(dir.clone().multiply(-0.5 * progress)).add(0, d, 0);
            hand.getWorld().spawnParticle(Particle.SMALL_FLAME, dot, 1, 0, 0, 0, 0);
        }
    }

    private void launchFireArrow(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Звук старта — мощный хлопок
        world.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 2.0f, 0.5f);
        world.playSound(eye, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);

        new BukkitRunnable() {
            Location current = eye.clone();
            int dist = 0;

            @Override
            public void run() {
                if (dist > 30 || current.getBlock().getType().isSolid()) {
                    explodeArrow(current, player);
                    this.cancel();
                    return;
                }

                // Шлейф стрелы (очень густой огонь)
                world.spawnParticle(Particle.FLAME, current, 15, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.LAVA, current, 2, 0.05, 0.05, 0.05, 0);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, current, 5, 0.05, 0.05, 0.05, 0.02);

                // Проверка на попадание в сущность
                for (Entity e : world.getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity le && !e.equals(player)) {
                        explodeArrow(current, player);
                        this.cancel();
                        return;
                    }
                }

                current.add(dir.clone().multiply(2.5)); // Скорость стрелы
                dist++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void explodeArrow(Location loc, Player shooter) {
        World world = loc.getWorld();

        // Визуал атомного взрыва
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.FLAME, loc, 300, 3, 3, 3, 0.2);
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 100, 2, 2, 2, 0.05);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.5f);

        // Урон по площади
        for (Entity e : world.getNearbyEntities(loc, 7, 7, 7)) {
            if (e instanceof LivingEntity le) {

                // --- ФИКС: СУКУНА НЕ ПОЛУЧАЕТ УРОН ОТ СВОЕЙ СТРЕЛЫ ---
                if (le.equals(shooter)) continue;

                JJKDamage.causeAbilityDamage(le, shooter, 40.0);
                le.setFireTicks(200); // Поджог на 10 сек

                // Отбрасывание от эпицентра
                Vector v = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5);
                le.setVelocity(v);
            }
        }
    }

    private Location getHandLocation(Player player) {
        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        return loc.add(dir.multiply(0.5)).add(side.multiply(0.5)).subtract(0, 0.3, 0);
    }
}
