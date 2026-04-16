package abvgd.models.mahito.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SplitDecoy extends ActiveAbility {

    public SplitDecoy() {
        super(new JJKAbilityInfo(
                "Split Decoy",
                Material.ZOMBIE_HEAD,
                0, 15, 0, false
        ));
    }

    @Override
    public void onCast(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation();

        // 1. ПОИСК ЦЕЛИ
        RayTraceResult ray = world.rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 15, 1.0, e -> e != player && e instanceof LivingEntity);
        LivingEntity target = (ray != null && ray.getHitEntity() != null) ? (LivingEntity) ray.getHitEntity() : null;

        // 2. ГОТОВИМ МАСКИРОВКУ
        ItemStack zombieMask = new ItemStack(Material.ZOMBIE_HEAD);
        ItemStack suitChest = createColoredArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(80, 110, 120));
        ItemStack suitLegs = createColoredArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(45, 45, 45));
        ItemStack[] oldArmor = player.getInventory().getArmorContents();

        player.getInventory().setHelmet(zombieMask);
        player.getInventory().setChestplate(suitChest);
        player.getInventory().setLeggings(suitLegs);

        // 3. ПОЗИЦИИ
        Location pivot = (target != null) ? target.getLocation() : player.getLocation();
        List<Location> positions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(i * 120);
            positions.add(pivot.clone().add(5 * Math.cos(angle), 0, 5 * Math.sin(angle)));
        }
        Collections.shuffle(positions);

        // 4. СПАВН КЛОНОВ
        List<Zombie> clones = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Location loc = positions.get(i);
            Zombie clone = world.spawn(loc, Zombie.class, z -> {
                z.setAI(true); // Сразу включаем ИИ
                z.setSilent(true);
                z.setCanPickupItems(false);

                // ЗАЩИТА ОТ ДРОПА
                z.getEquipment().setHelmetDropChance(0);
                z.getEquipment().setChestplateDropChance(0);
                z.getEquipment().setLeggingsDropChance(0);
                z.getEquipment().setItemInMainHandDropChance(0);

                z.getEquipment().setHelmet(zombieMask);
                z.getEquipment().setChestplate(suitChest);
                z.getEquipment().setLeggings(suitLegs);
                z.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());

                z.setMetadata("MahitoClone", new FixedMetadataValue(JJKPlugin.getInstance(), true));
            });
            clones.add(clone);
            world.spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.05);
        }

        player.teleport(positions.get(2));
        world.playSound(player.getLocation(), Sound.BLOCK_CHERRY_SAPLING_BREAK, 1f, 0.5f);

        // 5. ЛОГИКА ПРЕСЛЕДОВАНИЯ И УРОНА
        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life > 100 || !player.isOnline()) { this.cancel(); return; }

                for (Zombie c : clones) {
                    if (!c.isValid()) continue;

                    // Заставляем бежать к цели
                    if (target != null && target.isValid()) {
                        c.setTarget(target);
                        c.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.5);
                    }

                    // Урон при касании
                    for (Entity e : c.getNearbyEntities(1.2, 1.2, 1.2)) {
                        if (e instanceof LivingEntity vic && e != player && !e.hasMetadata("MahitoClone")) {
                            JJKDamage.causeAbilityDamage(vic, player, 1.5);
                        }
                    }
                }
                life += 5;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 5L, 5L);

        // 6. ФИНАЛЬНЫЙ ВЗРЫВ
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getInventory().setArmorContents(oldArmor);
                for (Zombie c : clones) {
                    if (c.isValid()) {
                        Location l = c.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.BLOCK, l, 50, 0.5, 0.5, 0.5, Material.NETHER_WART_BLOCK.createBlockData());
                        world.playSound(l, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);

                        for (Entity e : world.getNearbyEntities(l, 3, 3, 3)) {
                            if (e instanceof LivingEntity vic && e != player && !e.hasMetadata("MahitoClone")) {
                                JJKDamage.causeAbilityDamage(vic, player, 8.0);
                            }
                        }
                        c.remove();
                    }
                }
            }
        }.runTaskLater(JJKPlugin.getInstance(), 100L);
    }

    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) { meta.setColor(color); item.setItemMeta(meta); }
        return item;
    }
}