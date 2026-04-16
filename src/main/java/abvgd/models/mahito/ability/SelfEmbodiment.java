package abvgd.models.mahito.ability;

import abvgd.JJKPlugin;
import abvgd.core.JJKAbilityInfo;
import abvgd.core.JJKDomain;
import abvgd.core.types.ActiveAbility;
import abvgd.manage.PlayerManager;
import abvgd.utils.JJKDamage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;

public class SelfEmbodiment extends JJKDomain {

    private final Map<UUID, List<BlockDisplay>> victimHands = new HashMap<>();
    private final Set<UUID> victimsInside = new HashSet<>();

    @Override
    public int getBurnoutTicks() {
        return 0;
    }

    private final Map<UUID, List<BlockDisplay>> decorations = new HashMap<>();
    private final Set<UUID> victims = new HashSet<>();

    public SelfEmbodiment() {
        super(new JJKAbilityInfo(
                "§0§lSelf-Embodiment",
                Material.GHAST_TEAR,
                30,
                45,
                0,
                false
        ));
    }

    @Override public int getCastTicks() { return 30; }
    @Override public int getDomainTicks() { return 200; } // 10 секунд
    @Override public double getRadius() { return 15.0; }
    @Override public boolean hasBarrier() { return true; }
    @Override public Material getFloorMaterial() { return Material.GRAY_CONCRETE_POWDER; }
    @Override public Material getBarrierMaterial() { return Material.BLACK_CONCRETE; }

    @Override
    protected void onCastTick(Player caster, int tick) {
        World world = caster.getWorld();
        Location loc = caster.getLocation();
        if (tick == 0) {
            playCustomSound("jjk.mahito.domain", caster, 1.0f, 1.0f);
            world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 0.5f);
            world.playSound(loc, Sound.BLOCK_DEEPSLATE_STEP, 1.5f, 0.5f);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 5f, 0.5f);
        }
        world.spawnParticle(Particle.SQUID_INK, loc.add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    protected void onExpand(Player caster, Location center) {
        victims.clear();
        // 1. ЗАПОЛНЯЕМ ТЕРРИТОРИЮ ДЕКОРАЦИЯМИ (Руки из пола)
        List<BlockDisplay> hands = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            double x = (Math.random() - 0.5) * (getRadius() * 1.5);
            double z = (Math.random() - 0.5) * (getRadius() * 1.5);
            Location handLoc = center.clone().add(x, -1, z);

            BlockDisplay hand = center.getWorld().spawn(handLoc, BlockDisplay.class, b -> {
                b.setBlock(Material.GRAY_CONCRETE.createBlockData());
                Transformation t = b.getTransformation();
                // Разные размеры рук для хаотичности
                float h = 2.0f + (float)Math.random() * 3f;
                t.getScale().set(0.6f, h, 0.6f);
                b.setTransformation(t);
                // Случайный наклон
                t.getLeftRotation().set((float)Math.random() * 0.2f, 0, (float)Math.random() * 0.2f, 1f);
            });
            hands.add(hand);
        }
        decorations.put(caster.getUniqueId(), hands);
    }

    private void playTickSound(LivingEntity victim, int tick) {
        float pitch = 1.0f + ((float) tick / getDomainTicks()); // Звук ускоряется к концу
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, pitch);
    }

    @Override
    protected void onSureHit(Player caster, LivingEntity victim, int tick) {
        UUID id = victim.getUniqueId();
        victims.add(id);

        // 1. СТАН И ВЫГОРАНИЕ
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 255, false, false));

        // 2. ЗВУК ТИКАНИЯ (каждую секунду)
        if (tick % 20 == 0) {
            playTickSound(victim, tick);
        }

        // 3. КРУГОВОЙ ТАЙМЕР НАД ГОЛОВОЙ
        drawOverheadTimer(victim, tick);

        // 4. ЭФФЕКТЫ "ИСКАЖЕНИЯ"
        if (tick % 10 == 0) {
            victim.getWorld().spawnParticle(Particle.WITCH, victim.getEyeLocation().add(0, 0.3, 0), 3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void drawOverheadTimer(LivingEntity victim, int tick) {
        Location loc = victim.getEyeLocation().add(0, 0.8, 0); // Позиция над головой
        double radius = 0.5;
        double progress = (double) tick / getDomainTicks(); // От 0.0 до 1.0

        int dots = 15; // Количество точек в круге
        for (int i = 0; i < dots; i++) {
            double angle = i * (2 * Math.PI / dots);
            double pointProgress = (double) i / dots;

            // Если точка круга "пройдена" по времени, меняем её цвет
            Color color = (pointProgress < progress) ? Color.PURPLE : Color.fromRGB(250,220,255);

            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location pLoc = loc.clone().add(x, 0, z);

            victim.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 0.8f));
        }

        if (tick % 5 == 0) {
            victim.getWorld().spawnParticle(Particle.ENCHANT, loc, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onEnd(Player caster) {
        // ФИНАЛЬНЫЙ РАСЧЕТ
        for (UUID id : victims) {
            Entity e = Bukkit.getEntity(id);
            if (e instanceof LivingEntity victim) {
                // Взрыв плоти
                victim.getWorld().spawnParticle(Particle.BLOCK, victim.getEyeLocation(), 150, 0.5, 0.5, 0.5, Material.REDSTONE_BLOCK.createBlockData());
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2f, 0.5f);

                JJKDamage.causeAbilityDamage(victim, caster, 40.0); // Смертельный урон
            }
        }

        // Очистка декораций
        List<BlockDisplay> hands = decorations.remove(caster.getUniqueId());
        if (hands != null) hands.forEach(Entity::remove);

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
    }

    @Override
    protected void onDomainTick(Player caster, Location center, int tick) {
        center.getWorld().spawnParticle(Particle.SCULK_SOUL, center, 20, getRadius(), 2, getRadius(), 0.05);
    }
}