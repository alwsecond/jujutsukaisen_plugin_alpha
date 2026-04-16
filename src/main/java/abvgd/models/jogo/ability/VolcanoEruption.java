package abvgd.models.jogo.ability;

import abvgd.core.types.ActiveAbility;
import org.bukkit.World;
import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class VolcanoEruption extends ActiveAbility {

    public VolcanoEruption() {
        super(new JJKAbilityInfo(
                "Volcano Eruption",
                Material.BASALT,
                55, 120, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        // Ищем блок земли перед игроком (до 10 блоков)
        Block targetBlock = player.getTargetBlockExact(10);
        Location spawnLoc = (targetBlock != null) ? targetBlock.getLocation().add(0, 1, 0) :
                player.getLocation().add(player.getLocation().getDirection().multiply(4));

        world.playSound(spawnLoc, Sound.BLOCK_BASALT_PLACE, 2f, 0.5f);
        world.playSound(spawnLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);

        // 1. СОЗДАЕМ МОДЕЛЬ ВУЛКАНА (BlockDisplay)
        BlockDisplay volcano = world.spawn(spawnLoc.clone().subtract(0, 1, 0), BlockDisplay.class, b -> {
            b.setBlock(Material.BASALT.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(1.2f, 0.1f, 1.2f); // Сначала плоский
            b.setTransformation(t);
            b.setInterpolationDuration(10);
            b.setInterpolationDelay(0);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Анимация роста вулкана
                if (ticks == 1) {
                    Transformation t = volcano.getTransformation();
                    t.getScale().set(1.2f, 1.5f, 1.2f); // Вырастает вверх
                    volcano.setTransformation(t);
                }

                // Эффекты дыма из жерла
                world.spawnParticle(Particle.LARGE_SMOKE, spawnLoc.clone().add(0, 1.5, 0), 2, 0.1, 0.1, 0.1, 0.05);
                if (ticks % 2 == 0) world.spawnParticle(Particle.LAVA, spawnLoc.clone().add(0, 1.5, 0), 1);

                // 2. ИЗВЕРЖЕНИЕ (на 20-м тике)
                if (ticks == 20) {
                    world.playSound(spawnLoc, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.5f);
                    world.playSound(spawnLoc, Sound.ITEM_FIRECHARGE_USE, 2f, 0.5f);

                    // Огромный столб огня вверх
                    new BukkitRunnable() {
                        int eTicks = 0;
                        @Override
                        public void run() {
                            if (eTicks > 15) {
                                volcano.remove();
                                this.cancel();
                                return;
                            }
                            // Частицы пламени летят вверх
                            Location top = spawnLoc.clone().add(0, 1.5, 0);
                            world.spawnParticle(Particle.FLAME, top, 30, 0.3, 2.0, 0.3, 0.3);
                            world.spawnParticle(Particle.LAVA, top, 5, 0.2, 1.0, 0.2, 0.1);

                            // Урон врагам над вулканом
                            for (Entity e : world.getNearbyEntities(top.add(0, 2, 0), 2.5, 5, 2.5)) {
                                if (e instanceof LivingEntity target && e != player) {
                                    JJKDamage.causeAbilityDamage(target, player, 12.0);
                                    target.setFireTicks(100); // Поджигает на 5 сек
                                    target.setVelocity(new Vector(0, 1.2, 0)); // Подбрасывает лавой
                                }
                            }
                            eTicks++;
                        }
                    }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);

                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}
