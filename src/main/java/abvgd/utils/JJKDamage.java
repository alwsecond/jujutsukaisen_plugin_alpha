package abvgd.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class JJKDamage {

    private static boolean internalCall = false;

    /**
     * Наносит урон от имени техники.
     * Этот урон НЕ будет вызывать Чёрную Молнию.
     */
    public static void causeAbilityDamage(LivingEntity victim, Player attacker, double amount) {
        internalCall = true;
        victim.damage(amount, attacker);
        internalCall = false;
    }

    public static boolean isAbilityDamage() {
        return internalCall;
    }
}