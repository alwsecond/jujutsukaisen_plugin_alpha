package abvgd.models.megumi.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Nue extends ActiveAbility {

    public Nue() {
        super(new JJKAbilityInfo(
                "Nue",
                Material.FEATHER,
                0, 10, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location spawnLoc = player.getLocation().add(0, 5, 0); // Спавним над головой

        // Эффект призыва (Тень + Электричество)
        world.spawnParticle(Particle.SQUID_INK, player.getLocation(), 30, 1, 0.1, 1, 0.05);
        world.spawnParticle(Particle.ELECTRIC_SPARK, spawnLoc, 50, 1, 1, 1, 0.1);
        world.playSound(spawnLoc, Sound.ENTITY_PHANTOM_AMBIENT, 2f, 0.5f);

        // 1. ПРИЗЫВ ФАНТОМА
        Phantom nue = world.spawn(spawnLoc, Phantom.class, p -> {
            p.setCustomName("§eNue");
            p.setCustomNameVisible(false);
            p.setSize(5); // Делаем его большим
            p.setMetadata("Nue", new org.bukkit.metadata.FixedMetadataValue(JJKPlugin.getInstance(), true));
            p.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.6);
        });

        // 2. ЛОГИКА ПОВЕДЕНИЯ
        new BukkitRunnable() {
            int ticks = 0;
            boolean dived = false;

            @Override
            public void run() {
                if (ticks > 120 || !nue.isValid() || !player.isOnline()) {
                    nue.remove();
                    this.cancel();
                    return;
                }

                // Частицы электричества вокруг Нуэ
                world.spawnParticle(Particle.ELECTRIC_SPARK, nue.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);

                // Ищем цель по взгляду игрока
                Entity target = getTarget(player);
                if (target instanceof LivingEntity livingTarget && !dived) {
                    performDive(nue, livingTarget, player);
                    dived = true; // Один мощный удар за призыв
                } else if (!dived) {
                    // Если цели нет, просто летает кругами над игроком
                    Location orbit = player.getLocation().add(0, 6, 0);
                    nue.setTarget(null);
                    nue.setVelocity(orbit.toVector().subtract(nue.getLocation().toVector()).normalize().multiply(0.5));
                }

                ticks += 5;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 5L);
    }

    private void performDive(Phantom nue, LivingEntity target, Player owner) {
        new BukkitRunnable() {
            int timeout = 0;

            @Override
            public void run() {
                // Если цель или Нуэ пропали, или летим слишком долго (более 3 сек)
                if (!nue.isValid() || !target.isValid() || timeout > 60) {
                    nue.remove();
                    this.cancel();
                    return;
                }

                // 1. ПРИНУДИТЕЛЬНОЕ ДВИЖЕНИЕ
                Vector dash = target.getLocation().add(0, 1, 0).toVector()
                        .subtract(nue.getLocation().toVector()).normalize().multiply(1.5);
                nue.setVelocity(dash);

                // Поворачиваем Нуэ мордой к цели
                nue.setRotation((float) Math.toDegrees(Math.atan2(-dash.getX(), dash.getZ())),
                        (float) Math.toDegrees(Math.asin(-dash.getY() / dash.length())));

                // 2. ПРОВЕРКА ДИСТАНЦИИ (Гитбокс Нуэ)
                // Проверяем, коснулся ли Нуэ цели (радиус 2.5 блока для надежности)
                if (nue.getLocation().distance(target.getLocation()) < 2.5) {
                    triggerElectricExplosion(nue, target, owner);
                    this.cancel();
                    return;
                }

                timeout++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L); // Проверка каждый тик для точности
    }

    private void triggerElectricExplosion(Phantom nue, LivingEntity target, Player owner) {
        World world = nue.getWorld();
        Location loc = target.getLocation();

        // ЭФФЕКТЫ
        world.spawnParticle(Particle.FLASH, loc, 3);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 100, 1.5, 1.5, 1.5, 0.2);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 2f);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f); // Звук "разбитой" тени

        // УРОН ПО ВСЕМ ВОКРУГ
        for (Entity e : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (e instanceof LivingEntity victim && e != owner && !(e instanceof Wolf) && !(e instanceof Phantom)) {
                // Твой системный урон
                JJKDamage.causeAbilityDamage(victim, owner, 14.0);

                // Эффект шока
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 4, false, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 1, false, false));
            }
        }

        // Нуэ исчезает после взрыва
        world.spawnParticle(Particle.SQUID_INK, nue.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
        nue.remove();
    }

    private Entity getTarget(Player p) {
        var ray = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getLocation().getDirection(), 20, 1.0, e -> e != p);
        return (ray != null) ? ray.getHitEntity() : null;
    }
}
