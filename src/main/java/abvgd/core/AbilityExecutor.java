package abvgd.core;

import abvgd.core.types.ActiveAbility;
import abvgd.core.types.DashAbility;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKHUD;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AbilityExecutor {

    public static void tryCast(Player player, JJKAbility ability) {
        JJKPlayer jjkPlayer = PlayerManager.get(player);
        JJKAbilityInfo info = ability.getInfo();
        if (jjkPlayer == null || jjkPlayer.getModel() == null) return;

        if (jjkPlayer.hasBurnout() && !(ability instanceof DashAbility)) {
            double remaining = jjkPlayer.getRemainingBurnoutSec();
            player.sendMessage(String.format("§c§lВЫГОРАНИЕ ТЕХНИК §7| §e%.1f сек.", remaining));
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
            return;
        }

        // 1. ПРОВЕРКА КУЛДАУНА (общая для всех)
        if (jjkPlayer.isOnCooldown(ability)) {
            double remaining = jjkPlayer.getRemainingCooldownMs(ability) / 1000.0;
            player.sendMessage(String.format("[ §e%.1f§r ] сек.", remaining));
            return;
        }

        // 2. ПРОВЕРКА ЭНЕРГИИ
        int cost = ability.getInfo().energyCost();
            if (!jjkPlayer.hasEnoughEnergy(cost)) {
                player.sendActionBar("§cНедостаточно проклятой энергии!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.5f);
                return;
        }

        // 3. ИСПОЛНЕНИЕ
        if (ability instanceof ActiveAbility active) {
            if (info.consumesMastery()) {jjkPlayer.resetMastery();}

            jjkPlayer.consumeEnergy(cost);
            int cd = (ability instanceof DashAbility) ? 100 : info.cooldownTicks();
            jjkPlayer.setCooldown(ability, cd);

            active.onCast(player);
            JJKHUD.update(player, jjkPlayer);
        }
    }
}