package abvgd.models.yuji;

import abvgd.core.JJKModel;
import abvgd.models.yuji.ability.*;

public class YujiModel extends JJKModel {
    public YujiModel() {
        setDashAbility(new YujiDash());
        setInteractAbility(new YujiInteract());
    }

    @Override
    protected void registerAbilities() {
        // Основные техники Итадори
        addAbility(new DivergentFist()); // Расходящийся кулак
        addAbility(new SoulHit());
        addAbility(new ManjiKick());
        addAbility(new SoulScissors());
        addAbility(new BlackFlashFocus());
        addAbility(new BenevolentShrine());
    }

    @Override
    public int getMaxMastery() { return 100; } // Он должен полностью раскрыться

    @Override
    public double getRegenCE() { return 1.3; } // Хорошая регенерация энергии

    @Override
    public String getName() { return "§6§lYuji Itadori"; }

    @Override
    public int getMaxEnergy() { return 1000; }

    @Override
    public float getWalkSpeed() {
        return 0.30f; // Очень быстрый бег
    }

    @Override
    public double getBaseStrength() {
        return 7.0; // Высокий базовый урон кулаками
    }

    @Override
    public double getBlackFlashChance() {
        return 0.16; // 16% — самый высокий шанс в плагине
    }
}
