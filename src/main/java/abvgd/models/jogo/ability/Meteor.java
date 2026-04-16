package abvgd.models.jogo.ability;

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

public class Meteor extends ActiveAbility {

    public Meteor() {
        super(new JJKAbilityInfo(
                "Meteor",
                Material.MAGMA_CREAM,
                120, 800, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();

        // 1. ПОДГОТОВКА (Короткая заморозка и гул)
        JJKFunc.freezePlayer(player, 25);
        world.playSound(eyeLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
        world.playSound(eyeLoc, Sound.ITEM_FIRECHARGE_USE, 2f, 0.5f);

        // Спавним метеор чуть выше и впереди игрока
        Location spawnLoc = eyeLoc.clone().add(player.getLocation().getDirection().multiply(2)).add(0, 4, 0);

        BlockDisplay meteor = world.spawn(spawnLoc, BlockDisplay.class, b -> {
            b.setBlock(Material.MAGMA_BLOCK.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(2.5f, 2.5f, 2.5f); // Средний размер, меньше шансов застрять
            b.setTransformation(t);
            b.setInterpolationDuration(5);
        });

        new BukkitRunnable() {
            int ticks = 0;
            Vector shootDir = null;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    meteor.remove();
                    this.cancel();
                    return;
                }
                Location targetLoc = player.getTargetBlockExact(30) != null ?
                        player.getTargetBlockExact(30).getLocation() :
                        eyeLoc.clone().add(player.getLocation().getDirection().multiply(30));

                if (ticks < 20) {
                    // Метеор висит над игроком и накапливает жар
                    world.spawnParticle(Particle.FLAME, meteor.getLocation().add(1, 1, 1), 20, 1, 1, 1, 0.1);
                    world.spawnParticle(Particle.LAVA, meteor.getLocation().add(1, 1, 1), 5, 0.5, 0.5, 0.5, 0.05);
                }

                if (ticks == 20) {
                    // Фиксируем направление взгляда в момент выстрела
                    shootDir = targetLoc.toVector().subtract(meteor.getLocation().toVector()).normalize();
                    world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2f, 0.5f);
                }

                if (ticks > 20) {
                    Location current = meteor.getLocation().add(shootDir.clone().multiply(1.3));
                    meteor.teleport(current);

                    world.spawnParticle(Particle.FLAME, current.clone().add(1, 1, 1), 20, 0.5, 0.5, 0.5, 0.1);

                    // Проверка столкновения
                    if (current.getBlock().getType().isSolid() || current.distance(targetLoc) < 1.5) {
                        triggerExplosion(player, current);
                        meteor.remove();
                        this.cancel();
                    }

                    for (Entity e : world.getNearbyEntities(current.clone().add(1, 1, 1), 2, 2, 2)) {
                        if (e instanceof LivingEntity target && e != player) {
                            triggerExplosion(player, current);
                            meteor.remove();
                            this.cancel();
                            break;
                        }
                    }
                }

                if (ticks > 100) { // Защита от бесконечного полета
                    meteor.remove();
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void triggerExplosion(Player caster, Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 10, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.FLAME, loc, 500, 6, 3, 6, 0.5);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);

        for (Entity e : world.getNearbyEntities(loc, 10, 6, 10)) {
            if (e instanceof LivingEntity target && e != caster) {
                JJKDamage.causeAbilityDamage(target, caster, 22.0);
                target.setFireTicks(100);
                Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.0).setY(1.0);
                target.setVelocity(push);
            }
        }
    }
}