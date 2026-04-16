package abvgd.models.mahito;

import abvgd.core.JJKModel;
import abvgd.models.mahito.ability.*;

public class MahitoModel extends JJKModel {
    public MahitoModel() {
        setDashAbility(new MahitoDash());
        setInteractAbility(new MahitoInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new SplitDecoy());
        addAbility(new BodyRepel());
        addAbility(new SelfEmbodiment());
        addAbility(new SoulHammer());
        addAbility(new BlackFlashStrike());
    }

    @Override
    public int getMaxMastery() { return 50; }

    @Override
    public double getRegenCE() { return 1.3; }

    @Override
    public String getName() { return "§dmahito"; }

    @Override
    public int getMaxEnergy() { return 1000; }

    @Override
    public float getWalkSpeed() { return 0.28f; }

    @Override
    public double getBaseStrength() { return 5; }

    @Override
    public double getBlackFlashChance() { return 0.12; }
}
