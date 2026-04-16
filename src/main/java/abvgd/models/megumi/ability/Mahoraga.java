package abvgd.models.megumi.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Mahoraga extends ActiveAbility {

    public Mahoraga() {
        super(new JJKAbilityInfo(
                "§6§lMahoraga Summon",
                Material.NETHER_STAR,
                0, 1200, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        // Чуть дальше, чтобы камера игрока охватила весь масштаб
        Location summonLoc = player.getLocation().add(player.getLocation().getDirection().multiply(-4.0)).add(0, 0.1, 0);

        player.sendMessage("§8§l[JJK] §fС этим сокровищем я призываю...");

        List<ItemDisplay> cocoonParts = new ArrayList<>();

        // Создаем 8 элементов для более плотного и большого кокона
        for (int i = 0; i < 8; i++) {
            ItemDisplay part = world.spawn(summonLoc, ItemDisplay.class, d -> {
                // Используем панель для эффекта острых нитей
                d.setItemStack(new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
                Transformation t = d.getTransformation();

                // УВЕЛИЧЕННЫЙ РАЗМЕР: 3.5 в ширину и 5.5 в высоту
                t.getScale().set(3.5f, 5.5f, 3.5f);

                // Рандомный поворот для хаотичности
                t.getLeftRotation().set(new Quaternionf().rotateXYZ(
                        (float)(Math.random() * Math.PI),
                        (float)(Math.random() * Math.PI),
                        (float)(Math.random() * Math.PI)
                ));
                d.setTransformation(t);
                d.setInterpolationDuration(80);
            });
            cocoonParts.add(part);
        }

        new BukkitRunnable() {
            int ticks = 0;
            float wheelRotation = 0;
            IronGolem mahoraga = null;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cocoonParts.forEach(Entity::remove);
                    this.cancel();
                    return;
                }

                // ЭТАП 1: Рост и Плетение (увеличенные частицы)
                if (ticks < 120) {
                    // Рисуем нити в большем радиусе
                    if (ticks % 2 == 0) {
                        // Нити теперь стягиваются из радиуса 5 блоков
                        double angle = Math.random() * 2 * Math.PI;
                        Location start = summonLoc.clone().add(Math.cos(angle) * 4, Math.random() * 5, Math.sin(angle) * 4);
                        drawThread(start, summonLoc.clone().add(0, 2.5, 0), world);
                    }

                    // Эффект "сердцебиения" кокона (пульсация стала мощнее)
                    for (ItemDisplay part : cocoonParts) {
                        Transformation t = part.getTransformation();
                        float pulse = (float) (1.0 + Math.sin(ticks * 0.15) * 0.08);
                        // Кокон слегка расширяется и сужается
                        t.getScale().set(3.5f * pulse, 5.5f * pulse, 3.5f * pulse);
                        part.setTransformation(t);
                    }

                    // Звуки натяжения
                    if (ticks % 15 == 0) {
                        world.playSound(summonLoc, Sound.BLOCK_DEEPSLATE_STEP, 0.8f, 0.5f);
                    }
                }

                // ЭТАП 2: ФИНАЛЬНОЕ НАТЯЖЕНИЕ (100 - 120 тиков)
                if (ticks >= 100 && ticks < 120) {
                    // Интенсивный треск
                    if (ticks % 4 == 0) {
                        world.playSound(summonLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                        world.playSound(summonLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 0.5f);
                    }
                    // Тряска кокона
                    for (ItemDisplay part : cocoonParts) {
                        Transformation t = part.getTransformation();
                        t.getTranslation().add((float)Math.random()*0.1f - 0.05f, 0, (float)Math.random()*0.1f - 0.05f);
                        part.setTransformation(t);
                    }
                }

                if (ticks == 125) {
                    // 1. Удаляем кокон
                    cocoonParts.forEach(Entity::remove);
                    cocoonParts.clear();

                    // 2. Звуки разрыва и тяжести
                    world.playSound(summonLoc, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
                    world.playSound(summonLoc, Sound.ENTITY_IRON_GOLEM_STEP, 1.5f, 0.5f);
                    world.playSound(summonLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.5f);

                    // 3. Эффект разлетающихся нитей и пыли
                    world.spawnParticle(Particle.CLOUD, summonLoc.clone().add(0, 2, 0), 120, 1.5, 2, 1.5, 0.05);
                    world.spawnParticle(Particle.BLOCK, summonLoc.clone().add(0, 1, 0), 150, 1.5, 1, 1.5, Material.WHITE_STAINED_GLASS.createBlockData());

                    // 4. СПАВН МАХОРАГИ
                    mahoraga = world.spawn(summonLoc, IronGolem.class, g -> {
                        g.setCustomName("§6§lMahoraga");
                        g.setMetadata("Mahoraga", new FixedMetadataValue(JJKPlugin.getInstance(), true));
                        g.setMetadata("Summon", new FixedMetadataValue(JJKPlugin.getInstance(), true));
                        g.setMetadata("Owner", new FixedMetadataValue(JJKPlugin.getInstance(), player.getUniqueId().toString()));

                        g.getAttribute(Attribute.MAX_HEALTH).setBaseValue(150.0);
                        g.setHealth(150.0);
                        g.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(18.0);
                        g.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.45);
                        g.setRemoveWhenFarAway(false);
                    });
                }

                // ЭТАП 3: ПОЯВЛЕНИЕ (125 тик)
                if (ticks > 125) {
                    if (mahoraga == null || !mahoraga.isValid()) {
                        this.cancel();
                        return;
                    }

                    // Вращаем угол колеса
                    wheelRotation += 5.0f;
                    if (wheelRotation >= 360) wheelRotation = 0;

                    // Отрисовка колеса над головой
                    drawRotatingWheel(mahoraga.getLocation().add(0, 3, 0), wheelRotation);

                    // ИНТЕЛЛЕКТ МАХОРАГИ
                    LivingEntity target = mahoraga.getTarget();

                    // Поиск цели, если текущей нет или она - хозяин
                    if (target == null || target.isDead() || target.equals(player)) {
                        for (Entity e : mahoraga.getNearbyEntities(25, 10, 25)) {
                            if (e instanceof LivingEntity le && !e.equals(player) && !e.hasMetadata("Summon")) {
                                mahoraga.setTarget(le);
                                break;
                            }
                        }
                    }

                    // Если целей нет - идем к хозяину
                    if (mahoraga.getTarget() == null) {
                        double dist = mahoraga.getLocation().distance(player.getLocation());
                        if (dist > 8.0) {
                            mahoraga.getPathfinder().moveTo(player.getLocation());
                        } else if (dist < 4.0) {
                            mahoraga.getPathfinder().stopPathfinding();
                        }
                    }
                }
                if (ticks >= 925) {
                    if (mahoraga != null && mahoraga.isValid()) {
                        world.spawnParticle(Particle.SQUID_INK, mahoraga.getLocation().add(0, 1.5, 0), 100, 1, 1.5, 1, 0.1);
                        world.spawnParticle(Particle.WHITE_ASH, mahoraga.getLocation().add(0, 1.5, 0), 100, 1, 1.5, 1, 0.1);
                        world.playSound(mahoraga.getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, 1.5f, 0.3f);

                        mahoraga.remove();
                    }
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
    }

    // Вспомогательный метод для отрисовки нити
    private void drawThread(Location start, Location end, World world) {
        Vector direction = end.toVector().subtract(start.toVector());
        double len = direction.length();
        direction.normalize();
        for (double i = 0; i < len; i += 0.3) {
            world.spawnParticle(Particle.DUST, start.clone().add(direction.clone().multiply(i)), 1,
                    new Particle.DustOptions(Color.WHITE, 0.5f));
        }
    }
    private void drawRotatingWheel(Location loc, float rotationAngle) {
        double radius = 1.1;
        World world = loc.getWorld();
        double angleOffset = Math.toRadians(rotationAngle);

        // 1. Обод колеса
        for (double i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 1,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 0.7f));
        }

        // 2. Спицы (вращающиеся)
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.toRadians(45)) + angleOffset;
            for (double d = 0; d <= radius; d += 0.25) {
                double x = d * Math.cos(angle);
                double z = d * Math.sin(angle);
                world.spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 1,
                        new Particle.DustOptions(Color.fromRGB(218, 165, 32), 0.5f));
            }

            // Маленькие "наконечники" на концах спиц
            double tipX = radius * Math.cos(angle);
            double tipZ = radius * Math.sin(angle);
            world.spawnParticle(Particle.DUST, loc.clone().add(tipX, 0.1, tipZ), 1,
                    new Particle.DustOptions(Color.WHITE, 0.5f));
        }

        // Центр
        world.spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(Color.ORANGE, 1.0f));
    }
}