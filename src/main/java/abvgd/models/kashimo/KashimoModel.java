package abvgd.models.kashimo;

import abvgd.core.JJKModel;
import abvgd.models.kashimo.ability.*;

public class KashimoModel extends JJKModel {
    public KashimoModel() {
        // У Кашимо очень быстрый рывок, основанный на молнии
        setDashAbility(new KashimoDash());
        setInteractAbility(new KashimoInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new ElectricalDischarge());
        addAbility(new StaffStrike());
        addAbility(new ElectricField());
        addAbility(new MagneticPull());
        addAbility(new ThunderStrike());
        addAbility(new MythicalBeastAmber());
    }

    @Override
    public int getMaxMastery() { return 50; }

    @Override
    public double getRegenCE() {
        return 1.5; // У Кашимо отличный контроль энергии
    }

    @Override
    public String getName() {
        return "§eKashimo"; // Светло-желтый цвет под молнии
    }

    @Override
    public int getMaxEnergy() {
        return 1200; // Немного больше, чем у Махито
    }

    @Override
    public float getWalkSpeed() {
        return 0.32f; // Кашимо быстрее Махито
    }

    @Override
    public double getBaseStrength() {
        return 7; // Выше базовый урон в рукопашную
    }

    @Override
    public double getBlackFlashChance() {
        return 0.12; // Чуть выше шанс крита из-за его боевого опыта
    }
}
