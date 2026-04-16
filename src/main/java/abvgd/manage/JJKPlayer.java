package abvgd.manage;

import abvgd.core.JJKAbility;
import abvgd.core.JJKModel;
import org.bukkit.entity.Player;

import java.util.*;

public class JJKPlayer {
    private final UUID uuid;
    private JJKModel currentModel;
    private double energy;
    private double mastery = 0;
    private long burnoutUntil = 0;
    private JJKAbility selectedAbility;

    private final Map<String, Long> cooldowns = new HashMap<>();

    public JJKPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    // --- УПРАВЛЕНИЕ МОДЕЛЬЮ ---
    public void setModel(JJKModel model, Player bukkitPlayer) {
        this.currentModel = model;
        if (model != null) {
            this.energy = model.getMaxEnergy();
            bukkitPlayer.setWalkSpeed(model.getWalkSpeed());
        } else {
            bukkitPlayer.setWalkSpeed(0.2f);
        }
    }

    public void tickRegeneration() {
        if (currentModel == null) return;
        regenerate(currentModel.getRegenCE());
    }

    public void regenerate(double amount) {
        if (currentModel == null) return;
        double max = (double) currentModel.getMaxEnergy();
        if (this.energy < max) {
            this.energy = Math.min(this.energy + amount, max);
        }
    }

    // --- ЭНЕРГИЯ (Чистая математика) ---
    public void addEnergy(double amount) {
        if (currentModel == null) return;
        this.energy = Math.min(this.energy + amount, (double) currentModel.getMaxEnergy());
    }

    public void setEnergy(double amount) {this.energy = Math.max(0, amount);}

    public void consumeEnergy(double amount) {
        this.energy = Math.max(0, this.energy - amount);
    }

    public boolean hasEnoughEnergy(double amount) {
        return this.energy >= amount;
    }

    // --- МАСТЕРСТВО ---
    public void addMastery(double amount) {
        if (currentModel == null) return;
        this.mastery = Math.min(this.mastery + amount, (double) currentModel.getMaxMastery());
    }

    public void resetMastery() { this.mastery = 0; this.setSelectedAbility(null);}

    public double getMasteryPercent() {
        if (currentModel == null || currentModel.getMaxMastery() <= 0) return 0.0;
        return mastery / currentModel.getMaxMastery();
    }

    // --- КУЛДАУНЫ ---
    public void setCooldown(JJKAbility ability, int ticks) {
        cooldowns.put(ability.getInfo().name(), System.currentTimeMillis() + (ticks * 50L));
    }

    public boolean isOnCooldown(JJKAbility ability) {
        Long end = cooldowns.get(ability.getInfo().name());
        return end != null && end > System.currentTimeMillis();
    }

    public long getRemainingCooldownMs(JJKAbility ability) {
        Long end = cooldowns.get(ability.getInfo().name());
        return (end == null) ? 0 : Math.max(0, end - System.currentTimeMillis());
    }

    // --- ВЫГОРАНИЕ (Burnout) ---
    public void setBurnout(int ticks) {
        this.burnoutUntil = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean hasBurnout() {
        return System.currentTimeMillis() < burnoutUntil;
    }

    public double getRemainingBurnoutSec() {
        return hasBurnout() ? (burnoutUntil - System.currentTimeMillis()) / 1000.0 : 0;
    }

    // --- ГЕТТЕРЫ ---
    public JJKModel getModel() { return currentModel; }
    public double getEnergy() { return energy; }
    public double getMastery() { return mastery; }
    public JJKAbility getSelectedAbility() { return selectedAbility; }
    public void setSelectedAbility(JJKAbility ability) { this.selectedAbility = ability; }

    public int getMaxEnergy() {
        return currentModel != null ? currentModel.getMaxEnergy() : 0;
    }
}