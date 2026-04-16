package abvgd.utils;

import abvgd.JJKPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JJKFunc {
    public static final Map<UUID, Map<Location, BlockData>> domainHistory = new HashMap<>();
    public static void freezePlayer(Player player, int ticks) {
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= ticks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Получаем текущий вектор движения
                Vector vel = player.getVelocity();

                // ЗАМЕДЛЕНИЕ:
                // Умножаем X и Z на 0.75 (тормозим горизонтальное движение)
                // Умножаем Y на 0.5 (замедляем падение или прыжок)
                vel.setX(vel.getX() * 0.4);
                vel.setZ(vel.getZ() * 0.4);
                vel.setY(vel.getY() * 0.3);

                // Применяем измененную скорость обратно игроку
                player.setVelocity(vel);

                elapsed++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    public static void fxDestruction(Location loc, int radius, double chance, double power) {
        World world = loc.getWorld();
        if (world == null) return;

        int r = radius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    // Вычисляем текущий блок
                    Location targetLoc = loc.clone().add(x, y, z);

                    // Проверка на сферу (чтобы не ломать идеальный куб)
                    if (targetLoc.distance(loc) > radius) continue;

                    Block b = targetLoc.getBlock();
                    Material type = b.getType();

                    // Игнорируем воздух, бедрок и слишком твердые блоки (воду тоже)
                    if (type == Material.AIR || type == Material.BEDROCK || type == Material.BARRIER) continue;
                    if (type.getHardness() < 0 || type.getHardness() > 10) continue;
                    if (b.isLiquid()) continue;

                    // Шанс срабатывания
                    if (Math.random() > chance) continue;

                    // 1. Создаем вектор разлета (от центра взрыва к блоку)
                    Vector velocity = targetLoc.toVector().subtract(loc.toVector()).normalize().multiply(power);
                    velocity.setY(velocity.getY() + 0.2); // Чуть подбрасываем вверх для красоты

                    // 2. Спавним FallingBlock (визуальный объект)
                    FallingBlock fallingBlock = world.spawnFallingBlock(
                            targetLoc.add(0.5, 0.1, 0.5), // Центрируем по блоку
                            b.getBlockData()
                    );

                    fallingBlock.setDropItem(false); // Чтобы не спамить предметами на полу
                    fallingBlock.setHurtEntities(false); // Чтобы блоки не убивали игроков сами по себе
                    fallingBlock.setVelocity(velocity);

                    // 3. Убираем оригинальный блок
                    b.setType(Material.AIR);

                    // Добавляем немного пыли на месте разрушения
                    if (Math.random() > 0.8) {
                        world.spawnParticle(Particle.BLOCK, targetLoc, 5, b.getBlockData());
                    }
                }
            }
        }
    }
}
