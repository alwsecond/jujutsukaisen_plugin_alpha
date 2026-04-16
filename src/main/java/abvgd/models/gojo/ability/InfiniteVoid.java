package abvgd.models.gojo.ability;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import org.bukkit.entity.*;

public class InfiniteVoid extends JJKDomain {

    public InfiniteVoid() {
        super(new JJKAbilityInfo(
                "§f§lInfinite Void",
                Material.NETHER_STAR,
                25, 36, 0, false
        ));
    }

    // --- НАСТРОЙКИ ---
    @Override public int getCastTicks() { return 30; }
    @Override public int getDomainTicks() { return 240; }
    @Override public int getBurnoutTicks() { return 400; } // 20 сек выгорания
    @Override public double getRadius() { return 16.0; }

    @Override public boolean hasBarrier() { return true; } // Закрытая территория
    @Override public Material getFloorMaterial() { return Material.BLACK_CONCRETE; }
    @Override public Material getBarrierMaterial() { return Material.BLACK_CONCRETE; }

    // --- АНИМАЦИЯ КАСТА ---
    @Override
    protected void onCastTick(Player caster, int tick) {
        if (tick == 0) {
            caster.sendMessage("§b§l[JJK] §fРасширение территории...");
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 0.5f);
            playCustomSound("jjk.gojo.domain", caster, 0.5f, 1.0f);
        }
        // Частицы "всасывания"
        if (tick % 3 == 0) {
            caster.getWorld().spawnParticle(Particle.REVERSE_PORTAL, caster.getLocation().add(0, 1, 0), 10, 2, 2, 2, 0.1);
        }
    }

    // --- МОМЕНТ РАСКРЫТИЯ ---
    @Override
    protected void onExpand(Player caster, Location center) {
        caster.sendMessage("§9§lНЕОБЪЯТНАЯ ПУСТОТА");
        caster.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 5f, 1f);
        caster.getWorld().playSound(center, Sound.BLOCK_BELL_RESONATE, 3f, 0.1f);
    }

    // --- ФОНОВЫЕ ЭФФЕКТЫ (КОСМОС) ---
    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        // Звезды
        if (tick % 5 == 0) {
            center.getWorld().spawnParticle(Particle.END_ROD, center, 40, getRadius(), getRadius(), getRadius(), 0.01);
        }
        if (tick % 20 == 0) {
            createWhiteClouds(center); createWhiteClouds(center); createWhiteClouds(center);
        }
    }

    protected void createWhiteClouds(Location center) {
        double x = (Math.random() - 0.5) * 20; // 20 block range
        double y = (Math.random() - 0.5) * 20;
        double z = (Math.random() - 0.5) * 20;
        Location randomLoc = center.clone().add(x, y, z);

        center.getWorld().spawnParticle(Particle.CLOUD, randomLoc, 60, 1, 1, 1, 0);
    }

    // --- ГАРАНТИРОВАННОЕ ПОПАДАНИЕ (SURE-HIT) ---
    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        // Полный паралич (Стан)
        victim.setVelocity(new Vector(0, -0.5, 0)); // Прижимаем к полу
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 1, false, false));

        // Эффект перегрузки (Вспышки в глазах)
        if (tick % 10 == 0) {
            victim.getWorld().spawnParticle(Particle.FLASH, victim.getEyeLocation(), 1, 0, 0, 0, 0);
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);

            // Если враг - маг, жжем ему техники на 5 сек
            if (victim instanceof Player p) {
                PlayerManager.get(p).setBurnout(100);
            }
        }

        // Урон
        if (tick % 20 == 0) {
            JJKDamage.causeAbilityDamage(victim, caster, 6.0);
        }
    }

    // --- ЗАВЕРШЕНИЕ ---
    @Override
    protected void onEnd(Player caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 2f, 1f);
    }
}