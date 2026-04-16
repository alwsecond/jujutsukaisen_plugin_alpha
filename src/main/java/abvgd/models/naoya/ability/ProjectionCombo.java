package abvgd.models.naoya.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class ProjectionCombo extends ActiveAbility {

    public ProjectionCombo() {
        super(new JJKAbilityInfo(
                "Projection Combo",
                Material.GRAY_STAINED_GLASS,
                0, 20, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        RayTraceResult ray = world.rayTraceEntities(player.getEyeLocation(),
                player.getLocation().getDirection(), 7, 1.2, (e) -> e instanceof LivingEntity && e != player);

        if (ray == null || ray.getHitEntity() == null) return;
        LivingEntity target = (LivingEntity) ray.getHitEntity();

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 1, false, false));

        new BukkitRunnable() {
            int hits = 0;
            final int maxHits = 6;

            @Override
            public void run() {
                if (hits >= maxHits || !target.isValid() || !player.isOnline()) {
                    performFinisher(player, target);
                    this.cancel();
                    return;
                }

                // 1. ПОЗИЦИИ (Круг вокруг цели)
                double angle = hits * (Math.PI * 2 / maxHits);
                // Смещение, чтобы прямоугольник летел ровно через центр
                Location startLoc = target.getLocation().clone().add(Math.cos(angle) * 4, 0.2, Math.sin(angle) * 4);
                startLoc.setDirection(target.getLocation().toVector().subtract(startLoc.toVector()));

                // Телепорт Наои для эффекта присутствия
                player.teleport(startLoc.clone().add(0, 1, 0));
                world.playSound(startLoc, Sound.BLOCK_GLASS_HIT, 1f, 2f);

                // 2. СОЗДАНИЕ БОЛЬШОГО ПРЯМОУГОЛЬНИКА
                BlockDisplay glassPanel = world.spawn(startLoc, BlockDisplay.class, ent -> {
                    ent.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
                    Transformation trans = ent.getTransformation();

                    // Делаем широкий и высокий прямоугольник (как экран)
                    // Scale: Ширина 2.5, Высота 2.5, Толщина 0.05
                    trans.getScale().set(2.5f, 2.5f, 0.05f);
                    // Центрируем панель относительно точки спавна
                    trans.getTranslation().set(-1.25f, 0f, 0f);

                    ent.setTransformation(trans);
                    ent.setInterpolationDuration(4); // Длительность полета
                    ent.setInterpolationDelay(0);
                });

                // 3. АНИМАЦИЯ ПРОЛЕТА
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!glassPanel.isValid()) return;
                        Transformation t = glassPanel.getTransformation();
                        // Смещаем панель на 8 блоков вперед (сквозь цель)
                        t.getTranslation().add(0, 0, 8f);
                        glassPanel.setTransformation(t);
                    }
                }.runTaskLater(JJKPlugin.getInstance(), 1L);

                // Эффекты при пролете через центр
                world.playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);
                world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, Material.WHITE_STAINED_GLASS.createBlockData());
                JJKDamage.causeAbilityDamage(target, player, 2.0);

                Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), glassPanel::remove, 5L);
                hits++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 5L);
    }

    private void performFinisher(Player player, LivingEntity target) {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // Взрыв кадра
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_STEP, 2f, 0.8f);
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 5);

        // Расчет векторов для отскока
        Vector dir = player.getLocation().getDirection().normalize();

        // Наоя отпрыгивает назад и вверх
        player.setVelocity(dir.multiply(-1.3).setY(0.6));

        // Цель отлетает вперед
        target.setVelocity(dir.multiply(2.0).setY(0.4));

        JJKDamage.causeAbilityDamage(target, player, 5.0);
    }
}