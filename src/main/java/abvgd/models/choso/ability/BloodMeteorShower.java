package abvgd.models.choso.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class BloodMeteorShower extends ActiveAbility {

    public BloodMeteorShower() {
        super(new JJKAbilityInfo(
                "Blood Meteor Shower",
                Material.NETHER_WART_BLOCK,
                100, 600, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // 1. ПЛАВНЫЙ ПОДЛЁТ И ЗАМОРОЗКА
        JJKFunc.freezePlayer(player, 50); // Замораживаем на 2.5 сек
        player.setVelocity(new Vector(0, 0.45, 0)); // Небольшой импульс вверх

        world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
        world.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.5f, 0.1f);

        // Создаем огромную сферу (BlockDisplay)
        BlockDisplay mainOrb = world.spawn(player.getEyeLocation().add(0, 3, 0), BlockDisplay.class, b -> {
            b.setBlock(Material.RED_NETHER_BRICKS.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(2.0f, 2.0f, 2.0f); // Увеличил размер сферы
            b.setTransformation(t);
            b.setInterpolationDuration(40);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks < 45) {
                    // Сфера "пульсирует", стягивая частицы
                    mainOrb.teleport(player.getEyeLocation().add(0, 3, 0));
                    world.spawnParticle(Particle.DUST, mainOrb.getLocation(), 20, 1.5, 1.5, 1.5,
                            new Particle.DustOptions(Color.fromRGB(120, 0, 0), 2.5f));

                    // Эффект левитации (удерживаем в воздухе во время каста)
                    player.setVelocity(new Vector(0, 0.05, 0));
                }

                if (ticks == 50) {
                    // 2. РАЗРЫВ СФЕРЫ И ГРАД МЕТЕОРИТОВ
                    mainOrb.remove();
                    world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0, 3, 0), 5);

                    // Залп из 25 мощных метеоритов
                    for (int i = 0; i < 25; i++) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                launchHeavyMeteor(player);
                            }
                        }.runTaskLater(JJKPlugin.getInstance(), i * 1L); // Быстрый темп огня
                    }
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void launchHeavyMeteor(Player player) {
        World world = player.getWorld();
        // Точка спавна метеорита в небе над игроком
        Location spawnLoc = player.getEyeLocation().add(Math.random() * 8 - 4, 3, Math.random() * 8 - 4);
        Vector dir = new Vector(Math.random() * 0.2 - 0.1, -1.0, Math.random() * 0.2 - 0.1).normalize();

        new BukkitRunnable() {
            Location current = spawnLoc.clone();
            int life = 0;

            @Override
            public void run() {
                if (life > 40 || current.getBlock().getType().isSolid()) {
                    // Эффект мощного удара
                    world.spawnParticle(Particle.BLOCK, current, 30, 0.5, 0.5, 0.5, Material.REDSTONE_BLOCK.createBlockData());
                    world.spawnParticle(Particle.EXPLOSION, current, 1);
                    world.playSound(current, Sound.BLOCK_NETHER_BRICKS_BREAK, 1f, 0.5f);

                    // УВЕЛИЧЕННЫЙ УРОН ПО ОБЛАСТИ
                    for (Entity e : world.getNearbyEntities(current, 4, 4, 4)) {
                        if (e instanceof LivingEntity target && e != player) {
                            JJKDamage.causeAbilityDamage(target, player, 10.0); // Урон за каждый метеорит

                            // Прижим к земле (тяжесть крови)
                            target.setVelocity(new Vector(0, -0.8, 0));
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 2));
                        }
                    }
                    this.cancel();
                    return;
                }

                // Визуал: Плотный кровавый снаряд
                world.spawnParticle(Particle.BLOCK, current, 5, 0.1, 0.1, 0.1, Material.REDSTONE_BLOCK.createBlockData());
                world.spawnParticle(Particle.DUST, current, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.2f));

                current.add(dir.clone().multiply(1.5)); // Очень высокая скорость падения
                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}