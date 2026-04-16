package abvgd.models.gojo;

import abvgd.core.JJKModel;
import abvgd.models.gojo.ability.*;
import abvgd.models.ReverseCursedTechnique;

public class GojoModel extends JJKModel {
    public GojoModel() {
        setDashAbility(new GojoDash());
        setInteractAbility(new GojoInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new RapidStrikes());
        addAbility(new Blue());
        addAbility(new Red());
        addAbility(new HollowPurple());
        addAbility(new InfiniteVoid());
        addAbility(new ReverseCursedTechnique());
    }

    @Override
    public int getMaxMastery() {
        return 45;
    }

    @Override
    public double getRegenCE() {
        return 1.4;
    }

    @Override
    public String getName() {
        return "gojo";
    }

    @Override
    public int getMaxEnergy() {
        return 1500;
    }

    @Override
    public float getWalkSpeed() {
        return 0.30f;
    }

    @Override
    public double getBaseStrength() {
        return 5.0;
    }

    @Override
    public double getBlackFlashChance() {
        return 0.15;
    }
}
