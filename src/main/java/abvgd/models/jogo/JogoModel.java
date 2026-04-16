package abvgd.models.jogo;

import abvgd.core.JJKModel;
import abvgd.models.jogo.ability.*;

public class JogoModel extends JJKModel {
    public JogoModel() {
        // У Джого быстрый рывок через реактивную струю пламени
        setDashAbility(new JogoDash());
        setInteractAbility(new JogoInteract());
    }

    @Override
    protected void registerAbilities() {
        addAbility(new EmberInsects());    // Самонаводящиеся жуки
        addAbility(new VolcanoEruption());
        addAbility(new DisasterFlames());
        addAbility(new Meteor());
        addAbility(new CoffinIronMountain());
    }

    @Override
    public int getMaxMastery() { return 50; }

    @Override
    public double getRegenCE() { return 1.6; } // Очень высокий реген энергии

    @Override
    public String getName() { return "§6Jogo"; } // Оранжевый цвет огня

    @Override
    public int getMaxEnergy() { return 1500; } // Огромный запас магии

    @Override
    public float getWalkSpeed() { return 0.28f; } // Довольно быстрый

    @Override
    public double getBaseStrength() { return 4; } // Слабый в рукопашную

    @Override
    public double getBlackFlashChance() { return 0.05; }
}
