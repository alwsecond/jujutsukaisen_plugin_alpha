package abvgd.models.yuji;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class YujiInteract extends InteractAbility {

    private final Color yujiBlue = Color.fromRGB(0, 191, 255);

    public YujiInteract() {
        super(new JJKAbilityInfo(
                "§6§lTicks Interact",
                Material.BLACK_DYE,
                0, 10, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        if (player.hasMetadata("BF_Perfect_Window")) {
            int level = player.getMetadata("BF_Rhythm_Level").get(0).asInt();

            // Заряжаем кулак
            player.setMetadata("BlackFlashActive", new FixedMetadataValue(JJKPlugin.getInstance(), true));
            player.removeMetadata("BF_Perfect_Window", JJKPlugin.getInstance()); // Чтобы нельзя было нажать F дважды за один цикл

            player.sendActionBar("§b§lЗАРЯЖЕНО! §f(Ударь врага)");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f + (level * 0.3f));
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1.2, 0), 15, 0.2, 0.2, 0.2, 0.05);
        } else if (player.hasMetadata("BF_Rhythm_Level")) {
            // Нажал раньше времени
            player.removeMetadata("BF_Rhythm_Level", JJKPlugin.getInstance());
            player.sendActionBar("§8§lРАНО!");
        }
    }
}