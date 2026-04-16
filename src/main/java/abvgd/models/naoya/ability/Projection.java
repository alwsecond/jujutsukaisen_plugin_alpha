package abvgd.models.naoya.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Projection extends ActiveAbility {

    // Сет для хранения UUID сущностей, которые сейчас "заморожены", чтобы не дамажить их дважды
    private static final Set<UUID> frozenEntities = new HashSet<>();

    public Projection() {
        super(new JJKAbilityInfo(
                "Projection Dash",
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                0,
                40,
                0,
                false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Vector dir = player.getLocation().getDirection().setY(0).normalize();

        // --- 1. СКОРОСТЬ И ИНВИЗ ---
        int currentLevel = player.hasPotionEffect(PotionEffectType.SPEED) ?
                player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() : 0;

        int nextSpeed = Math.min(currentLevel + 15, 20);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, nextSpeed, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 1, false, false, false));

        player.setVelocity(dir.multiply(1.5 + (nextSpeed * 0.1)));
        world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 2f);

        // --- 2. БЕСКОНЕЧНЫЙ ШЛЕЙФ И ПРОВЕРКА СТОЛКНОВЕНИЙ ---
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasPotionEffect(PotionEffectType.SPEED)) {
                    this.cancel();
                    return;
                }

                if (player.getPotionEffect(PotionEffectType.SPEED).getDuration() < 2) {
                    this.cancel();
                    return;
                }

                Location currentLoc = player.getLocation();

                // Спавним кадр ровно под игроком
                spawnPerfectFrame(currentLoc);

                if (Math.random() > 0.8) {
                    world.playSound(currentLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
                }

                // --- 3. ПРОВЕРКА ПРОХОЖДЕНИЯ СКВОЗЬ ЭНТИТИ (ЗАМОРОЗКА) ---
                // Проверяем радиус 1.5 блока вокруг Наои
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 2.0, 1.5)) {
                    if (entity instanceof LivingEntity target && target != player) {
                        applyFrameFreeze(player, target, nextSpeed);
                    }
                }
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 2);
    }

    private void applyFrameFreeze(Player caster, LivingEntity target, int speedLevel) {
        UUID targetId = target.getUniqueId();

        // 1. ПРОВЕРКА: Если цель уже "в стекле", выходим.
        // Она удалится из сета только через 20 тиков, когда сработает урон.
        if (frozenEntities.contains(targetId)) return;
        frozenEntities.add(targetId);

        // Оглушение
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 10, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 25, 250, false, false, false));

        // Создаем визуальный "кокон" из стекла
        BlockDisplay frameCage = target.getWorld().spawn(target.getLocation(), BlockDisplay.class, ent -> {
            ent.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
            Transformation trans = ent.getTransformation();
            // Чуть больше хитбокса игрока
            trans.getScale().set(1.1f, 2.1f, 1.1f);
            trans.getTranslation().set(-0.55f, 0f, -0.55f);
            ent.setTransformation(trans);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setBillboard(Display.Billboard.FIXED); // Чтобы не крутилось за камерой
        });

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 1.5f);

        // --- ТАЙМЕР: Разрушение и урон ---
        new BukkitRunnable() {
            @Override
            public void run() {
                if (frameCage.isValid()) frameCage.remove();

                if (target.isValid() && !target.isDead()) {
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);

                    // Эффект осколков
                    target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                            40, 0.3, 0.5, 0.3, Material.WHITE_STAINED_GLASS.createBlockData());

                    // Наносим урон (caster указан как источник для правильных логов смерти)
                    double damage = 8.0 + (speedLevel * 1.5);
                    JJKDamage.causeAbilityDamage(target, caster, damage);
                }

                // ТОЛЬКО ТЕПЕРЬ цель может снова получить урон от этой техники
                frozenEntities.remove(targetId);
            }
        }.runTaskLater(JJKPlugin.getInstance(), 20L);
    }

    private void spawnPerfectFrame(Location loc) {
        BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class, ent -> {
            ent.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());

            Transformation trans = ent.getTransformation();
            trans.getScale().set(1.2f, 2.0f, 0.01f);

            trans.getTranslation().set(-0.6f, 0f, -0.005f);
            ent.setTransformation(trans);

            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setRotation(loc.getYaw(), 0);
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                if (display.isValid()) display.remove();
            }
        }.runTaskLater(JJKPlugin.getInstance(), 8);
    }
}