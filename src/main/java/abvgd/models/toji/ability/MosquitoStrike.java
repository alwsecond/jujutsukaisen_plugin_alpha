package abvgd.models.toji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MosquitoStrike extends ActiveAbility {

    public MosquitoStrike() {
        super(new JJKAbilityInfo(
                "MosquitoStrike",
                Material.IRON_SWORD,
                0,
                400, // 20 sec cooldown
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        LivingEntity target = findSingleTarget(player, 8.0);

        if (target == null) {
            player.sendActionBar("§8[§c!§8] §7Цель слишком далеко...");
            PlayerManager.get(player).setCooldown(this, 20);
            return;
        }

        // --- РЫВОК ТОДЗИ (БЛИЦ) ---
        // Телепортируемся максимально близко (0.7 блока)
        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        Location blitzPos = target.getLocation().subtract(toTarget.multiply(0.7));
        blitzPos.setDirection(toTarget);
        player.teleport(blitzPos);

        // Звук резкого перемещения и сердцебиение хищника
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1.2f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 16;

            @Override
            public void run() {
                if (!player.isOnline() || !target.isValid() || ticks > duration) {
                    executeBrutalFinisher(player, target);
                    this.cancel();
                    return;
                }

                // 1. ПРИЛИПАНИЕ (Тоджи не дает врагу сбежать)
                Location targetLoc = target.getLocation();
                Vector stickDir = targetLoc.getDirection().multiply(-0.8);
                Location followLoc = targetLoc.clone().add(stickDir).add(0, 0.4, 0);
                followLoc.setDirection(targetLoc.toVector().subtract(followLoc.toVector()));
                player.teleport(followLoc);

                // 2. СЕРИЯ ТЯЖЕЛЫХ УДАРОВ (каждые 3 тика)
                if (ticks % 3 == 0) {
                    // Звуки: Тяжелый удар по дереву/кости + глухой хруст
                    world.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.8f); // Звук сильного удара игрока
                    world.playSound(targetLoc, Sound.ENTITY_LLAMA_SPIT, 1.5f, 0.5f);           // Глухой "шлепок" (на низкой тональности)
                    world.playSound(targetLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.2f);

                    // Визуал: Ударная волна и "густая" кровь
                    Location hitPoint = targetLoc.clone().add(0, 0.5 + Math.random(), 0);
                    world.spawnParticle(Particle.CRIT, hitPoint, 10, 0.1, 0.1, 0.1, 0.2);
                    world.spawnParticle(Particle.GUST_EMITTER_SMALL, hitPoint, 1, 0, 0, 0, 0);

                    // Кровавые брызги
                    Particle.DustOptions bloodDust = new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.8f);
                    world.spawnParticle(Particle.DUST, hitPoint, 15, 0.2, 0.2, 0.2, bloodDust);

                    // Урон
                    target.setNoDamageTicks(0);
                    JJKDamage.causeAbilityDamage(target, player, 4.0);
                }

                // Эффект "Тряски" для эпичности
                player.setVelocity(new Vector(0, 0.05, 0));
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void executeBrutalFinisher(Player player, LivingEntity target) {
        World world = target.getWorld();
        Location loc = target.getLocation().add(0, 1, 0);

        // ЗВУК: Огромный взрыв физической силы
        world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        // ВИЗУАЛ: Разлетающиеся куски земли и "взрыв" крови
        world.spawnParticle(Particle.BLOCK, loc, 120, 0.5, 0.5, 0.5, 0.2, Material.REDSTONE_BLOCK.createBlockData());
        world.spawnParticle(Particle.BLOCK, loc, 60, 0.6, 0.2, 0.6, 0.15, Material.DIRT.createBlockData());
        world.spawnParticle(Particle.FLASH, loc, 2, 0, 0, 0, 0);

        // ОТКИДЫВАНИЕ: Тоджи буквально выбивает цель из пространства
        Vector launch = player.getLocation().getDirection().normalize().multiply(3.2).setY(0.5);
        target.setVelocity(launch);

        // Финальный урон
        JJKDamage.causeAbilityDamage(target, player, 4.0);

        player.sendActionBar("§8§l[ §f§lDOMINATED §8§l]");
    }

    private LivingEntity findSingleTarget(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                0.8, // Немного увеличил хитбокс поиска
                e -> e instanceof LivingEntity && !e.equals(player)
        );
        return (result != null && result.getHitEntity() != null) ? (LivingEntity) result.getHitEntity() : null;
    }
}
