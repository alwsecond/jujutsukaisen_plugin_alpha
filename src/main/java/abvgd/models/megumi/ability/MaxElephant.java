package abvgd.models.megumi.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MaxElephant extends ActiveAbility {

    public MaxElephant() {
        super(new JJKAbilityInfo(
                "Max Elephant: Crush",
                Material.WATER_BUCKET,
                0, 10, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // 1. ПОИСК ЦЕЛИ (Над кем упадет слон)
        var ray = world.rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 15, 1.0, e -> e != player);
        Location targetLoc;

        if (ray != null && ray.getHitEntity() != null) {
            targetLoc = ray.getHitEntity().getLocation();
        } else {
            // Если цели нет, спавним в 8 блоках перед игроком
            targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(8));
        }

        // Точка спавна высоко в небе
        Location spawnLoc = targetLoc.clone().add(0, 10, 0);

        // 2. ПРИЗЫВ СЛОНИКА
        Ravager elephant = world.spawn(spawnLoc, Ravager.class, e -> {
            e.setCustomName("§7Max Elephant");
            e.setMetadata("Summon", new FixedMetadataValue(JJKPlugin.getInstance(), true));
            e.setGravity(true);
            // Чтобы он не бил игрока сам по себе
            e.setAI(false);
        });

        // Визуал тени в небе
        world.spawnParticle(Particle.SQUID_INK, spawnLoc, 50, 1, 1, 1, 0.1);

        // 3. ОТСЛЕЖИВАНИЕ ПАДЕНИЯ
        new BukkitRunnable() {
            double fallSpeed = 0.0;

            @Override
            public void run() {
                if (!elephant.isValid() || elephant.isDead()) {
                    this.cancel();
                    return;
                }

                // ВРУЧНУЮ ДВИГАЕМ ВНИЗ (Имитация гравитации)
                fallSpeed += 0.08; // Ускорение свободного падения
                elephant.teleport(elephant.getLocation().subtract(0, fallSpeed, 0));

                // Частицы воды во время полета
                world.spawnParticle(Particle.FALLING_WATER, elephant.getLocation().add(0, 1, 0), 10, 1, 1, 1, 0.1);

                // ПРОВЕРКА КАСАНИЯ ЗЕМЛИ
                // Если блок под ногами не воздух или мы опустились достаточно низко
                if (elephant.getLocation().getBlock().getType().isSolid() || elephant.isOnGround()) {
                    triggerCrushEffect(elephant, player);
                    this.cancel();
                }
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void triggerCrushEffect(Ravager elephant, Player owner) {
        Location loc = elephant.getLocation();
        World world = loc.getWorld();

        // ЭФФЕКТЫ ПРИЗЕМЛЕНИЯ
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        world.playSound(loc, Sound.ENTITY_RAVAGER_STEP, 2f, 0.5f);
        world.playSound(loc, Sound.ITEM_BUCKET_EMPTY, 1.5f, 0.5f);

        // Огромный всплеск воды и пыли
        world.spawnParticle(Particle.SPLASH, loc, 500, 4, 1, 4, 0.5);
        world.spawnParticle(Particle.CLOUD, loc, 100, 3, 0.5, 3, 0.1);

        // УРОН И ОТТАЛКИВАНИЕ
        for (Entity entity : world.getNearbyEntities(loc, 6, 4, 6)) {
            if (entity instanceof LivingEntity victim && entity != owner && !entity.hasMetadata("Summon")) {

                // Вектор от центра приземления к жертве
                Vector push = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
                push.multiply(2.5).setY(0.8); // Сильно раскидываем в стороны и вверх

                victim.setVelocity(push);

                // Системный урон (большой, так как это слон)
                JJKDamage.causeAbilityDamage(victim, owner, 18.0);

                // Эффект оглушения
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 10));
            }
        }

        // Слон исчезает в луже тени через секунду после удара
        new BukkitRunnable() {
            @Override
            public void run() {
                world.spawnParticle(Particle.SQUID_INK, elephant.getLocation(), 100, 1.5, 0.5, 1.5, 0.1);
                elephant.remove();
            }
        }.runTaskLater(JJKPlugin.getInstance(), 20L);
    }
}