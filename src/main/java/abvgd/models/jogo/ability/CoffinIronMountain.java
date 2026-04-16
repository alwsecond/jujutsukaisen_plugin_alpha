package abvgd.models.jogo.ability;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class CoffinIronMountain extends JJKDomain {

    public CoffinIronMountain() {
        super(new JJKAbilityInfo(
                "§6§lCoffin of the Iron Mountain",
                Material.MAGMA_BLOCK,
                25, 36, 0, false
        ));
    }

    // --- НАСТРОЙКИ ---
    @Override public int getCastTicks() { return 35; }
    @Override public int getDomainTicks() { return 200; }
    @Override public int getBurnoutTicks() { return 0; }
    @Override public double getRadius() { return 15.0; }

    @Override public boolean hasBarrier() { return true; }
    @Override public Material getFloorMaterial() { return Material.MAGMA_BLOCK; }
    @Override public Material getBarrierMaterial() { return Material.BASALT; }

    // --- АНИМАЦИЯ КАСТА ---
    @Override
    protected void onCastTick(Player caster, int tick) {
        if (tick == 0) {
            caster.sendMessage("§6§l[JJK] §eРасширение территории...");
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 2f, 0.5f);
            playCustomSound("jjk.jogo.domain", caster, 0.6f, 1.0f);
        }
        // Эффект поднимающегося пепла и жара
        if (tick % 2 == 0) {
            caster.getWorld().spawnParticle(Particle.LAVA, caster.getLocation().add(0, 1, 0), 5, 1, 1, 1, 0.1);
            caster.getWorld().spawnParticle(Particle.FLAME, caster.getLocation(), 10, 2, 0.1, 2, 0.05);
        }
    }

    // --- МОМЕНТ РАСКРЫТИЯ ---
    @Override
    protected void onExpand(Player caster, Location center) {
        caster.sendMessage("§4§lГРОБНИЦА ЖЕЛЕЗНОЙ ГОРЫ");
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 5f, 0.5f);
        center.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 3f, 0.1f);

        // Вспышка магмы при открытии
        center.getWorld().spawnParticle(Particle.FLAME, center, 500, 5, 5, 5, 0.5);
    }

    // --- ФОНОВЫЕ ЭФФЕКТЫ (ВУЛКАН) ---
    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        // Падающие угли
        if (tick % 3 == 0) {
            center.getWorld().spawnParticle(Particle.SMALL_FLAME, center, 50, getRadius(), 8, getRadius(), 0.02);
        }
        // Гул земли и лавы
        if (tick % 40 == 0) {
            center.getWorld().playSound(center, Sound.BLOCK_LAVA_POP, 2f, 0.5f);
            createLavaBursts(center);
        }
    }

    protected void createLavaBursts(Location center) {
        for (int i = 0; i < 3; i++) {
            double x = (Math.random() - 0.5) * 20;
            double z = (Math.random() - 0.5) * 20;
            Location randomLoc = center.clone().add(x, -1, z);

            center.getWorld().spawnParticle(Particle.LAVA, randomLoc.add(0, 1, 0), 20, 0.5, 2, 0.5, 0.1);
        }
    }

    // --- ГАРАНТИРОВАННОЕ ПОПАДАНИЕ (SURE-HIT) ---
    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        // Тепловой удар (Замедление от жара)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 2, false, false));

        // Визуальное горение
        if (tick % 5 == 0) {
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        }

        // УРОН (У Джого Sure-hit очень горячий)
        if (tick % 20 == 0) {
            JJKDamage.causeAbilityDamage(victim, caster, 2.0);
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
        }

        // Редкие взрывы под ногами жертвы
        if (tick % 60 == 0) {
            victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation(), 1);
            JJKDamage.causeAbilityDamage(victim, caster, 2.0);
            victim.setVelocity(new Vector(0, 0.4, 0));
        }
    }

    // --- ЗАВЕРШЕНИЕ ---
    @Override
    protected void onEnd(Player caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BASALT_BREAK, 2f, 1f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 0.8f);
    }
}
