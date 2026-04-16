package abvgd.manage;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static final Map<UUID, JJKPlayer> players = new HashMap<>();

    public static JJKPlayer get(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), JJKPlayer::new);
    }

    public static void remove(Player player) {
        players.remove(player.getUniqueId());
    }
}