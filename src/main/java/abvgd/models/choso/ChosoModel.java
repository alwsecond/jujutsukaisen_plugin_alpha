package abvgd.models.choso;

import abvgd.core.JJKModel;
import abvgd.models.choso.ability.BloodMeteorShower;
import abvgd.models.choso.ability.PiercingBlood;
import abvgd.models.choso.ability.SlicingExorcism;
import abvgd.models.choso.ability.Supernova;

public class ChosoModel extends JJKModel {
    public ChosoModel() {
        setDashAbility(new ChosoDash());
        setInteractAbility(new ChosoInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new PiercingBlood());    // Основной луч (Пронзающая кровь)
        addAbility(new Supernova());
        addAbility(new SlicingExorcism());
        addAbility(new BloodMeteorShower());
    }

    @Override
    public int getMaxMastery() { return 50; }

    @Override
    public double getRegenCE() {
        return 1.2; // Средний реген энергии
    }

    @Override
    public String getName() {
        return "§4Choso"; // Темно-красный цвет
    }

    @Override
    public int getMaxEnergy() {
        return 1100;
    }

    @Override
    public float getWalkSpeed() {
        return 0.28f; // Базовая скорость средняя
    }

    @Override
    public double getBaseStrength() {
        return 5;
    }

    @Override
    public double getBlackFlashChance() {
        return 0.05;
    }
}
