package abvgd.models.toji;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class TojiDash extends DashAbility {

    public TojiDash() {
        super(new JJKAbilityInfo(
                "§8§lApex Flicker",
                Material.IRON_BOOTS,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();
        double range = 10.0;

        // 1. РАСЧЕТ ТОЧКИ ПРИЗЕМЛЕНИЯ (мгновенный рейтрейс)
        RayTraceResult ray = world.rayTraceBlocks(player.getEyeLocation(), direction, range, FluidCollisionMode.NEVER, true);
        Location target = (ray != null && ray.getHitBlock() != null)
                ? ray.getHitPosition().toLocation(world).subtract(direction.clone().multiply(0.8))
                : startLoc.clone().add(direction.multiply(range));

        target.setY(startLoc.getY()); // Удерживаем на уровне земли
        target.setDirection(player.getLocation().getDirection());

        // 2. СОЗДАНИЕ ПОСЛЕОБРАЗА (Статичный силуэт Тодзи)
        spawnTojiGhost(startLoc);

        // 3. ЗВУКИ: Резкий хлопок воздуха (как выстрел)
        world.playSound(startLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.0f);
        world.playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 1.5f);

        // 4. МГНОВЕННОЕ ПЕРЕМЕЩЕНИЕ (через 1 тик для плавности анимации)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Линия "разреза" воздуха по пути
                world.spawnParticle(Particle.CLOUD, startLoc.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.5);

                player.teleport(target);

                // Звук приземления (тяжелый шаг)
                world.playSound(target, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);
                world.spawnParticle(Particle.WHITE_ASH, target, 15, 0.3, 0.1, 0.3, 0.05);
            }
        }.runTaskLater(JJKPlugin.getInstance(), 1L);
    }

    private void spawnTojiGhost(Location loc) {
        World world = loc.getWorld();
        // Используем темные частицы (черный и серый), так как Тодзи — "невидимка" для магов
        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life > 10) { this.cancel(); return; }

                for (double y = 0; y < 1.8; y += 0.4) {
                    world.spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 5, 0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.fromRGB(40, 40, 40), 1.2f));
                }
                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}