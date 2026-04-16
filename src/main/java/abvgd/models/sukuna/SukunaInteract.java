package abvgd.models.sukuna;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SukunaInteract extends InteractAbility {

    private final Color sukunaRed = Color.fromRGB(120, 0, 0);

    public SukunaInteract() {
        super(new JJKAbilityInfo(
                "§4§lKing's Aura",
                Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                0, 0, 0.0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();

        // 1. Резист III на 1.5 секунды
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 2, false, false, true));

        // 2. Звук: Тяжелый низкий гул (как включение мощи)
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f);

        // 3. Визуальный цикл ауры
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 30 || !player.isOnline()) { // Длится ровно время резиста
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();

                // Тонкие вертикальные потоки энергии вокруг игрока
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double x = Math.cos(angle) * 0.7;
                    double z = Math.sin(angle) * 0.7;

                    // Темно-красные искры, поднимающиеся вверх
                    world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 1, 0,
                            new Particle.DustOptions(sukunaRed, 1.2f));

                    // Редкий темный дым (искажение)
                    if (t % 5 == 0) {
                        world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(x, 1, z), 1, 0, 0.1, 0, 0.02);
                    }
                }

                // Эффект "пульсации" у ног
                if (t % 10 == 0) {
                    world.spawnParticle(Particle.WHITE_ASH, loc.add(0, 0.1, 0), 10, 0.5, 0.1, 0.5, 0.01);
                }

                t++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }
}