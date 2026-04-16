package abvgd.models.choso.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class SlicingExorcism extends ActiveAbility {

    public SlicingExorcism() {
        super(new JJKAbilityInfo(
                "Slicing Exorcism",
                Material.IRON_HOE,
                35, 40, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // Выпускаем два диска: левый и правый
        fireVisibleDisk(player, true);
        fireVisibleDisk(player, false);

        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.5f);
    }

    private void fireVisibleDisk(Player player, boolean left) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        // Смещение в сторону для старта дуги
        Vector sideOffset = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(left ? 1.2 : -1.2);

        // 1. СОЗДАЕМ ВИДИМЫЙ ДИСК
        BlockDisplay disk = world.spawn(start.clone().add(sideOffset), BlockDisplay.class, b -> {
            b.setBlock(Material.RED_NETHER_BRICKS.createBlockData());
            Transformation t = b.getTransformation();
            // Делаем его плоским и широким как диск (X и Y большие, Z тонкий)
            t.getScale().set(0.8f, 0.8f, 0.05f);
            b.setTransformation(t);
            b.setInterpolationDuration(1);
            b.setInterpolationDelay(0);
        });

        new BukkitRunnable() {
            int ticks = 0;
            Location current = start.clone().add(sideOffset);
            float rotation = 0;

            @Override
            public void run() {
                if (ticks > 30 || current.getBlock().getType().isSolid() || !player.isOnline()) {
                    disk.remove();
                    this.cancel();
                    return;
                }

                // 2. ТРАЕКТОРИЯ ДУГИ
                // Диски сначала разлетаются, а потом сходятся в точку взгляда игрока
                double curve = Math.sin(ticks * 0.15) * (left ? 1.0 : -1.0);
                current.add(direction.clone().multiply(1.1)).add(sideOffset.clone().multiply(curve * 0.5));

                // 3. ВРАЩЕНИЕ И ТЕЛЕПОРТАЦИЯ
                rotation += 45f; // Скорость вращения диска
                disk.teleport(current);

                // Настраиваем поворот диска, чтобы он "лежал" или стоял вертикально и крутился
                Transformation t = disk.getTransformation();
                t.getLeftRotation().setAngleAxis((float) Math.toRadians(rotation), 0, 0, 1);
                disk.setTransformation(t);

                // Эффект кровавого шлейфа
                world.spawnParticle(Particle.DUST, current, 3, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f));

                // 4. ПРОВЕРКА ПОПАДАНИЯ
                for (Entity e : world.getNearbyEntities(current, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity target && e != player) {
                        JJKDamage.causeAbilityDamage(target, player, 10.0);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 50, 1));

                        world.playSound(target.getLocation(), Sound.BLOCK_NETHER_BRICKS_BREAK, 1f, 1.8f);
                        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 25, 0.2, 0.2, 0.2, Material.REDSTONE_BLOCK.createBlockData());

                        disk.remove();
                        this.cancel();
                        return;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}