package abvgd.models.yuji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ManjiKick extends ActiveAbility {

    private final Color yujiBlue = Color.fromRGB(0, 191, 255);

    public ManjiKick() {
        super(new JJKAbilityInfo(
                "§6§lManji Kick",
                Material.LEATHER_BOOTS,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // 1. ФАЗА: ПОДГОТОВКА И ПОДБРОС (Подсечка)
        player.setVelocity(new Vector(0, 0.3, 0));
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);

        // Собираем цели в радиусе
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(4.5, 2.5, 4.5)) {
            if (e instanceof LivingEntity victim && !e.equals(player)) {
                targets.add(victim);
                // Резко подбрасываем цели ВВЕРХ
                victim.setVelocity(new Vector(0, 0.8, 0));
                world.spawnParticle(Particle.CLOUD, victim.getLocation(), 5, 0.2, 0.1, 0.2, 0.05);
            }
        }

        // 2. ФАЗА: ОСНОВНОЙ УДАР (Разлет в стороны)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Визуал кругового удара
                drawBlueArc(player);
                world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2f, 0.8f);
                world.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);

                for (LivingEntity victim : targets) {
                    if (!victim.isValid()) continue;

                    // Вычисляем вектор "ОТ Игрока К Жертве"
                    Vector launchDir = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    // Сильно толкаем в сторону и чуть-чуть вверх для пафоса
                    launchDir.multiply(1.8).setY(0.3);

                    victim.setVelocity(launchDir);

                    // Урон и эффекты
                    JJKDamage.causeAbilityDamage(victim, player, 8.0);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    world.spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                }
            }
        }.runTaskLater(JJKPlugin.getInstance(), 5L); // Задержка 5 тиков, пока цели в воздухе
    }

    private void drawBlueArc(Player player) {
        World world = player.getWorld();
        for (int i = 0; i < 24; i++) {
            double angle = i * (Math.PI * 2 / 24);
            double x = Math.cos(angle) * 4.0;
            double z = Math.sin(angle) * 4.0;
            Location pLoc = player.getLocation().add(x, 0.5, z);
            world.spawnParticle(Particle.DUST, pLoc, 3, 0.1, 0.1, 0.1, new Particle.DustOptions(yujiBlue, 2.0f));
            if (i % 2 == 0) world.spawnParticle(Particle.SOUL, pLoc, 1, 0, 0, 0, 0.05);
        }
    }
}
