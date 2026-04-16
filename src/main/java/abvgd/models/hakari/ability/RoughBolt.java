package abvgd.models.hakari.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;

import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public class RoughBolt extends ActiveAbility {

    public RoughBolt() {
        super(new JJKAbilityInfo("Rough Bolt", Material.LIME_CANDLE, 0, 20, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
        Vector direction = player.getLocation().getDirection().normalize();

        // 1. ПРИКОЛЬНЫЙ ЭФФЕКТ КАСТА (Накапливание на пальце)
        world.playSound(start, Sound.BLOCK_COPPER_BULB_HIT, 1f, 2f);
        world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 5) { // Каст длится 0.25 сек перед выстрелом
                    shoot(player, world);
                    this.cancel();
                    return;
                }

                // Стягивающиеся к пальцу зеленые искры
                Location hand = getHandLocation(player);
                world.spawnParticle(Particle.HAPPY_VILLAGER, hand, 5, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.DUST, hand, 3, 0.1, 0.1, 0.1,
                        new Particle.DustOptions(Color.fromRGB(173, 255, 47), 1.0f));

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void shoot(Player player, World world) {
        Location origin = getHandLocation(player);
        Vector dir = player.getLocation().getDirection().normalize();

        world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.8f);
        world.playSound(origin, Sound.ITEM_FLINTANDSTEEL_USE, 1.5f, 1.2f);

        new BukkitRunnable() {
            Location current = origin.clone();
            int steps = 0;

            @Override
            public void run() {
                // Увеличим количество шагов за один тик для плавности или просто увеличим дистанцию
                for (int i = 0; i < 2; i++) { // Проверяем дважды за тик для точности
                    if (steps > 30 || !current.getBlock().getType().isAir()) {
                        this.cancel();
                        return;
                    }

                    // Визуал
                    world.spawnParticle(Particle.DUST, current, 3, 0.05, 0.05, 0.05,
                            new Particle.DustOptions(Color.fromRGB(50, 205, 50), 0.8f));
                    if (steps % 2 == 0) world.spawnParticle(Particle.SCRAPE, current, 1, 0, 0, 0, 0);

                    Collection<Entity> targets = world.getNearbyEntities(current, 1.2, 1.2, 1.2);
                    for (Entity e : targets) {
                        if (e instanceof LivingEntity victim && e != player) {
                            applyHit(victim, player);
                            this.cancel();
                            return;
                        }
                    }

                    current.add(dir.clone().multiply(0.75)); // Шаг стал меньше, но проверок больше = точность
                    steps++;
                }
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void applyHit(LivingEntity victim, Player caster) {
        victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, Material.LIME_CONCRETE.createBlockData());
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.8f);

        JJKDamage.causeAbilityDamage(victim, caster, 8.0);
        // Небольшое замедление от "шершавого" шока
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1));
    }

    private Location getHandLocation(Player p) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        return loc.add(dir.multiply(0.8)).add(side.multiply(0.35)).subtract(0, 0.2, 0);
    }
}