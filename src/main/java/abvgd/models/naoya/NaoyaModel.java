package abvgd.models.naoya;

import abvgd.core.JJKModel;
import abvgd.models.naoya.ability.*;


public class NaoyaModel extends JJKModel {
    public NaoyaModel() {
        setDashAbility(new NaoyaDash());
        setInteractAbility(new NaoyaInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new ProjectionCombo());
        addAbility(new Projection());
        addAbility(new Rapid());
        addAbility(new VerticalProjection());
        addAbility(new HumanCellPalace());
    }

    @Override
    public int getMaxMastery() { return 40; }

    @Override
    public double getRegenCE() { return 1.1; }

    @Override
    public String getName() { return "§3naoya"; }

    @Override
    public int getMaxEnergy() { return 800; }

    @Override
    public float getWalkSpeed() {
        return 0.28f;
    }

    @Override
    public double getBaseStrength() { return 4.5; }

    @Override
    public double getBlackFlashChance() { return 0.0; }
}
