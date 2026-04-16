package abvgd.models.hakari;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;

import abvgd.core.types.DashAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HakariDash extends DashAbility {

    public HakariDash() {
        super(new JJKAbilityInfo(
                "§a§lJackpot Dash",
                Material.EMERALD,
                0,
                0,
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector dir = player.getLocation().getDirection().setY(0).normalize();

        // Основной рывок
        player.setVelocity(dir.multiply(1.5).setY(0.25)); // 10 блоков +-
        player.setFallDistance(0);

        // Звуки при старте
        world.playSound(player.getLocation(), Sound.BLOCK_COPPER_BREAK, 1.2f, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);

        // Создаем динамический след (Trail)
        // Запускаем цикл на 6 тиков (примерно время полета), чтобы частицы шли за игроком
        for (int i = 0; i <= 6; i++) {
            Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), () -> {
                if (player.isOnline()) {
                    Location trailLoc = player.getLocation().add(0, 0.8, 0);

                    // Основной шлейф: искры металла и зеленая энергия
                    world.spawnParticle(Particle.SCRAPE, trailLoc, 5, 0.1, 0.1, 0.1, 0.05);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, trailLoc, 3, 0.2, 0.2, 0.2, 0.02);

                    // Дополнительный "мусор" (чернокамень), который сыплется за игроком
                    world.spawnParticle(Particle.BLOCK, trailLoc, 2, 0.1, 0.1, 0.1, 0.01,
                            Material.GILDED_BLACKSTONE.createBlockData());

                    // Звук свиста воздуха в середине пути
                    if (Bukkit.getCurrentTick() % 2 == 0) {
                        world.playSound(trailLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.5f);
                    }
                }
            }, i); // i - это задержка в тиках
        }
    }
}