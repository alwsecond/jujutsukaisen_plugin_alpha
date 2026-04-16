package abvgd.utils;

import abvgd.core.JJKModel;
import abvgd.manage.JJKPlayer;
import org.bukkit.entity.Player;

public class JJKHUD {
    public static void update(Player player, JJKPlayer jjkPlayer) {
        JJKModel model = jjkPlayer.getModel();
        if (model == null) return;

        // --- 1. ОБНОВЛЕНИЕ ЭНЕРГИИ (XP BAR) ---
        int maxE = model.getMaxEnergy();

        if (maxE > 0) {
            // Если у персонажа есть энергия (Маги)
            player.setLevel((int) jjkPlayer.getEnergy());
            float progress = (float) (jjkPlayer.getEnergy() / maxE);
            player.setExp(Math.max(0, Math.min(progress, 0.999f)));
        } else {
            // Если энергии нет (Тодзи), просто обнуляем полоску, чтобы она не мешала
            player.setLevel(0);
            player.setExp(0);
        }

        double masteryPercent = jjkPlayer.getMasteryPercent() * 100;

        // Добавим немного красоты: если у игрока выгорание, пишем об этом
        if (jjkPlayer.hasBurnout()) {
            player.sendActionBar(String.format("§c§lВЫГОРАНИЕ §7| §e%.1f%%", masteryPercent));
        } else {
            player.sendActionBar(String.format("§f§d%.1f%%", masteryPercent));
        }
    }
}
