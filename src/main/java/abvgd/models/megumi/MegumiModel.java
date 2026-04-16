package abvgd.models.megumi;

import abvgd.core.JJKModel;
import abvgd.models.megumi.ability.*;

public class MegumiModel extends JJKModel {
    public MegumiModel() {
        setDashAbility(new MegumiDash());
        setInteractAbility(new MegumiInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new DivineDogs());
        addAbility(new Nue());
        addAbility(new MaxElephant());
        addAbility(new Toad());
        addAbility(new RabbitEscape());
        addAbility(new ChimeraShadowGarden());
        addAbility(new Mahoraga());
    }

    @Override
    public int getMaxMastery() { return 45; }

    @Override
    public double getRegenCE() { return 1.2; }

    @Override
    public String getName() { return "megumi"; }

    @Override
    public int getMaxEnergy() { return 800; }

    @Override
    public float getWalkSpeed() { return 0.25f; }

    @Override
    public double getBaseStrength() { return 4.0; }

    @Override
    public double getBlackFlashChance() { return 0.03; } // 3%
}
