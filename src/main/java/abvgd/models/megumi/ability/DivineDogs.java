package abvgd.models.megumi.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class DivineDogs extends ActiveAbility {

    public DivineDogs() {
        super(new JJKAbilityInfo(
                "Divine Dogs",
                Material.BONE,
                0, 10, 0, false // Тестовые КД 10 и 0 ПЭ
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location spawnLoc = player.getLocation();

        // Эффект тени при призыве
        world.spawnParticle(Particle.SQUID_INK, spawnLoc, 50, 1, 0.1, 1, 0.05);
        world.playSound(spawnLoc, Sound.ENTITY_WOLF_ANGRY_GROWL, 1f, 1f);

        // Призываем двух собак
        spawnDog(player, "§8Black Dog", Color.BLACK);
        spawnDog(player, "§fWhite Dog", Color.WHITE);
    }

    private void spawnDog(Player owner, String name, Color color) {
        Wolf dog = (Wolf) owner.getWorld().spawnEntity(owner.getLocation(), EntityType.WOLF);

        dog.setCustomName(name);
        dog.setCustomNameVisible(true);
        dog.setOwner(owner);
        dog.setAngry(true);
        dog.setMetadata("DivineDog", new FixedMetadataValue(JJKPlugin.getInstance(), true)); // Метка

        // Усиливаем статы до максимума
        dog.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(4.0); // Базовый урон
        dog.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);
        dog.setHealth(20.0);

        new BukkitRunnable() {
            int life = 300; // 15 секунд

            @Override
            public void run() {
                if (life <= 0 || !dog.isValid()) {
                    dog.getWorld().spawnParticle(Particle.SQUID_INK, dog.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
                    dog.remove();
                    this.cancel();
                    return;
                }

                // --- МЕХАНИКА ПРЫЖКА (POUNCE) ---
                LivingEntity target = dog.getTarget();
                if (target != null && dog.getLocation().distance(target.getLocation()) > 5) {
                    // Мгновенный перенос к цели из тени
                    dog.getWorld().spawnParticle(Particle.SQUID_INK, dog.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
                    dog.teleport(target.getLocation().subtract(target.getLocation().getDirection().multiply(1.5)));
                    dog.getWorld().playSound(dog.getLocation(), Sound.ENTITY_WOLF_GROWL, 1f, 1.5f);
                }

                life -= 10;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 10L);
    }
}
