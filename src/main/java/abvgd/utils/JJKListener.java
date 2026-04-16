package abvgd.utils;

import abvgd.JJKPlugin;
import abvgd.core.AbilityExecutor;
import abvgd.core.JJKAbility;
import abvgd.core.JJKModel;
import abvgd.core.types.DashAbility;
import abvgd.menu.SelectionMenu;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import abvgd.models.sukuna.SukunaInteract;
import abvgd.models.yuji.ability.BlackFlashFocus;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

public class JJKListener implements Listener {
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        JJKPlayer jjkPlayer = PlayerManager.get(player);
        if (jjkPlayer.getModel() == null) return;

        JJKAbility selected = jjkPlayer.getSelectedAbility();

        // --- 1. ВЫБОР МЕНЮ (Shift + ПКМ) ---
        if (player.isSneaking() && event.getAction().name().contains("RIGHT_CLICK")) {
            SelectionMenu.showMenu(player, jjkPlayer);
            return;
        }

        // --- 2. ОБЫЧНЫЙ КАСТ ТЕХНИК (Shift + ЛКМ) ---
        if (player.isSneaking() && event.getAction().name().contains("LEFT_CLICK")) {
            if (selected == null) {
                player.sendTitle("", "§cТехника не выбрана", 5, 20, 5);
                return;
            }
            AbilityExecutor.tryCast(player, selected);
        }
    }

    @EventHandler
    public void onJump(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() != Statistic.JUMP) return;

        Player player = event.getPlayer();

        if (!player.isSneaking()) return;

        JJKPlayer jjkPlayer = PlayerManager.get(player);
        if (jjkPlayer != null && jjkPlayer.getModel() != null) {
            DashAbility dash = jjkPlayer.getModel().getDashAbility();

            if (dash != null) {
                AbilityExecutor.tryCast(player, dash);
            }
        }
    }

    @EventHandler
    public void onFKey(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        JJKPlayer jjkPlayer = PlayerManager.get(player);
        if (jjkPlayer != null && jjkPlayer.getModel() != null) {
            JJKModel model = jjkPlayer.getModel();
            if (model.getInteractAbility() != null) {
                event.setCancelled(true);
                AbilityExecutor.tryCast(player, model.getInteractAbility());
            }
        }
    }

    // 2. ОБРАБОТКА УРОНА И ЧЁРНОЙ МОЛНИИ
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // --- СТРУКТУРИРОВАННАЯ ПРОВЕРКА ---
        if (JJKDamage.isAbilityDamage()) return;

        JJKPlayer jjkPlayer = PlayerManager.get(attacker);
        if (jjkPlayer == null || jjkPlayer.getModel() == null) return;

        JJKModel model = jjkPlayer.getModel();

        // 1. Сила кулака
        if (attacker.getInventory().getItemInMainHand().getType() == Material.AIR) {
            event.setDamage(model.getBaseStrength());
        }

        // mahito / yuju
        if (attacker.hasMetadata("BlackFlashActive")) {
            BlackFlashMechanic.trigger(event, attacker, jjkPlayer);
            attacker.removeMetadata("BlackFlashActive", JJKPlugin.getInstance());
            if (attacker.hasMetadata("BF_Rhythm_Level")) {
                int lastLevel = attacker.getMetadata("BF_Rhythm_Level").get(0).asInt();
                if (lastLevel < 3) {
                    BlackFlashFocus.startRhythmCycle(attacker, lastLevel + 1);
                } else {
                    attacker.removeMetadata("BF_Rhythm_Level", JJKPlugin.getInstance());
                    attacker.sendActionBar("§c§lМАКСИМАЛЬНАЯ КОНЦЕНТРАЦИЯ!");
                }
            }
            return;
        }

        // 2. Шанс на Чёрную Молнию (только для физических ударов!)
        double chance = model.getBlackFlashChance();
        if (Math.random() < chance) {
            BlackFlashMechanic.trigger(event, attacker, jjkPlayer);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        // Когда игрок ОТПУСКАЕТ Shift
        if (!event.isSneaking()) {
            if (SelectionMenu.isMenuOpen(player)) {
                JJKPlayer jjkPlayer = PlayerManager.get(player);
                SelectionMenu.closeMenuAndSelect(player, jjkPlayer);
            }
        }
    }
}