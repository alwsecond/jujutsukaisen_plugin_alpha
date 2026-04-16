package abvgd.models.gojo;

import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.DashAbility;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GojoDash extends DashAbility {

    private final Color blueColor = Color.fromRGB(0, 160, 255); // Чистый небесно-синий
    private final Color whiteColor = Color.fromRGB(250, 250, 250); // Белоснежный

    public GojoDash() {
        super(new JJKAbilityInfo(
                "§f§lBlink",
                Material.LAPIS_LAZULI,
                0,
                30, // кд на способность в тиках
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        double range = 10;
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        // Твоя оригинальная логика луча (raytrace)
        RayTraceResult ray = world.rayTraceBlocks(start, direction, range, FluidCollisionMode.NEVER, true);

        Location target;
        if (ray != null && ray.getHitBlock() != null) {
            // Отступаем от стены чуть больше (0.6), чтобы точно не застрять
            target = ray.getHitPosition().toLocation(world).subtract(direction.clone().multiply(0.6));
        } else {
            target = start.clone().add(direction.clone().multiply(range));
        }

        // --- ЭФФЕКТЫ НА СТАРТЕ ---
        // Звуки: звон кондуита и всплеск воды (дает эффект "чистоты")
        world.playSound(start, Sound.BLOCK_CONDUIT_ACTIVATE, 1.5f, 2.0f);
        world.playSound(start, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.5f);

        // Сине-белый силуэт (образ) на старте
        Particle.DustOptions blueDust = new Particle.DustOptions(blueColor, 1.8f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(whiteColor, 1.2f);

        world.spawnParticle(Particle.DUST, start, 25, 0.3, 0.6, 0.3, blueDust);
        world.spawnParticle(Particle.WHITE_ASH, start, 15, 0.2, 0.5, 0.2, 0.02);
        world.spawnParticle(Particle.SNOWFLAKE, start, 10, 0.4, 0.4, 0.4, 0.05);

        // --- ТЕЛЕПОРТАЦИЯ ---
        target.setDirection(direction);
        player.teleport(target);

        // --- ЭФФЕКТЫ В ТОЧКЕ ПРИБЫТИЯ ---
        Location endPos = target.clone().add(0, 1, 0); // Центр тела игрока

        // Звук резкого схлопывания пространства
        world.playSound(target, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.8f);
        world.playSound(target, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 2.0f);

        // Вспышка и круговая волна синего цвета
        world.spawnParticle(Particle.FLASH, endPos, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.DUST, endPos, 15, 0.4, 0.4, 0.4, whiteDust);

        for (int i = 0; i < 10; i++) {
            double angle = i * (Math.PI * 2 / 10);
            double x = Math.cos(angle) * 1.2;
            double z = Math.sin(angle) * 1.2;
            world.spawnParticle(Particle.DUST, endPos.clone().add(x, 0, z), 2, 0, 0, 0, blueDust);
        }
    }
}