package abvgd.models.mahito.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class SoulHammer extends ActiveAbility {

    public SoulHammer() {
        super(new JJKAbilityInfo("Soul Hammer", Material.NETHER_WART_BLOCK, 0, 35, 0, false));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location startLoc = player.getEyeLocation().add(dir.clone().multiply(0.5));
        float yaw = player.getLocation().getYaw();

        // 1. ЗВУКИ НАЧАЛА МУТАЦИИ
        world.playSound(startLoc, Sound.BLOCK_SLIME_BLOCK_STEP, 1.5f, 0.5f);
        world.playSound(startLoc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.5f);

        // 2. СОЗДАЕМ ТЕЛО МОЛОТА (Тентакль-рукоять)
        BlockDisplay handle = world.spawn(startLoc, BlockDisplay.class, b -> {
            b.setBlock(Material.NETHER_WART_BLOCK.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(0.4f, 0.4f, 0.4f); // Начинаем как комок
            b.setTransformation(t);
            b.setInterpolationDuration(3);
            b.setInterpolationDelay(0);
        });

        // 3. СОЗДАЕМ УДАРНУЮ ЧАСТЬ
        BlockDisplay head = world.spawn(startLoc, BlockDisplay.class, b -> {
            b.setBlock(Material.CRIMSON_NYLIUM.createBlockData());
            Transformation t = b.getTransformation();
            t.getScale().set(0.6f, 0.6f, 0.6f);
            b.setTransformation(t);
            b.setInterpolationDuration(3);
            b.setInterpolationDelay(0);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Эффект слизи в полете
                world.spawnParticle(Particle.ITEM_SLIME, handle.getLocation(), 3, 0.1, 0.1, 0.1, 0.05);

                if (ticks == 2) { // РЕЗКОЕ ВЫТЯГИВАНИЕ (Мутация в молот)
                    world.playSound(player.getLocation(), Sound.ENTITY_SLIME_ATTACK, 1.5f, 0.7f);

                    Location targetImpact = player.getLocation().add(dir.clone().multiply(4.5));
                    Location handlePos = player.getLocation().add(dir.clone().multiply(2.5)).add(0, 0.5, 0);

                    // Рукоять удлиняется
                    mutate(handle, handlePos, yaw, new Vector3f(0.3f, 3.5f, 0.3f), 4);
                    // Голова раздувается
                    mutate(head, targetImpact, yaw, new Vector3f(1.8f, 1.8f, 1.8f), 4);
                }

                if (ticks == 7) { // ФИНАЛЬНЫЙ УДАР ОБ ЗЕМЛЮ
                    applyOrganicImpact(head.getLocation(), player);
                    handle.remove();
                    head.remove();
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    private void mutate(BlockDisplay b, Location loc, float yaw, Vector3f scale, int duration) {
        b.setInterpolationDuration(duration);
        b.setInterpolationDelay(0);
        b.setRotation(yaw, 0);
        b.teleport(loc);

        Transformation t = b.getTransformation();
        t.getScale().set(scale);
        // Наклон для удара сверху вниз
        t.getLeftRotation().set(0.6f, 0, 0, 0.8f);
        b.setTransformation(t);
    }

    private void applyOrganicImpact(Location loc, Player caster) {
        World world = loc.getWorld();

        // Звуки "разрыва" и тяжелого приземления слизи
        world.playSound(loc, Sound.BLOCK_SLIME_BLOCK_BREAK, 2f, 0.5f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.5f);

        // Частицы: слизь + мясо + взрыв
        world.spawnParticle(Particle.ITEM_SLIME, loc, 60, 1.5, 0.5, 1.5, 0.1);
        world.spawnParticle(Particle.BLOCK, loc, 80, 1.5, 0.5, 1.5, Material.NETHER_WART_BLOCK.createBlockData());
        world.spawnParticle(Particle.EXPLOSION, loc, 1);

        for (Entity e : world.getNearbyEntities(loc, 4, 3, 4)) {
            if (e instanceof LivingEntity victim && e != caster) {
                JJKDamage.causeAbilityDamage(victim, caster, 22.0);

                // Врага приклеивает к земле (Сильная медлительность)
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));

                Vector v = victim.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(0.3);
                v.setY(-1.5); // Вбиваем в пол
                victim.setVelocity(v);
            }
        }
    }
}

