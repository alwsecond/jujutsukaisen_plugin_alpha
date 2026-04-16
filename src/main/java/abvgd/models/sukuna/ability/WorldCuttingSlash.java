package abvgd.models.sukuna.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorldCuttingSlash extends ActiveAbility {

    public WorldCuttingSlash() {
        super(new JJKAbilityInfo("WorldSlash", Material.NETHERITE_SWORD, 0, 20, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();

        // --- ЭПИЧНЫЙ СПИРАЛЬНЫЙ КАСТ ---
        JJKFunc.freezePlayer(player, 45);
        player.sendActionBar("§f§l✵ §0§lОБЪЯТЬ ПЕРЕОСМЫСЛЕНИЕ §f§l✵");

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { this.cancel(); return; }
                if (t >= 45) {
                    executePureSlash(player);
                    this.cancel();
                    return;
                }

                Location hand = getHandLoc(player);

                // Эффект воронки (стягивание к руке)
                double radius = 1.5 * (1.0 - (t / 45.0));
                double angle = t * 0.8;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                world.spawnParticle(Particle.FIREWORK, hand.clone().add(x, (Math.random()-0.5) * radius, z), 1, 0, 0, 0, 0);

                if (t % 15 == 0) {
                    world.playSound(hand, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.5f + (t * 0.01f));
                }
                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void executePureSlash(Player player) {
        World world = player.getWorld();
        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();

        // Математика 30 градусов (наклон лезвия)
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Vector up = new Vector(0, 1, 0).multiply(0.57); // tan(30°)
        Vector slashAxis = side.add(up).normalize();

        double range = 32.0;
        double width = 4.5;
        List<Entity> damaged = new ArrayList<>();

        // --- ЗВУКИ ОСТРОГО ЛЕЗВИЯ ---
        world.playSound(origin, Sound.ITEM_TRIDENT_THROW, 2f, 0.6f);
        world.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_HIT, 2f, 1.8f);
        world.playSound(origin, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1.5f, 2.0f);
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.4f);

        // Мгновенная вспышка в начале
        world.spawnParticle(Particle.FLASH, origin.clone().add(dir.clone().multiply(5)), 10, 2, 2, 2, 0);

        // --- МОМЕНТАЛЬНЫЙ РАЗРЕЗ ---
        for (double d = 0; d < range; d += 0.25) {
            Location center = origin.clone().add(dir.clone().multiply(d));

            for (double w = -width; w <= width; w += 0.1) {
                Location p = center.clone().add(slashAxis.clone().multiply(w));

                // Тончайший визуал (белая нить)
                world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 0.2f));
            }

            // Урон сущностям (проходит сквозь любые препятствия)
            for (Entity e : world.getNearbyEntities(center, 1.5, 4.0, 1.5)) {
                if (e instanceof LivingEntity le && !e.equals(player) && !damaged.contains(le)) {
                    damaged.add(le);
                    le.getWorld().spawnParticle(Particle.FLASH, le.getEyeLocation(), 1);
                    JJKDamage.causeAbilityDamage(le, player, 70.0);
                    le.setNoDamageTicks(0);
                }
            }
        }
        world.spawnParticle(Particle.FLASH, origin.clone().add(dir.clone().multiply(15)), 20, 10, 2, 10, 0);
    }

    private Location getHandLoc(Player player) {
        return player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.2));
    }
}