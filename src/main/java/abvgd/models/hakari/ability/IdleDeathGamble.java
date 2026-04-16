package abvgd.models.hakari.ability;
import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IdleDeathGamble extends JJKDomain {

    private final Random random = new Random();
    private final List<BlockDisplay> activeDisplays = new ArrayList<>();
    private boolean isFinished = false;
    private final List<BlockDisplay> decorations = new ArrayList<>();

    public IdleDeathGamble() { super(new JJKAbilityInfo(
            "IdleDeathGamble",
            Material.EMERALD,0,20,0.0,false
    )); }

    @Override public int getCastTicks() { return 15; }
    @Override public int getBurnoutTicks() { return 0; }
    @Override public double getRadius() { return 15; }
    @Override public boolean hasBarrier() { return true; }
    @Override public Material getFloorMaterial() { return Material.WHITE_CONCRETE_POWDER; }
    @Override public Material getBarrierMaterial() { return Material.WHITE_STAINED_GLASS; }
    @Override protected void onDomainTick(Player caster, Location center, int tick) {}
    @Override protected void onSureHit(Player caster, LivingEntity victim, int tick) {}

    @Override public int getDomainTicks() { return 80; }

    @Override
    protected void onCastTick(Player caster, int tick) {
        Location loc = caster.getLocation().add(0, 1, 0);
        // Сужающийся радиус частиц (от 3 блоков до 0)
        double radius = 3.0 * (1.0 - (double) tick / getCastTicks());

        if (tick == 0) {
            caster.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.2f);
            caster.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f);
            caster.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.5f);
        }

        for (int i = 0; i < 3; i++) {
            double angle = tick * 0.4 + (i * (2 * Math.PI / 3));
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Розово-фиолетовые частицы (цвет Хакари)
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, (double)tick/10, z), 2, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 20, 147), 1.5f));
        }

        // Нарастающий звук
        float pitch = 0.5f + ((float) tick / getCastTicks());
        caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, pitch);

        if (tick == getCastTicks() - 1) {
            caster.getWorld().spawnParticle(Particle.FLASH, loc, 3, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    protected void onExpand(Player caster, Location center) {
        this.isFinished = false;

        // Спавним декоративные автоматы по кругу (на границе территории)
        double radius = getRadius() - 3.5;
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 8) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location spawnLoc = center.clone().add(x, 0, z);
            spawnLoc.setY(center.getY()-1); // Ставим на пол

            BlockDisplay machine = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, bd -> {
                // Разные цвета для автоматов
                Material mat = switch (random.nextInt(4)) {
                    case 0 -> Material.RED_CONCRETE;
                    case 1 -> Material.BLACK_CONCRETE;
                    case 2 -> Material.GREEN_CONCRETE;
                    default -> Material.ORANGE_CONCRETE;
                };
                bd.setBlock(mat.createBlockData());
                // Поворачиваем их лицом к центру
                Vector dir = center.toVector().subtract(spawnLoc.toVector()).normalize();
                bd.setRotation((float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ())), 0);

                bd.setTransformation(new Transformation(
                        new org.joml.Vector3f(-0.5f, 0f, -0.2f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(1.0f, 2.2f, 0.6f),
                        new org.joml.Quaternionf()
                ));
            });
            decorations.add(machine);
        }

        // Запуск твоего роллинга (три гигантских блока)
        boolean isJackpot = (random.nextInt(4) == 0);
        spawnGiantCenterBlocks(caster, center);
        runGiantBlockAnimation(caster, generateResult(isJackpot), isJackpot);
    }

    private void spawnGiantCenterBlocks(Player caster, Location center) {
        Location displayBase = center.clone().add(0, 4, 0);
        for (int i = 0; i < 3; i++) {
            double offset = (i - 1) * 3.0;
            Location spawnLoc = displayBase.clone().add(offset, 0, 0);
            BlockDisplay bd = caster.getWorld().spawn(spawnLoc, BlockDisplay.class, display -> {
                display.setBlock(Material.WHITE_CONCRETE.createBlockData());
                display.setTransformation(new Transformation(
                        new org.joml.Vector3f(-1.5f, -1.5f, -1.5f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(2.0f, 2.0f, 2.0f),
                        new org.joml.Quaternionf()
                ));
            });
            activeDisplays.add(bd);
        }
    }

    private void runGiantBlockAnimation(Player caster, int[] result, boolean isJackpot) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (isFinished || !getActive() || activeDisplays.size() < 3) {
                    this.cancel();
                    return;
                }

                // 1. ФАЗА: Крутятся все
                if (tick < 30) {
                    for (int i = 0; i < 3; i++) updateBlock(activeDisplays.get(i), random.nextInt(4) + 1);
                    if (tick % 4 == 0) caster.playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.2f);
                }

                // 2. ФАЗА: Стоп ПЕРВОГО блока
                else if (tick < 50) {
                    updateBlock(activeDisplays.get(0), result[0]);
                    updateBlock(activeDisplays.get(1), random.nextInt(4) + 1);
                    updateBlock(activeDisplays.get(2), random.nextInt(4) + 1);
                    if (tick == 30) playLockSound(caster, 0.8f);
                }

                // 3. ФАЗА: Стоп ВТОРОГО блока
                else if (tick < 70) {
                    updateBlock(activeDisplays.get(1), result[1]);
                    updateBlock(activeDisplays.get(2), random.nextInt(4) + 1);
                    if (tick == 50) playLockSound(caster, 1.0f);
                }

                // 4. ФАЗА: ФИНАЛ (Стоп ТРЕТЬЕГО блока)
                else if (tick == 70) {
                    updateBlock(activeDisplays.get(2), result[2]);
                    playLockSound(caster, 1.2f);

                    if (isJackpot) {
                        jackpotFunction(caster);
                        caster.playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
                    } else {
                        caster.playSound(caster.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 0.5f);
                    }

                    this.cancel();
                    return;
                }

                tick++;
            }
        }.runTaskTimer(JJKPlugin.getInstance(), 0, 1);
    }

    private void playLockSound(Player p, float pitch) {
        p.playSound(p.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, pitch);
    }

    private int[] generateResult(boolean win) {
        if (win) {
            // Если джекпот, выбираем одно случайное число от 1 до 4 для всех слотов
            int n = random.nextInt(4) + 1;
            return new int[]{n, n, n};
        }

        // Если проигрыш, генерируем числа пока они не будут РАЗНЫМИ
        int n1 = random.nextInt(4) + 1;
        int n2 = random.nextInt(4) + 1;
        int n3 = random.nextInt(4) + 1;

        // Проверка: если вдруг рандом выдал три одинаковых при проигрыше,
        // просто меняем последнее число
        if (n1 == n2 && n1 == n3) {
            n3 = (n1 % 4) + 1;
        }

        return new int[]{n1, n2, n3};
    }

    private void updateBlock(BlockDisplay bd, int type) {
        Material mat = switch (type) {
            case 1 -> Material.RED_CONCRETE;
            case 2 -> Material.BLACK_CONCRETE;
            case 3 -> Material.GREEN_CONCRETE;
            default -> Material.ORANGE_CONCRETE;
        };
        bd.setBlock(mat.createBlockData());
    }
    // JACKPOT!!!
    private void jackpotFunction(Player caster) {
        playCustomSound("jjk.hakari.jackpot", caster, 0.1f, 1.0f); // музло подрубаем
        caster.sendMessage("§b§lJACKPOT!");
        caster.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, caster.getLocation(), 100, 1, 2, 1, 0.5);
        caster.getWorld().spawnParticle(Particle.FLASH, caster.getLocation(), 5, 1, 1, 1, 0);
        caster.playSound(caster.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.2f);

        // 3. Выдача баффов (Бессмертие через регенерацию и сопротивление)
        // 1500 тиков = 75 секунд
        caster.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1500, 15, false, false));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1500, 5, false, false));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1500, 1, false, false));

        // 4. Логика JJK (Энергия и Техника)
        JJKPlayer jjkPlayer = PlayerManager.get(caster); // Использую твой менеджер из кода выше
        if (jjkPlayer != null) {
            // Восполняем энергию на максимум
            jjkPlayer.setEnergy(jjkPlayer.getMaxEnergy());

            // Сбрасываем Burnout (выгорание техники), чтобы можно было сразу кастовать снова
            jjkPlayer.setBurnout(0);

            // Запускаем поток частиц вокруг игрока на время действия джекпота
            new BukkitRunnable() {
                int timer = 0;
                @Override
                public void run() {
                    if (!caster.isOnline() || timer >= 75) { // 75 итераций по 20 тиков
                        this.cancel();
                        return;
                    }

                    caster.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0, 1, 0), 15, 0.5, 0.8, 0.5, 0.1);
                    caster.getWorld().spawnParticle(Particle.COMPOSTER, caster.getLocation().add(0, 1, 0), 5, 0.4, 0.7, 0.4, 0.05);
                    caster.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, caster.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.1);

                    // Каждые 2 секунды полностью восстанавливаем ману/энергию
                    if (timer % 2 == 0) {
                        jjkPlayer.setEnergy(jjkPlayer.getMaxEnergy());
                    }

                    timer++;
                }
            }.runTaskTimer(JJKPlugin.getInstance(), 0, 20);
        }
    }

    private void cleanup() {
        activeDisplays.forEach(display -> {
            if (display != null && display.isValid()) display.remove();
        });
        activeDisplays.clear();
    }

    @Override
    protected void onEnd(Player caster) {
        this.isFinished = true;
        cleanup();

        decorations.forEach(Entity::remove);
        decorations.clear();
    }
}