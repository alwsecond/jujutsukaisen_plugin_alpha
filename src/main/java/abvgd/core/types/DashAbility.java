package abvgd.core.types;

import abvgd.core.JJKAbilityInfo;
import org.bukkit.entity.Player;

public abstract class DashAbility extends ActiveAbility {
    public DashAbility(JJKAbilityInfo info) {
        super(info);
    }

    @Override
    public abstract void onCast(Player player);
}