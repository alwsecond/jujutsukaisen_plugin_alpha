package abvgd.core.types;

import abvgd.core.JJKAbilityInfo;
import org.bukkit.entity.Player;

public abstract class InteractAbility extends ActiveAbility {
    public InteractAbility(JJKAbilityInfo info) {
        super(info);
    }

    @Override
    public abstract void onCast(Player player);
}
