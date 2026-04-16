package abvgd.models.hakari;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HakariInteract extends InteractAbility {

    public HakariInteract() {
        super(new JJKAbilityInfo("Double Shutter", Material.IRON_TRAPDOOR, 0, 20, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        // Позиция щита (чуть дальше, чтобы зазор был виден)
        Location centerLoc = player.getEyeLocation().add(dir.multiply(1.8)).subtract(0, 0.4, 0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 25, 3, false, false, false));
        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
        // 1. ЗВУКИ: Тяжелый лязг и гидравлика
        world.playSound(centerLoc, Sound.BLOCK_IRON_DOOR_CLOSE, 2f, 0.5f);
        world.playSound(centerLoc, Sound.BLOCK_PISTON_EXTEND, 1.5f, 0.5f);

        // 2. ОТРИСОВКА ДВУХ СТВОРОК (Длится 1.2 сек / 24 тика)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 24) {
                    this.cancel();
                    return;
                }
                // Рисуем левую и правую створку с зазором 0.3 блока
                drawShutterLeaf(centerLoc.clone().add(side.clone().multiply(0.75)), side, true);
                drawShutterLeaf(centerLoc.clone().subtract(side.clone().multiply(0.75)), side, false);
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);

        // 3. МОЩНОЕ ОТТАЛКИВАНИЕ (Широкая область перед игроком)
        for (Entity e : world.getNearbyEntities(centerLoc, 3.0, 2.0, 3.0)) {
            if (e instanceof LivingEntity victim && e != player) {
                // Эффект удара об металл
                world.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.5f, 1.8f);
                world.spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1, 0), 1);

                // Импульс: Отбрасываем сильно назад и немного вверх
                Vector push = dir.clone().multiply(1.8).setY(0.4);
                victim.setVelocity(push);

                JJKDamage.causeAbilityDamage(victim, player, 8.0);
            }
        }
    }

    private void drawShutterLeaf(Location loc, Vector side, boolean isLeft) {
        double w = 1.1; // Ширина одной створки
        double h = 2.6; // Увеличенная высота

        Particle.DustOptions iron = new Particle.DustOptions(Color.fromRGB(190, 190, 200), 1.0f);
        Particle.DustOptions frame = new Particle.DustOptions(Color.fromRGB(40, 40, 45), 1.2f);

        for (double y = -h/2; y <= h/2; y += 0.3) {
            for (double s = -w/2; s <= w/2; s += 0.3) {
                // Рисуем плоскость перед игроком
                Location pLoc = loc.clone().add(side.clone().multiply(s)).add(0, y, 0);

                // Рамка по краям каждой створки
                boolean isEdge = Math.abs(y) > 1.1 || Math.abs(s) > 0.45;
                loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, isEdge ? frame : iron);
            }
        }
    }
}