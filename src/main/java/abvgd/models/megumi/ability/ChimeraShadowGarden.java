package abvgd.models.megumi.ability;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.manage.PlayerManager;
import org.bukkit.Material;

import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class ChimeraShadowGarden extends JJKDomain {

    public ChimeraShadowGarden() {
        super(new JJKAbilityInfo(
                "§8§lChimera Shadow Garden",
                Material.BLACK_DYE,
                0, 10, 0, false
        ));
    }

    @Override public int getCastTicks() { return 30; }
    @Override public int getDomainTicks() { return 300; }
    @Override public int getBurnoutTicks() { return 600; }
    @Override public double getRadius() { return 18.0; } // Увеличим радиус, раз нет барьера

    @Override public boolean hasBarrier() { return false; } // ОТКРЫТАЯ ТЕРРИТОРИЯ
    @Override public Material getFloorMaterial() { return Material.BLACK_CONCRETE; }

    @Override
    public Material getBarrierMaterial() {
        return null;
    }

    @Override
    protected void onCastTick(Player caster, int tick) {
        if (tick == 0) {
            caster.sendMessage("§8§l[JJK] §7Тень разливается...");
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 2f, 0.5f);
            playCustomSound("jjk.megumi.domain", caster, 0.5f, 1.0f);
        }
        caster.getWorld().spawnParticle(Particle.SQUID_INK, caster.getLocation(), 15, 2, 0.1, 2, 0.05);
    }

    @Override
    protected void onExpand(Player caster, Location center) {
        caster.sendMessage("§0§lСАД ТЕНЕЙ ХИМЕРЫ (НЕЗАВЕРШЕННЫЙ)");
        center.getWorld().playSound(center, Sound.AMBIENT_CAVE, 3f, 0.5f);
    }

    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        // --- ЭФФЕКТ НЕСТАБИЛЬНОСТИ ---
        // Каждые 40 тиков территория "мерцает": частицы на миг исчезают
        if (tick % 40 < 5) return;

        // Отрисовка "Живого пола"
        for (int i = 0; i < 15; i++) {
            double r = Math.random() * getRadius();
            double angle = Math.random() * 2 * Math.PI;
            Location pLoc = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
            center.getWorld().spawnParticle(Particle.SQUID_INK, pLoc, 5, 0.5, 0, 0.5, 0.01);
        }
    }

    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        // 1. МЕХАНИКА "УТОПЛЕНИЯ"
        // Тянем жертву вниз, имитируя погружение в вязкую тень
        Vector sink = new Vector(0, -0.15, 0);
        victim.setVelocity(victim.getVelocity().add(sink));

        // Эффект погружения по колено (замедление + невозможность прыжка)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 6, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 200, false, false)); // Запрет прыжка

        // 2. УКУСЫ ИЗ ТЕНИ
        if (tick % 15 == 0) {
            Location vLoc = victim.getLocation();
            vLoc.getWorld().playSound(vLoc, Sound.ENTITY_WOLF_BIG_PANT, 0.8f, 0.5f);
            vLoc.getWorld().spawnParticle(Particle.BLOCK, vLoc, 10, Material.BLACK_CONCRETE.createBlockData());
            JJKDamage.causeAbilityDamage(victim, caster, 5.0);
        }

        // 3. ВИЗУАЛ УТОПЛЕНИЯ
        if (tick % 10 == 0) {
            victim.getWorld().spawnParticle(Particle.SQUID_INK, victim.getLocation().add(0, 0.2, 0), 15, 0.3, 0.1, 0.3, 0.02);
        }

        // Burnout для жертв
        if (victim instanceof Player p) {
            PlayerManager.get(p).setBurnout(40);
        }
    }

    @Override
    protected void onEnd(Player caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 2f, 0.5f);
        caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, caster.getLocation(), 100, 8, 1, 8, 0.05);
    }
}
