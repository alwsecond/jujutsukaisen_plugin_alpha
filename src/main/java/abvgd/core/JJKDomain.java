package abvgd.core;

import abvgd.JJKPlugin;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public abstract class JJKDomain extends ActiveAbility {

    private boolean isCasting = false;
    private boolean isActive = false;
    private Location domainCenter;
    private boolean isClashing = false;

    // Хранилище для возврата мира
    private final Map<Location, BlockData> backupBlocks = new HashMap<>();
    // Отсортированный список координат для красивого роста барьера
    private final List<Location> shellBlocksToBuild = new ArrayList<>();

    public JJKDomain(JJKAbilityInfo info) {
        super(info);
    }

    // ==========================================
    // КОНСТРУКТОР ТЕРРИТОРИИ (НАСТРОЙКИ)
    // ==========================================
    public abstract int getCastTicks();
    public abstract int getDomainTicks();
    public abstract int getBurnoutTicks();
    public abstract double getRadius();

    public abstract boolean hasBarrier();

    public abstract Material getFloorMaterial();
    public abstract Material getBarrierMaterial();

    // ==========================================
    // СОБЫТИЯ
    // ==========================================
    protected abstract void onCastTick(Player caster, int tick);
    protected abstract void onExpand(Player caster, Location center);
    protected abstract void onDomainTick(Player caster, Location center, int tick);
    protected abstract void onSureHit(Player caster, LivingEntity victim, int tick);
    protected abstract void onEnd(Player caster);

    // ==========================================
    // ОСНОВНАЯ ЛОГИКА
    // ==========================================
    public void setClashing(boolean clashing) {
        this.isClashing = clashing;
    }

    @Override
    public void onCast(Player player) {
        if (isCasting || isActive) return;

        this.domainCenter = player.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5);
        this.isClashing = false;

        // Сразу регистрируем, чтобы другие нас видели
        JJKPlugin.activeDomains.add(this);
        isCasting = true;
        cleanupData();

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    abortDomain();
                    this.cancel();
                    return;
                }

                if (!isClashing) {
                    for (JJKDomain other : JJKPlugin.activeDomains) {
                        if (other == null || other == JJKDomain.this || other.domainCenter == null) continue;
                        if (other.domainCenter.distance(domainCenter) < (getRadius() + other.getRadius())) {
                            isClashing = true;
                            other.setClashing(true);

                            player.sendMessage("§c§lDOMAIN CLASH! §fГенерация пола остановлена.");
                            player.getWorld().playSound(domainCenter, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
                            break;
                        }
                    }
                }

                onCastTick(player, tick);

                // Генерируем пол ТОЛЬКО если Clash еще не случился
                if (!isClashing) {
                    double currentFloorRadius = getRadius() * ((double) tick / getCastTicks());
                    generateFloorStep(domainCenter, currentFloorRadius);
                }

                if (tick >= getCastTicks()) {
                    activateDomain(player);
                    this.cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void activateDomain(Player player) {
        isCasting = false;
        isActive = true;
        domainCenter.add(0, 1, 0); // Центрируем для сферы

        if (isClashing) {
            prepareDomainShell(domainCenter, getRadius(), false);

            // 2. МОМЕНТАЛЬНЫЙ ЭФФЕКТ ПОГЛОЩЕНИЯ
            World world = domainCenter.getWorld();
            world.playSound(domainCenter, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);

            // Визуал: Осколки барьера разлетаются и исчезают
            world.spawnParticle(Particle.BLOCK, domainCenter, 200, 5, 5, 5, 0.2, getBarrierMaterial().createBlockData());
            world.spawnParticle(Particle.FLASH, domainCenter, 5, 1, 1, 1, 0);

            player.sendMessage("§c§l[!] БАРЬЕРЫ ПОГЛОТИЛИ ДРУГ ДРУГА");
        } else {
            // Обычный режим (если один)
            prepareDomainShell(domainCenter, getRadius(), hasBarrier());
        }
        onExpand(player, domainCenter);

        // 3. ЦИКЛ ТЕРРИТОРИИ
        new BukkitRunnable() {
            int tick = 0;
            final int expansionTime = 15; // Время (в тиках) на постройку барьера

            @Override
            public void run() {
                if (tick >= getDomainTicks() || !player.isOnline()) {
                    endDomain(player);
                    this.cancel();
                    return;
                }

                if (!shellBlocksToBuild.isEmpty()) {
                    // Вычисляем, сколько блоков нужно ставить за тик, чтобы успеть
                    int blocksPerTick = (int) Math.ceil((double) shellBlocksToBuild.size() / (15 - tick));
                    if (blocksPerTick <= 0) blocksPerTick = shellBlocksToBuild.size();

                    for (int i = 0; i < blocksPerTick && !shellBlocksToBuild.isEmpty(); i++) {
                        // Достаем блок с самым низким Y (он в конце списка)
                        Location loc = shellBlocksToBuild.remove(shellBlocksToBuild.size() - 1);
                        loc.getBlock().setType(getBarrierMaterial(), false);

                        // Легкое свечение на границе роста барьера
                        if (Math.random() > 0.6) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                // --- ОБНОВЛЕНИЕ ТЕРРИТОРИИ ---
                onDomainTick(player, domainCenter, tick);

                // ГАРАНТИРОВАННОЕ ПОПАДАНИЕ (Sure-Hit Effect)
                if (!isClashing) {
                    for (Entity e : domainCenter.getWorld().getNearbyEntities(domainCenter, getRadius(), getRadius(), getRadius())) {
                        if (e instanceof LivingEntity victim && !e.equals(player)) {
                            if (victim.getLocation().distance(domainCenter) <= getRadius()) {

                                // --- ПРОВЕРКА НА ПРОКЛЯТУЮ ЭНЕРГИЮ (Небесное Проклятие) ---
                                boolean hasCursedEnergy = true;

                                // Если жертва - игрок, проверяем его модель
                                if (victim instanceof Player victimPlayer) {
                                    JJKPlayer jjkVictim = PlayerManager.get(victimPlayer);
                                    if (jjkVictim != null && jjkVictim.getModel() != null) {
                                        if (jjkVictim.getModel().getMaxEnergy() <= 0) {
                                            hasCursedEnergy = false;
                                        }
                                    }
                                }

                                if (hasCursedEnergy) {
                                    onSureHit(player, victim, tick);
                                } else {
                                    //
                                }
                            }
                        }
                    }
                }
                tick++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    // ==========================================
    // ЛОГИКА ЗАВЕРШЕНИЯ (CLEANUP)
    // ==========================================

    protected void endDomain(Player player) {
        onEnd(player);

        JJKPlayer jjkPlayer = PlayerManager.get(player);
        if (jjkPlayer != null) jjkPlayer.setBurnout(getBurnoutTicks());

        cleanupDomain();
    }

    private void abortDomain() {
        cleanupDomain();
    }

    protected void cleanupDomain() {
        restoreWorld();
        cleanupData();
        JJKPlugin.activeDomains.remove(this);

        isCasting = false;
        isActive = false;
        domainCenter = null;
    }

    private void cleanupData() {
        backupBlocks.clear();
        shellBlocksToBuild.clear();
    }

    private void restoreWorld() {
        backupBlocks.forEach((loc, data) -> loc.getBlock().setBlockData(data, false));
    }

    // ==========================================
    // МАТЕМАТИКА БЛОКОВ
    // ==========================================

    private void generateFloorStep(Location center, double currentRadius) {
        if (isClashing) return;
        int ir = (int) Math.ceil(currentRadius);
        Location floorCenter = center.clone().subtract(0, 1, 0);

        for (int x = -ir; x <= ir; x++) {
            for (int z = -ir; z <= ir; z++) {
                Location loc = floorCenter.clone().add(x, 0, z);
                if (loc.distance(floorCenter) <= currentRadius) {
                    Block b = loc.getBlock();
                    if (b.getType() != getFloorMaterial() && b.getType() != Material.BEDROCK) {
                        backupBlocks.putIfAbsent(loc.clone(), b.getBlockData());
                        b.setType(getFloorMaterial(), false);
                    }
                }
            }
        }
    }

    private void prepareDomainShell(Location center, double r, boolean withBarrier) {
        int ir = (int) Math.ceil(r);
        for (int x = -ir; x <= ir; x++) {
            for (int y = -ir; y <= ir; y++) {
                for (int z = -ir; z <= ir; z++) {
                    Location loc = center.clone().add(x, y, z);
                    double dist = loc.distance(center);
                    if (dist > r) continue;

                    Block b = loc.getBlock();
                    if (b.getType() == Material.BEDROCK) continue;

                    // Бэкап
                    backupBlocks.putIfAbsent(loc.clone(), b.getBlockData());

                    if (y <= -2) {
                        b.setType(getFloorMaterial(), false);
                    } else if (dist > r - 1.5 && withBarrier) {
                        // ДОБАВЛЯЕМ В СПИСОК
                        shellBlocksToBuild.add(loc);
                    } else if (withBarrier) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
        shellBlocksToBuild.sort((loc1, loc2) -> Double.compare(loc2.getY(), loc1.getY()));
    }
    public boolean getActive() {return this.isActive; }
}