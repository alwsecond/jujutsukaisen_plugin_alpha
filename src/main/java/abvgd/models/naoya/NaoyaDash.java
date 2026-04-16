package abvgd.models.naoya;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class NaoyaDash extends DashAbility {

    public NaoyaDash() {
        super(new JJKAbilityInfo(
                "§e§l24 Frames",
                Material.FEATHER,
                0,
                0, // КД 0 для тестов
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector dir = player.getLocation().getDirection().normalize();

        // 1. Эффект ускорения "Проекции"
        // Наоя не получает невидимость надолго, он просто становится сверхбыстрым
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 4, false, false, false));

        // Звук: Резкий "стеклянный" щелчок и свист (как затвор камеры)
        world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 2.0f);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 1.8f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline() || t >= 10) {
                    this.cancel();
                    return;
                }

                // ФИКС ПРЫЖКА (как у Сукуны, прижимаем к земле для скорости)
                if (player.getVelocity().getY() > 0) {
                    player.setVelocity(player.getVelocity().setY(-0.1));
                }

                // 2. ВИЗУАЛ: "Кадры анимации"
                // Оставляем позади плоские вспышки, похожие на кадры кинопленки
                Location loc = player.getLocation().add(0, 1, 0);

                // Белые и желтые частицы "стекла"
                world.spawnParticle(Particle.INSTANT_EFFECT, loc, 5, 0.2, 0.4, 0.2, 0.05);

                if (t % 2 == 0) {
                    // Каждые 2 тика спавним "панель"
                    spawnFrame(loc);
                }

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void spawnFrame(Location loc) {
        // Эффект плоского кадра из частиц
        World world = loc.getWorld();
        for (double y = -0.5; y <= 0.5; y += 0.5) {
            for (double xz = -0.5; xz <= 0.5; xz += 0.5) {
                world.spawnParticle(Particle.CRIT, loc.clone().add(xz, y, xz), 1, 0, 0, 0, 0);
            }
        }
    }
}
