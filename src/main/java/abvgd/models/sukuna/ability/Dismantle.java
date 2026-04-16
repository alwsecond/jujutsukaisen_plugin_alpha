package abvgd.models.sukuna.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Dismantle extends ActiveAbility {

    public Dismantle() {
        super(new JJKAbilityInfo(
                "Dismantle",
                Material.IRON_SWORD,
                0,
                260,    // Кулдаун 13 секунд
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        LivingEntity target = findTarget(player, 9.0);

        if (target == null) {
            player.sendMessage("§8[§c!§8] §7Цель не найдена");
            PlayerManager.get(player).setCooldown(this, 20);
            return;
        }

        // --- ФАЗА 1: "ДЗИИИИИНЬ" (ПОЯВЛЕНИЕ МЕТКИ) ---
        // Протяжный звук затачивающегося металла
        world.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.1f);
        world.playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            final int delay = 13; // 1 секунда подготовки

            @Override
            public void run() {
                if (!player.isOnline() || !target.isValid()) { this.cancel(); return; }

                if (t >= delay) {
                    // --- ФАЗА 2: КАЗНЬ (МОМЕНТАЛЬНЫЙ РАЗРЕЗ) ---
                    executeExecution(player, target);
                    this.cancel();
                    return;
                }

                // Плавное проявление контура разреза (белые искры)
                renderGhostlyEdge(target, t / (double)delay);

                // Легкая вибрация камеры цели (страх)
                if (t % 5 == 0) {
                    target.setVelocity(new Vector(0, 0.02, 0));
                }

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void renderGhostlyEdge(LivingEntity target, double progress) {
        Location loc = target.getLocation().add(0, 1.2, 0);
        double size = 1.3 * progress; // Линия растет со временем

        // Используем белые частицы для "призрачного" лезвия
        for (double i = -size; i <= size; i += 0.2) {
            Location p = loc.clone().add(i, i * 0.5, 0);
            target.getWorld().spawnParticle(Particle.FIREWORK, p, 1, 0, 0, 0, 0);
        }
    }

    private void executeExecution(Player player, LivingEntity target) {
        Location loc = target.getLocation().add(0, 1.2, 0);
        World world = target.getWorld();

        // Моментальный "ДЗИНЬ!"
        world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 2.0f, 1.8f);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.1f);

        // Кровавый разрез (Крест или широкая линия)
        drawBloodySlash(loc);

        // Взрыв частиц крови (Редстоун блоки)
        world.spawnParticle(Particle.BLOCK, loc, 60, 0.2, 0.4, 0.2, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        world.spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.3, 0.3, 0.3, 0);

        // Урон
        target.setNoDamageTicks(0);
        JJKDamage.causeAbilityDamage(target, player, 25.0); // Высокий урон за задержку

        // Эффект оцепенения после удара
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 10));
    }

    private void drawBloodySlash(Location center) {
        Particle.DustOptions blood = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.0f);
        double size = 1.5;
        for (double i = -size; i <= size; i += 0.1) {
            Location p = center.clone().add(i, i * 0.4, 0);
            center.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, blood);

            // Добавляем черные частицы (проклятая энергия)
            if (Math.random() > 0.8) {
                center.getWorld().spawnParticle(Particle.SMOKE, p, 1, 0, 0, 0, 0.02);
            }
        }
    }

    private LivingEntity findTarget(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                1.5,
                e -> e instanceof LivingEntity && !e.equals(player)
        );
        return (result != null && result.getHitEntity() != null) ? (LivingEntity) result.getHitEntity() : null;
    }
}
