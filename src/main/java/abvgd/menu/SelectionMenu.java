package abvgd.menu;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbility;
import abvgd.manage.JJKPlayer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SelectionMenu {
    private static final Map<UUID, List<ItemDisplay>> activeMenus = new HashMap<>();
    private static final Map<UUID, BukkitTask> updateTasks = new HashMap<>();
    private static final Map<UUID, Vector> menuDirections = new HashMap<>();

    // Вспомогательный метод для фильтрации (чтобы не дублировать код)
    private static List<JJKAbility> getFilteredAbilities(JJKPlayer jjkPlayer) {
        List<JJKAbility> all = jjkPlayer.getModel().getAbilities();
        List<JJKAbility> filtered = new ArrayList<>();
        double currentProgress = jjkPlayer.getMasteryPercent();

        for (JJKAbility a : all) {
            if (currentProgress >= a.getInfo().requiredMasteryPercent()) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    public static void showMenu(Player player, JJKPlayer jjkPlayer) {
        UUID uuid = player.getUniqueId();
        if (activeMenus.containsKey(uuid)) return;

        // ИСПОЛЬЗУЕМ ФИЛЬТР ВМЕСТО getModel().getAbilities()
        List<JJKAbility> abilities = getFilteredAbilities(jjkPlayer);
        if (abilities.isEmpty()) return;

        Vector initialDir = player.getEyeLocation().getDirection().setY(0).normalize();
        menuDirections.put(uuid, initialDir);

        List<ItemDisplay> displays = new ArrayList<>();
        for (int i = 0; i < abilities.size(); i++) {
            ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(player.getLocation(), EntityType.ITEM_DISPLAY);
            display.setItemStack(new ItemStack(abilities.get(i).getInfo().icon()));
            display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.addScoreboardTag("jjk_idx_" + i);
            displays.add(display);
        }
        activeMenus.put(uuid, displays);

        final double radius = 3.3;
        final double angleStep = 0.35;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeMenus.containsKey(uuid)) {
                    this.cancel();
                    return;
                }
                Vector baseDir = menuDirections.get(uuid);
                Location eyeLoc = player.getEyeLocation();
                for (int i = 0; i < displays.size(); i++) {
                    double currentAngle = (i - (displays.size() - 1) / 2.0) * angleStep;
                    Vector rotatedDir = baseDir.clone();
                    rotateAroundY(rotatedDir, -currentAngle);
                    Location loc = eyeLoc.clone().add(rotatedDir.multiply(radius)).subtract(0, 0.3, 0);
                    displays.get(i).teleport(loc);
                }
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);

        updateTasks.put(uuid, task);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
    }

    public static void closeMenuAndSelect(Player player, JJKPlayer jjkPlayer) {
        UUID uuid = player.getUniqueId();
        if (updateTasks.containsKey(uuid)) {
            updateTasks.get(uuid).cancel();
            updateTasks.remove(uuid);
        }
        menuDirections.remove(uuid);

        if (!activeMenus.containsKey(uuid)) return;

        List<ItemDisplay> displays = activeMenus.get(uuid);
        List<JJKAbility> abilities = getFilteredAbilities(jjkPlayer);

        ItemDisplay bestTarget = null;
        double minAngle = 0.38;
        Vector lookDir = player.getEyeLocation().getDirection();

        for (ItemDisplay display : displays) {
            Vector toDisplay = display.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
            double angle = lookDir.angle(toDisplay);
            if (angle < minAngle) {
                minAngle = angle;
                bestTarget = display;
            }
        }

        if (bestTarget != null) {
            for (String tag : bestTarget.getScoreboardTags()) {
                if (tag.startsWith("jjk_idx_")) {
                    int idx = Integer.parseInt(tag.replace("jjk_idx_", ""));
                    if (idx >= 0 && idx < abilities.size()) {
                        JJKAbility selected = abilities.get(idx);
                        jjkPlayer.setSelectedAbility(selected);
                        player.sendTitle(" ", "" + selected.getInfo().name(), 5, 20, 5);
                    }
                    break;
                }
            }
        }

        for (ItemDisplay d : displays) d.remove();
        activeMenus.remove(uuid);
    }

    private static void rotateAroundY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        v.setX(x).setZ(z);
    }

    public static boolean isMenuOpen(Player player) {
        return activeMenus.containsKey(player.getUniqueId());
    }
}