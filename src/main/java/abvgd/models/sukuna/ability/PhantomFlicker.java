package abvgd.models.sukuna.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class PhantomFlicker extends ActiveAbility {

    public PhantomFlicker() {
        super(new JJKAbilityInfo(
                "Phantom Flicker",
                Material.NETHERITE_BOOTS,
                0,
                30, // Кулдаун 15 секунд
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();

        // --- 1. СОЗДАНИЕ ПОСЛЕОБРАЗА ---
        // Создаем "фантома" на 2 секунды (визуальный силуэт)
        spawnAfterimage(startLoc);

        // --- 2. ЭФФЕКТЫ КОРОЛЯ ---
        // Невидимость на 3 секунды (60 тиков)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 1, false, false, false));
        // Огромная скорость для маневров (Speed 10+)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 5, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 4, false, false, false));

        // Звук: Резкий "свист" и затишье
        world.playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
        world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);

        // Убираем частицы от бега (чтобы игрока не вычислили по пыли)
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline() || t >= 40) {
                    // Звук выхода из инвиза
                    if (player.isOnline()) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
                        player.getWorld().spawnParticle(Particle.WHITE_SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.5, 0.2, 0.05);
                    }
                    this.cancel();
                    return;
                }

                // Пока игрок в инвизе, пускаем очень редкие частицы дыма, чтобы он сам видел себя
                player.spawnParticle(Particle.WHITE_SMOKE, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void spawnAfterimage(Location loc) {
        World world = loc.getWorld();

        // Рисуем статичный силуэт Сукуны из частиц
        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life > 30) { // Силуэт стоит 1.5 секунды
                    this.cancel();
                    return;
                }

                // Частицы формируют контур человека (упрощенно)
                for (double y = 0; y < 2; y += 0.4) {
                    world.spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 5, 0.2, 0.1, 0.2,
                            new Particle.DustOptions(Color.BLACK, 1.2f));
                }

                // Эффект искажения вокруг фантома
                if (life % 5 == 0) {
                    world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1, 0), 2, 0.1, 0.5, 0.1, 0.01);
                }

                life++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }
}