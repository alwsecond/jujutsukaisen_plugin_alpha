package abvgd.models.yuji.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.types.ActiveAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BlackFlashFocus extends ActiveAbility {

    public BlackFlashFocus() {
        super(new JJKAbilityInfo("§0§lBlack Flash §c§lFocus §7<interact>", Material.BLACK_DYE, 0, 0, 0, false));
    }

    @Override
    public void onCast(Player player) {
        startRhythmCycle(player, 1);
    }

    public static void startRhythmCycle(Player player, int level) {
        if (level > 3 || !player.isOnline()) return;

        player.removeMetadata("BF_Rhythm_Level", JJKPlugin.getInstance());
        player.removeMetadata("BF_Perfect_Window", JJKPlugin.getInstance());

        Bukkit.getScheduler().runTaskLater(JJKPlugin.getInstance(), () -> {
            player.setMetadata("BF_Rhythm_Level", new FixedMetadataValue(JJKPlugin.getInstance(), level));

            new BukkitRunnable() {
                int ticks = 0;
                // Момент, когда прозвучит "Дзинь" (например, через 20 тиков)
                int strikeTick = 20;

                // ОКНО: С каждым уровнем оно становится короче (10 тиков -> 7 тиков -> 4 тика)
                int windowSize = 14 - (level * 3);

                @Override
                public void run() {
                    if (!player.isOnline() || !player.hasMetadata("BF_Rhythm_Level")) { this.cancel(); return; }

                    // 1. МОМЕНТ УДАРА (Дзинь)
                    if (ticks == strikeTick) {
                        player.setMetadata("BF_Perfect_Window", new FixedMetadataValue(JJKPlugin.getInstance(), true));

                        // Эффекты единственного "Дзинь"
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, windowSize, 0, false, false, false));
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.5f + (level * 0.5f));
                        player.sendMessage("["+level+"] " +"§b§l● §f§lУДАРЯЙ §b§l●");
                    }

                    // 2. ЗАКРЫТИЕ ОКНА
                    if (ticks >= strikeTick + windowSize) {
                        if (player.hasMetadata("BF_Perfect_Window")) {
                            failRhythm(player);
                        }
                        this.cancel();
                    }

                    ticks++;
                }
            }.runTaskTimer(JJKPlugin.getInstance(), 0L, 1L);
        }, 5L);
    }

    private static void playTick(Player player, int stage, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8, 0, false, false, false));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 2.0f, 0.5f + (stage * 0.4f) + (level * 0.2f));
        String color = level == 1 ? "§e" : (level == 2 ? "§6" : "§c");
        player.sendActionBar("§0§l[ " + color + "● ".repeat(stage) + "§8○ ".repeat(3-stage) + "§0§l] §7Уровень: " + level);
    }

    private static void failRhythm(Player player) {
        player.removeMetadata("BF_Rhythm_Level", JJKPlugin.getInstance());
        player.removeMetadata("BF_Perfect_Window", JJKPlugin.getInstance());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        player.sendMessage("§8§lРитм сбит...");
    }
}
