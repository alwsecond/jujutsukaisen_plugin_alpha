package abvgd.models.sukuna.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import abvgd.utils.JJKFunc;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Spiderweb extends ActiveAbility {

    public Spiderweb() {
        super(new JJKAbilityInfo(
                "Spiderweb",
                Material.COBWEB,
                0,
                300,    // Кулдаун 15 секунд
                0.0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location origin = player.getLocation();

        // --- ФАЗА 1: УДАР О ЗЕМЛЮ ---
        // Замораживаем на мгновение для анимации наклона
        JJKFunc.freezePlayer(player, 10);

        // Звук удара по камню и тяжелого давления
        world.playSound(origin, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.5f, 0.5f);
        world.playSound(origin, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);

        // Визуал в точке удара (пыль и трещины)
        world.spawnParticle(Particle.BLOCK, origin, 50, 1, 0.1, 1, 0.1, origin.clone().subtract(0, 0.1, 0).getBlock().getBlockData());
        world.spawnParticle(Particle.EXPLOSION, origin, 2, 0, 0, 0, 0);

        // --- ФАЗА 2: РАСПРОСТРАНЕНИЕ ПАУТИНЫ ---
        // Создаем 8 лучей разрезов в разные стороны
        for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4);
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));

            // Чтобы лучи расходились красиво, можно передать i как задержку
            long delay = 0L; // Сделай (long)i, если хочешь задержку между лучами

            new BukkitRunnable() {
                int currentStep = 0; // Называем иначе, чтобы не путать
                final int maxSteps = 10;
                Location currentLoc = origin.clone();

                @Override
                public void run() {
                    if (currentStep >= maxSteps) {
                        this.cancel();
                        return;
                    }

                    // Двигаем "разрез" вперед
                    currentLoc.add(direction);

                    // Улучшенная адаптация под рельеф
                    if (currentLoc.getBlock().getType().isSolid()) {
                        currentLoc.add(0, 1, 0);
                    } else {
                        // Если под нами воздух — спускаемся
                        for (int fall = 0; fall < 3; fall++) {
                            if (currentLoc.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                                currentLoc.subtract(0, 1, 0);
                            } else {
                                break;
                            }
                        }
                    }

                    renderSpiderLine(currentLoc);
                    damageEntitiesAt(player, currentLoc);

                    currentStep++;
                }
            }.runTaskTimer(JJKPlugin.getInstance(), delay, 1); // Исправлено: 0L или delay
        }
    }

    private void renderSpiderLine(Location loc) {
        World world = loc.getWorld();
        // Тонкие черные и красные частицы (разрезы в земле)
        Particle.DustOptions black = new Particle.DustOptions(Color.BLACK, 1.2f);
        world.spawnParticle(Particle.DUST, loc.clone().add(0, 0.1, 0), 5, 0.2, 0, 0.2, black);
        world.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 0.1, 0), 1, 0, 0, 0, 0);

        // Звук треска земли
        if (Math.random() > 0.5) {
            world.playSound(loc, Sound.BLOCK_NETHERITE_BLOCK_STEP, 0.8f, 0.5f);
        }
    }

    private void damageEntitiesAt(Player player, Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 2.0, 1.5)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                // Звук "Дзин" при попадании разреза в цель
                loc.getWorld().playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.5f, 1.8f);

                // Эффект крови
                loc.getWorld().spawnParticle(Particle.BLOCK, le.getLocation().add(0, 1, 0), 20, 0.1, 0.3, 0.1, Material.REDSTONE_BLOCK.createBlockData());

                // Урон и подкидывание (врага подбрасывает от "взрыва" земли под ним)
                JJKDamage.causeAbilityDamage(le, player, 14.0);
                le.setNoDamageTicks(0);
                le.setVelocity(new Vector(0, 0.45, 0));

                // Кратковременный стан
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 5));
            }
        }
    }
}
