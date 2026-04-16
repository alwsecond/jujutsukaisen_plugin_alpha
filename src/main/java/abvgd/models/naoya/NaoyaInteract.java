package abvgd.models.naoya;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class NaoyaInteract extends InteractAbility {

    public NaoyaInteract() {
        super(new JJKAbilityInfo(
                "§e§lFrame Trap",
                Material.GLASS_PANE,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();

        RayTraceResult ray = world.rayTraceEntities(start, dir, 6, 1.2, (e) -> !e.equals(player) && e instanceof LivingEntity);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {
            Location freezeLoc = victim.getLocation(); // Точка полной фиксации

            // 1. Создание стеклянного кадра (BlockDisplay)
            BlockDisplay frame = world.spawn(freezeLoc.clone().add(0, 1, 0), BlockDisplay.class, display -> {
                display.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
                Transformation trans = display.getTransformation();
                trans.getScale().set(1.5f, 2.0f, 0.01f);
                trans.getTranslation().set(-0.75f, -1.0f, 0.0f);
                display.setTransformation(trans);
                display.setRotation(player.getLocation().getYaw(), 0);
            });

            world.playSound(freezeLoc, Sound.BLOCK_GLASS_PLACE, 1.5f, 2.0f);

            // 2. Основной цикл: Фиксация и отслеживание удара Наои
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    // Если время вышло, Наоя ушел далеко или цель умерла
                    if (ticks >= 40 || !victim.isValid() || !player.isOnline()) {
                        frame.remove();
                        this.cancel();
                        return;
                    }

                    // ПОЛНАЯ ФИКСАЦИЯ: телепорт в исходную точку каждый тик
                    victim.teleport(freezeLoc);
                    victim.setFallDistance(0); // Чтобы не разбился при отмене телепорта

                    // ПРОВЕРКА УДАРА: Если Наоя подошел ближе чем на 1.8 блока и "замахнулся"
                    // (Либо просто проверка дистанции для упрощения)
                    if (player.getLocation().distance(victim.getLocation()) < 1.8) {
                        breakFrame(player, victim, frame);
                        this.cancel();
                        return;
                    }

                    // Визуал "дрожания" кадра
                    if (ticks % 5 == 0) {
                        world.spawnParticle(Particle.INSTANT_EFFECT, freezeLoc.clone().add(0, 1, 0), 5, 0.4, 0.6, 0.4, 0);
                    }

                    ticks++;
                }
            }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
        }
    }

    private void breakFrame(Player naoya, LivingEntity victim, BlockDisplay frame) {
        World world = victim.getWorld();
        Location loc = victim.getLocation().add(0, 1, 0);

        // Убираем визуал
        frame.remove();

        // Эффекты взрыва стекла
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.2f);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
        world.spawnParticle(Particle.BLOCK, loc, 50, 0.3, 0.5, 0.3, Material.GLASS.createBlockData());
        world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);

        // Наносим большой урон
        JJKDamage.causeAbilityDamage(victim, naoya, 15.0);

        // Резкое отталкивание после разбития
        victim.setVelocity(naoya.getLocation().getDirection().multiply(1.2).setY(0.4));

        naoya.sendActionBar("§e§lFRAME SHATTERED!");
    }
}
