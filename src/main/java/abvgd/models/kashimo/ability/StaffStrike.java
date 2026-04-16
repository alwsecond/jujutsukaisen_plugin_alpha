package abvgd.models.kashimo.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class StaffStrike extends ActiveAbility {

    public StaffStrike() {
        super(new JJKAbilityInfo(
                "Staff Strike <interact>",
                Material.WOODEN_SHOVEL,
                30, 25, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        // 1. СОЗДАЕМ ПОСОХ
        BlockDisplay staff = world.spawn(start, BlockDisplay.class, b -> {
            b.setBlock(Material.COPPER_BLOCK.createBlockData());
            Transformation t = b.getTransformation();
            // Начальный размер (короткий, перед выпадом)
            t.getScale().set(0.15f, 0.15f, 1.0f);
            b.setTransformation(t);
            b.setInterpolationDuration(3); // Плавность выпада
            b.setInterpolationDelay(0);
        });

        // 2. ЗВУК ВЫПАДА (Свист воздуха + лязг)
        world.playSound(start, Sound.ITEM_TRIDENT_THROW, 1.2f, 1.2f);
        world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 6 || !player.isOnline()) {
                    staff.remove();
                    this.cancel();
                    return;
                }

                // 3. ЭФФЕКТ ВЫПАДА (Удлинение модели)
                if (ticks == 1) {
                    Transformation t = staff.getTransformation();
                    t.getScale().set(0.15f, 0.15f, 4.5f); // Резко удлиняем до 4.5 блоков
                    staff.setTransformation(t);
                }

                // Позиционирование перед игроком
                Location currentLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.2));
                staff.teleport(currentLoc);
                staff.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());

                // Проверка Комбо
                if (player.hasMetadata("IsCharged")) {
                    for (Entity e : world.getNearbyEntities(currentLoc.add(direction.multiply(2)), 6, 6, 6)) {
                        if (e instanceof LivingEntity target && e != player) {
                            performElectricCombo(player, target);
                            player.removeMetadata("IsCharged", JJKPlugin.getInstance());
                            staff.remove();
                            this.cancel();
                            return;
                        }
                    }
                }

                // Обычный урон выпада
                for (Entity e : world.getNearbyEntities(currentLoc.add(direction.multiply(2.5)), 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity target && e != player) {
                        JJKDamage.causeAbilityDamage(target, player, 7.0);
                        world.playSound(target.getLocation(), Sound.BLOCK_COPPER_HIT, 1f, 1.2f);
                        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.5);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void performElectricCombo(Player player, LivingEntity target) {
        World world = player.getWorld();

        // Рывок (Зигзаг)
        player.teleport(target.getLocation().subtract(target.getLocation().getDirection().multiply(1.5)));
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 2f);

        new BukkitRunnable() {
            int hit = 0;
            @Override
            public void run() {
                if (hit >= 3 || target.isDead()) {
                    target.setVelocity(new Vector(0, 1.3, 0));
                    world.spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1);
                    world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1f);
                    this.cancel();
                    return;
                }

                world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
                world.playSound(target.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 1f, 1.4f + (hit * 0.3f));

                JJKDamage.causeAbilityDamage(target, player, 5.0);
                player.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).multiply(0.25));

                hit++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 3L);
    }
}