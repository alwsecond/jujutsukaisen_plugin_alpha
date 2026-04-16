package abvgd.models.sukuna;

import abvgd.core.JJKModel;
import abvgd.models.ReverseCursedTechnique;
import abvgd.models.sukuna.ability.*;

public class SukunaModel extends JJKModel {
    public SukunaModel() {
        setDashAbility(new SukunaDash());
        setInteractAbility(new SukunaInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new Dismantle());
        addAbility(new WorldCuttingSlash());
        addAbility(new Spiderweb());
        addAbility(new FireArrow());
        addAbility(new MalevolentShrine());
        addAbility(new ReverseCursedTechnique());
    }

    @Override
    public int getMaxMastery() {
        return 50;
    }

    @Override
    public double getRegenCE() {return 1.4;}

    @Override
    public String getName() {
        return "sukuna";
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
