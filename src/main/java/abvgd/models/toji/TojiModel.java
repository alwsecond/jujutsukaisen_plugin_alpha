package abvgd.models.toji;

import abvgd.core.JJKModel;
import abvgd.models.toji.ability.*;

public class TojiModel extends JJKModel {
    public TojiModel() {
        setDashAbility(new TojiDash());
        setInteractAbility(new TojiInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new ISOH());
        addAbility(new SoulKatana());
        addAbility(new PhantomDash());
        addAbility(new InvisibleThreat());
        addAbility(new MosquitoStrike());
    }

    @Override
    public int getMaxMastery() { return 20; }

    @Override
    public String getName() { return "toji"; }

    @Override
    public int getMaxEnergy() { return 0; }

    @Override
    public double getRegenCE() { return 0; }

    @Override
    public double getBaseStrength() { return 8.0; }

    @Override
    public double getBlackFlashChance() { return 0.0; }

    @Override
    public float getWalkSpeed() { return 0.32f; }
}
