package abvgd.models.kashimo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MythicalBeastAmber extends ActiveAbility {

    public MythicalBeastAmber() {
        super(new JJKAbilityInfo(
                "Mythical Beast Amber",
                Material.HONEYCOMB, // Если версия ниже 1.21, используй HONEYCOMB
                100, 300, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // 1. ЗАМОРОЗКА И ПОДГОТОВКА (Начало трансформации)
        JJKFunc.freezePlayer(player, 60); // Замораживаем на 3 секунды для каста

        // Звуки нарастающей мощи
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 2f, 0.5f);
        world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);

        new BukkitRunnable() {
            int timer = 0;
            @Override
            public void run() {
                if (timer > 60 || !player.isOnline()) {
                    startForm(player); // Запуск самой формы (код ниже)
                    this.cancel();
                    return;
                }

                // Эффект "стягивания" энергии: частицы летят к игроку
                world.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10, 2, 2, 2, -0.15);

                // Нарастающий звон
                if (timer % 10 == 0) {
                    world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f + (timer * 0.02f));
                    world.spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1);
                }

                // В конце каста подбрасываем игрока немного вверх
                if (timer == 50) {
                    player.setVelocity(new Vector(0, 0.2, 0));
                    world.playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 0.5f, 2f);
                }

                timer++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void startForm(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // ФИНАЛЬНЫЙ ВЗРЫВ ПРИ ПРЕВРАЩЕНИИ
        world.strikeLightningEffect(loc); // Безвредная молния
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);

        // Огромное кольцо искр
        for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
            Vector v = new Vector(Math.cos(i), 0.1, Math.sin(i)).multiply(3);
            world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(v), 5, 0.1, 0.1, 0.1, 0.5);
        }

        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc.add(0, 1, 0), 3);

        activateAmberEffects(player);
    }

    private void activateAmberEffects(Player player) {
        World world = player.getWorld();

        // 1. УСТАНОВКА БАФФОВ
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 1, false, false));

        // Включаем полет (Кашимо перемещается как электрический разряд)
        player.setAllowFlight(true);
        player.setFlying(true);

        player.sendMessage("§e§l⚡ ТЕЛО ПРЕОБРАЗОВАНО В ЯНТАРНЫЙ ИНЕЙ ⚡");

        new BukkitRunnable() {
            int ticks = 0;
            final int maxDuration = 600; // 30 секунд

            @Override
            public void run() {
                // Условия завершения: время вышло, игрок вышел или погиб
                if (ticks > maxDuration || !player.isOnline() || player.isDead()) {
                    endTransformation(player);
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);

                // 2. ПОСТОЯННЫЙ ВИЗУАЛ (Электрический силуэт)
                // Спавним искры хаотично вокруг игрока
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 8, 0.4, 0.6, 0.4, 0.2);
                if (ticks % 5 == 0) {
                    world.spawnParticle(Particle.FLASH, loc, 1, 0.1, 0.1, 0.1, 0);
                }

                // 3. АУРА УРОНА (Ионизация)
                // Каждые 10 тиков (0.5 сек) бьем током всех в радиусе 5 блоков
                if (ticks % 20 == 0) {
                    for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
                        if (entity instanceof LivingEntity target && entity != player) {
                            // Наносим урон через твой JJKDamage
                            JJKDamage.causeAbilityDamage(target, player, 2.0);

                            // Эффект микро-стана
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 3));
                            world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.1);
                            world.playSound(target.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 0.5f, 2f);
                        }
                    }
                }

                // 4. ЭФФЕКТ ДВИЖЕНИЯ
                // Если игрок быстро летит, оставляем за ним "хвост" из молний
                if (player.isFlying()) {
                    world.spawnParticle(Particle.FIREWORK, player.getLocation(), 1, 0, 0, 0, 0.05);
                }

                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void endTransformation(Player player) {
        World world = player.getWorld();
        player.setAllowFlight(false);
        player.setFlying(false);

        // Эффект распада тела (Суицидальная механика Кашимо)
        world.strikeLightningEffect(player.getLocation());
        world.spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 200, 1, 2, 1, 0.05);
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
        player.setHealth(1); // Не Смерть не по канону
    }
}
