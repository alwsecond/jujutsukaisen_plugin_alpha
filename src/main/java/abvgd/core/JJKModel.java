package abvgd.core;

import abvgd.core.types.DashAbility;
import abvgd.core.types.InteractAbility;

import java.util.ArrayList;
import java.util.List;

public abstract class JJKModel {
    private final List<JJKAbility> abilities = new ArrayList<>();
    private DashAbility dashAbility;
    private InteractAbility interactAbility;

    public JJKModel() {
        registerAbilities();
    }

    public void setDashAbility(DashAbility dash) {this.dashAbility = dash;}
    public DashAbility getDashAbility() {return dashAbility;}

    public void setInteractAbility(InteractAbility ability) { this.interactAbility = ability; }
    public InteractAbility getInteractAbility() { return interactAbility; }

    // Регистрация способностей при создании модели
    protected abstract void registerAbilities();

    protected void addAbility(JJKAbility ability) {
        abilities.add(ability);
    }

    public List<JJKAbility> getAbilities() {
        return abilities;
    }

    public abstract double getRegenCE();

    public abstract int getMaxMastery();

    public abstract String getName();

    public abstract double getBaseStrength();

    public abstract int getMaxEnergy();

    public float getWalkSpeed() { return 0.2f; }

    public abstract double getBlackFlashChance();
}