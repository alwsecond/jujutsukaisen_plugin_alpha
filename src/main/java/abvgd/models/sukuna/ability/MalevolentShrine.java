package abvgd.models.sukuna.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MalevolentShrine extends JJKDomain {

    public MalevolentShrine() {
        super(new JJKAbilityInfo(
                "§4§lMalevolent Shrine",
                Material.NETHER_BRICKS,
                25, // Мастерство
                40, // Кулдаун (минуты/сек в зависимости от ядра)
                0,
                false
        ));
    }

    // --- НАСТРОЙКИ ---
    @Override public int getCastTicks() { return 40; } // Чуть дольше каст для пафоса
    @Override public int getDomainTicks() { return 220; } // 11 секунд резни
    @Override public int getBurnoutTicks() { return 500; }
    @Override public double getRadius() { return 20.0; } // Открытая территория обычно больше

    @Override public boolean hasBarrier() { return false; } // ОТКРЫТАЯ ТЕРРИТОРИЯ
    @Override public Material getFloorMaterial() { return Material.NETHER_BRICKS; }
    @Override public Material getBarrierMaterial() { return null; } // Барьера нет

    // --- АНИМАЦИЯ КАСТА ---
    @Override
    protected void onCastTick(Player caster, int tick) {
        World world = caster.getWorld();
        Location loc = caster.getLocation();

        if (tick == 0) {
            playCustomSound("jjk.sukuna.domain", caster, 1.0f, 1.0f);
            caster.sendMessage("§4§l[JJK] §7Ритм проклятого чрева...");
            world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 2f, 0.5f);
        }

        // Эффект темной ауры, стягивающейся к Сукуне
        if (tick % 2 == 0) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 20, 3, 0.1, 3, 0.05);
            world.spawnParticle(Particle.LARGE_SMOKE, loc, 5, 1, 2, 1, 0.02);
        }

        // Звук нарастающего гула
        if (tick % 10 == 0) {
            world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f + (tick * 0.01f));
        }
    }

    // --- МОМЕНТ РАСКРЫТИЯ ---
    @Override
    protected void onExpand(Player caster, Location center) {
        caster.sendMessage("§0§lГРОБНИЦА ЗЛА");
        World world = caster.getWorld();

        // Мощнейший визуальный и звуковой хлопок
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 5f, 0.5f);
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2f, 0.5f);
        world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3f, 0.1f);

        // Всплеск "крови" по всему радиусу при раскрытии
        world.spawnParticle(Particle.BLOCK, center, 500, getRadius(), 1, getRadius(), Material.REDSTONE_BLOCK.createBlockData());
    }

    // --- ФОНОВЫЕ ЭФФЕКТЫ (АД) ---
    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        World world = center.getWorld();

        // 1. Кровавый туман
        if (tick % 5 == 0) {
            world.spawnParticle(Particle.DUST, center, 100, getRadius(), 5, getRadius(),
                    new Particle.DustOptions(Color.fromRGB(100, 0, 0), 2.0f));
        }

        // 2. Случайные "вспышки" разрезов в воздухе по всей территории
        if (tick % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                double rx = (Math.random() - 0.5) * (getRadius() * 2);
                double rz = (Math.random() - 0.5) * (getRadius() * 2);
                Location slashLoc = center.clone().add(rx, Math.random() * 5, rz);

                world.spawnParticle(Particle.SWEEP_ATTACK, slashLoc, 1, 0.5, 0.5, 0.5, 0);
                if (tick % 10 == 0) world.playSound(slashLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
            }
        }
    }

    // --- ГАРАНТИРОВАННОЕ ПОПАДАНИЕ (БЕСКОНЕЧНЫЕ РАЗРЕЗЫ) ---
    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        World world = victim.getWorld();
        Location vLoc = victim.getEyeLocation();
        Location center = caster.getLocation();
        double distance = victim.getLocation().distance(center);

        Vector velocity = victim.getVelocity();
        victim.setVelocity(new Vector(0, velocity.getY() < 0 ? velocity.getY() : -0.1, 0));

        if (distance > getRadius()) {
            Vector toCenter = center.toVector().subtract(victim.getLocation().toVector()).normalize();
            victim.teleport(victim.getLocation().add(toCenter.multiply(1.5)));

            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        }

        if (victim instanceof Mob mob) {
            mob.setTarget(null); // Сбрасываем цель, чтобы не бежали атаковать
        }

        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, false));

        if (tick % 2 == 0) {
            world.spawnParticle(Particle.SWEEP_ATTACK, vLoc, 1, 0.3, 0.3, 0.3, 0);
            world.spawnParticle(Particle.BLOCK, vLoc, 10, 0.2, 0.2, 0.2, Material.REDSTONE_BLOCK.createBlockData());
        }

        // Звук шинковки
        if (tick % 4 == 0) {
            world.playSound(vLoc, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.8f, 1.8f);
        }

        // УРОН: Сукуна бьет чаще и больнее
        if (tick % 5 == 0) {
            // Наносим урон, игнорируя броню (эффект Cleave)
            JJKDamage.causeAbilityDamage(victim, caster, 2.0);
            victim.setNoDamageTicks(0);
        }
    }

    // --- ЗАВЕРШЕНИЕ ---
    @Override
    protected void onEnd(Player caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);
        caster.sendMessage("§4§lГробница исчезает...");
    }
}
