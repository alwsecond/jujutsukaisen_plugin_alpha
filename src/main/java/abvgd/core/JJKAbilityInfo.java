package abvgd.core;

import org.bukkit.Material;

public record JJKAbilityInfo(
        String name,
        Material icon,
        int energyCost,     // Цена в ПЭ (Проклятой Энергии)
        int cooldownTicks,   // Кулдаун в тиках (20 тиков = 1 сек)
        double requiredMasteryPercent,
        boolean consumesMastery
) {}