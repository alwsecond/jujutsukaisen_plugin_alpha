package abvgd.models.naoya.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class HumanCellPalace extends JJKDomain {

    public HumanCellPalace() {
        super(new JJKAbilityInfo(
                "§3§lHuman Cell Palace",
                Material.CYAN_WOOL,
                0, 10, 0, false // Тестовые значения ПЭ и КД
        ));
    }

    @Override public int getCastTicks() { return 35; }
    @Override public int getDomainTicks() { return 240; }
    @Override public int getBurnoutTicks() { return 500; }
    @Override public double getRadius() { return 14.0; }

    @Override public boolean hasBarrier() { return true; }
    @Override public Material getFloorMaterial() { return Material.LIGHT_GRAY_CONCRETE; }
    @Override public Material getBarrierMaterial() { return Material.CYAN_STAINED_GLASS; }

    @Override
    protected void onCastTick(Player caster, int tick) {
        if (tick == 0) {
            caster.sendMessage("§3§l[JJK] §bРасширение территории...");
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_PLACE, 2f, 0.5f);
        }
        // Эффект собирающихся стекол
        if (tick % 2 == 0) {
            caster.getWorld().spawnParticle(Particle.BLOCK, caster.getLocation().add(0, 1, 0), 15, 3, 3, 3, Material.WHITE_STAINED_GLASS.createBlockData());
        }
    }

    @Override
    protected void onExpand(Player caster, Location center) {
        caster.sendMessage("§b§lДВОРЕЦ ЧЕЛОВЕЧЕСКИХ КЛЕТОК");
        caster.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 5f, 0.5f);
        caster.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 2f, 2f);
    }

    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        // Спавним "Осколки реальности" - большие стеклянные панели, которые медленно вращаются
        if (tick % 10 == 0) {
            Location loc = center.clone().add((Math.random()-0.5)*20, Math.random()*5, (Math.random()-0.5)*20);
            center.getWorld().spawn(loc, BlockDisplay.class, b -> {
                b.setBlock(Material.CYAN_STAINED_GLASS.createBlockData());
                Transformation t = b.getTransformation();
                t.getScale().set(3.0f, 0.1f, 3.0f); // Огромные плоские зеркала
                b.setTransformation(t);
                b.setInterpolationDuration(40);

                // Удаляем через 2 секунды
                Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), () -> {
                    b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation(), 10, Material.WHITE_STAINED_GLASS.createBlockData());
                    b.remove();
                }, 40L);
            });
        }
    }

    // --- ГАРАНТИРОВАННОЕ ПОПАДАНИЕ (SURE-HIT) ---
    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        // 1. ПРОВЕРКА ДВИЖЕНИЯ (Суть техники)
        // Если жертва пытается идти — она ломается
        Vector velocity = victim.getVelocity();
        if (velocity.length() > 0.05) {
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 0.5f);
            victim.getWorld().spawnParticle(Particle.BLOCK, victim.getEyeLocation(), 20, Material.REDSTONE_BLOCK.createBlockData());
            JJKDamage.causeAbilityDamage(victim, caster, 2); // Повышенный урон за движение
        }

        // 2. ЭФФЕКТ "СЛОЕВ" (Визуал разреза)
        if (tick % 15 == 0) {
            // Спавним тонкое красное стекло ПРЯМО ВНУТРИ жертвы
            victim.getWorld().spawn(victim.getLocation().add(0, 1, 0), BlockDisplay.class, b -> {
                b.setBlock(Material.RED_STAINED_GLASS.createBlockData());
                Transformation t = b.getTransformation();
                t.getScale().set(2.0f, 0.01f, 2.0f); // Тончайший срез
                b.setTransformation(t);

                // Удаляем через 5 тиков (мерцание)
                Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), b::remove, 5L);
            });

            // Звук биологического "сбоя"
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        }

        // 3. СТАТИЧНЫЕ ФАНТОМЫ (Копии жертвы)
        if (tick % 60 == 0) {
            // Создаем стеклянный силуэт там, где жертва стояла секунду назад
            BlockDisplay shadow = victim.getWorld().spawn(victim.getLocation(), BlockDisplay.class, s -> {
                s.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
                Transformation t = s.getTransformation();
                t.getScale().set(0.6f, 1.8f, 0.6f); // Размер игрока
                s.setTransformation(t);
            });
            Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), shadow::remove, 40L);
        }

        // Замедляем и "ослепляем" (эффект шока)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 5, false, false));
        victim.setFreezeTicks(40);

    }

    @Override
    protected void onEnd(Player caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 3f, 0.1f);
        caster.getWorld().spawnParticle(Particle.BLOCK, caster.getLocation(), 200, 5, 5, 5, Material.WHITE_STAINED_GLASS.createBlockData());
    }
}
