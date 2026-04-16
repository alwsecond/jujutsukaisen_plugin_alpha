package abvgd.models.gojo;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class GojoInteract extends InteractAbility {

    public GojoInteract() {
        super(new JJKAbilityInfo(
                "§1§lInfinity Step",
                Material.LAPIS_LAZULI,
                0,
                10, // КД для теста
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getLocation();
        double radius = 15.0;

        // 1. ИЩЕМ ЦЕЛЬ (Ближайший игрок в радиусе 15 блоков)
        LivingEntity target = null;
        double minDistance = Double.MAX_VALUE;

        for (Entity entity : world.getNearbyEntities(startLoc, radius, radius, radius)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player) && !entity.hasMetadata("Summon")) {
                double dist = entity.getLocation().distance(startLoc);
                if (dist < minDistance) {
                    minDistance = dist;
                    target = victim;
                }
            }
        }

        if (target == null) return; // Нет цели — нет телепорта

        // 2. СОЗДАЕМ ПОСЛЕОБРАЗ НА СТАРОМ МЕСТЕ
        // Создаем "застывший силуэт" из частиц
        Location imageLoc = startLoc.clone().add(0, 1, 0);
        world.spawnParticle(Particle.CLOUD, imageLoc, 50, 0.3, 0.6, 0.3, 0.01);
        world.spawnParticle(Particle.FLASH, imageLoc, 1, 0, 0, 0, 0);

        // Звук разбития стекла на старом месте
        world.playSound(startLoc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.2f);
        world.playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

        // 3. ТЕЛЕПОРТ ЗА СПИНУ
        // Вычисляем точку: локация цели + (вектор взгляда цели, повернутый назад на 1.5 блока)
        Vector targetDir = target.getLocation().getDirection().normalize();
        Location backLoc = target.getLocation().subtract(targetDir.multiply(1.5));

        // Поворачиваем Годжо лицом к цели после телепорта
        backLoc.setDirection(target.getLocation().toVector().subtract(backLoc.toVector()));

        player.teleport(backLoc);

        // 4. ЭФФЕКТЫ НА НОВОМ МЕСТЕ
        world.playSound(backLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        world.spawnParticle(Particle.REVERSE_PORTAL, backLoc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);

        // Послеобраз исчезает через полсекунды (плавно)
        Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), () -> {
            world.spawnParticle(Particle.SMOKE, imageLoc, 10, 0.2, 0.4, 0.2, 0.02);
        }, 10L);
    }
}