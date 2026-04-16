package abvgd.models.hakari;

import abvgd.core.JJKModel;
import abvgd.models.hakari.ability.*;

public class HakariModel extends JJKModel {
    public HakariModel() {
        setDashAbility(new HakariDash());
        setInteractAbility(new HakariInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new TrainDoors());
        addAbility(new RoughCombo());
        addAbility(new RoughBolt());
        addAbility(new IdleDeathGamble());
    }

    @Override
    public int getMaxMastery() { return 60; }

    @Override
    public double getRegenCE() { return 1.1; }

    @Override
    public String getName() { return "§d§lhakari"; }

    @Override
    public int getMaxEnergy() { return 1000; }

    @Override
    public float getWalkSpeed() { return 0.26f; }

    @Override
    public double getBaseStrength() {
        return 7.0;
    }

    @Override
    public double getBlackFlashChance() {
        return 0.05;
    }
}
