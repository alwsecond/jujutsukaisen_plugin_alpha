package abvgd.core;

import org.bukkit.entity.Player;

public abstract class JJKAbility {

    private final JJKAbilityInfo info;

    public JJKAbility(JJKAbilityInfo info) {
        this.info = info;
    }

    public JJKAbilityInfo getInfo() {
        return info;
    }

    public abstract void onCast(Player player);
    // если есть с моим то текстурпаком
    public void playCustomSound(String key, Player caster, float v1, float v2) {
        caster.getWorld().playSound(
                caster.getLocation(),
                key,
                v1,
                v2
        );
    }
}