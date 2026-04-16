package abvgd.models.yuji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BenevolentShrine extends JJKDomain {

    private final Map<UUID, Double> originalHealthMap = new HashMap<>();

    public BenevolentShrine() {
        super(new JJKAbilityInfo(
                "§6§lBenevolent Shrine",
                Material.WHITE_STAINED_GLASS,
                0, 0, 0, false
        ));
    }

    @Override public int getCastTicks() { return 30; }
    @Override public int getDomainTicks() { return 320; }

    @Override
    public int getBurnoutTicks() {
        return 0;
    }

    @Override public double getRadius() { return 20.0; }
    @Override public boolean hasBarrier() { return true; }
    @Override public Material getFloorMaterial() { return Material.SNOW_BLOCK; }

    @Override
    public Material getBarrierMaterial() {
        return Material.BARRIER;
    }

    @Override
    protected void onCastTick(Player caster, int tick) {
        if (tick % 10 == 0) {
            caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation().add(0, 1, 0), 30, 2, 2, 2, 0);
        }
    }

    @Override
    protected void onExpand(Player caster, Location center) {
        caster.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.5f);
        // Вместо метадаты дадим Юдзи очень сильную силу, но только внутри территории
        caster.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, getDomainTicks(), 10, false, false));
    }

    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        // Эффект "тишины": очень много белого пепла
        center.getWorld().spawnParticle(Particle.WHITE_ASH, center, 100, getRadius(), 5, getRadius(), 0.01);
    }

    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        // 1. БЕСШУМНОЕ СТИРАНИЕ ДУШИ
        AttributeInstance maxHealth = victim.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && tick % 20 == 0) {
            UUID id = victim.getUniqueId();
            if (!originalHealthMap.containsKey(id)) {
                originalHealthMap.put(id, maxHealth.getBaseValue());
            }

            double currentMax = maxHealth.getBaseValue();
            // Стираем по 10% каждую секунду
            double newValue = Math.max(1.0, currentMax * 0.90);
            maxHealth.setBaseValue(newValue);

            // Если ХП стерто почти до конца (меньше 2), подсвечиваем цель
            if (newValue <= 2.0) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
            }

            // Визуал "испарения"
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.5, 0.2, 0);
        }
    }

    @Override
    protected void onEnd(Player caster) {
        caster.removePotionEffect(PotionEffectType.STRENGTH);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.5f);

        // ВОССТАНОВЛЕНИЕ ЧЕРЕЗ 20 СЕКУНД
        new BukkitRunnable() {
            @Override
            public void run() {
                originalHealthMap.forEach((uuid, health) -> {
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity instanceof LivingEntity victim) {
                        AttributeInstance attr = victim.getAttribute(Attribute.MAX_HEALTH);
                        if (attr != null) attr.setBaseValue(health);
                    }
                });
                originalHealthMap.clear();
            }
        }.runTaskLater(JJKPlugin.getInstance(), 400L);
    }
}
