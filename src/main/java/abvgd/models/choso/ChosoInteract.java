package abvgd.models.choso;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.InteractAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChosoInteract extends InteractAbility {

    public ChosoInteract() {
        super(new JJKAbilityInfo(
                "Flowing Red Scale",
                Material.REDSTONE,
                0, 15, 0, false // КД 15 секунд
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // 1. ЗВУКОВАЯ АКТИВАЦИЯ (Удары сердца и кипение крови)
        world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 1.2f);
        world.playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1f, 2f);

        // 2. ВИЗУАЛЬНЫЕ ЭФФЕКТЫ
        // Накладываем "метки" Чосо под глазами (частицы пыли)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 160 || !player.isOnline()) { // Длится 8 секунд
                    player.removeMetadata("FlowingRedScale", JJKPlugin.getInstance());
                    this.cancel();
                    return;
                }

                // Частицы крови у глаз игрока
                Location eye = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.3));
                Vector side = new Vector(-player.getLocation().getDirection().getZ(), 0, player.getLocation().getDirection().getX()).normalize().multiply(0.2);

                player.getWorld().spawnParticle(Particle.DUST, eye.clone().add(side), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 0, 0), 0.6f));
                player.getWorld().spawnParticle(Particle.DUST, eye.clone().subtract(side), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 0, 0), 0.6f));

                ticks += 2;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 2L);

        // 3. МЕХАНИКА (Баффы)
        player.setMetadata("FlowingRedScale", new FixedMetadataValue(JJKPlugin.getInstance(), true));

        // Ускорение и Прыгучесть (Чосо становится физически сильнее)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 160, 1, false, false));

        // Вспышка на экране
        world.spawnParticle(Particle.FLASH, player.getEyeLocation(), 1, 0, 0, 0, 0);
        player.sendMessage("§4§l[!] §cСтекающая красная чешуя активирована!");
    }
}
