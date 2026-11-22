package dev.minime;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

/**
 * Мини-сущность: пружинная физика (инерция, раскачка, столкновения с блоками),
 * позиции спереди/сбоку/сзади, улетает в тишине и прилетает при новом сообщении, лопается от удара.
 */
public class MiniManager {
    public static LivingEntity mini;
    private static Vec3 vel = Vec3.ZERO;
    private static long respawnAt = 0;
    private static volatile long lastMsg = System.currentTimeMillis();
    private static boolean cameFromAway = false;
    private static int awayTicks = 0;
    private static final int FAKE_ID = -777001;
    private static final Random RND = new Random();

    public static void onMessage() { lastMsg = System.currentTimeMillis(); }

    /** Высота модели при масштабе 1 (для позиции пузырей). */
    public static double baseHeight() { return mini instanceof MiniChibiEntity ? 1.4 : 1.8; }

    private static boolean wantChibi(Config cfg) {
        return (cfg.model.equals("chibi") || cfg.model.equals("fox")) && MiniMeMain.CHIBI != null;
    }

    public static void tick(Minecraft mc) {
        Config cfg = MiniMeClient.config;
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null || !cfg.enabled || !cfg.isEntityModel()) { remove(); return; }
        long now = System.currentTimeMillis();
        if (now < respawnAt) return;

        boolean idle = cfg.autoAway && now - lastMsg > cfg.idleSeconds * 1000L;
        if (idle && mini == null) return;

        boolean chibi = wantChibi(cfg);
        if (mini != null && (mini.level() != level || mini.isRemoved())) mini = null;
        else if (mini != null && (mini instanceof MiniChibiEntity) != chibi) remove();

        if (mini == null) {
            mini = chibi ? new MiniChibiEntity(MiniMeMain.CHIBI, level)
                         : new MiniEntity(level, new GameProfile(UUID.randomUUID(), "Mini"));
            mini.setId(FAKE_ID);
            mini.setNoGravity(true);
            mini.setSilent(true);
            mini.noPhysics = false;
            Vec3 start = cameFromAway ? awayTarget(p) : target(p, cfg);
            mini.setPos(start.x, start.y, start.z);
            level.addEntity(mini);
            vel = Vec3.ZERO;
            if (!cameFromAway) burst(level, start.add(0, 0.3 * cfg.scale, 0), 10, "poof");
            cameFromAway = false;
            awayTicks = 0;
        }

        if (mini instanceof MiniChibiEntity c) {
            c.fox = cfg.model.equals("fox");
            c.texture = SkinManager.skinFor(mc, cfg.skin).body().texturePath();
        }

        AttributeInstance sc = mini.getAttribute(Attributes.SCALE);
        if (sc != null && Math.abs(sc.getBaseValue() - cfg.scale) > 1e-4) sc.setBaseValue(cfg.scale);

        Vec3 here = mini.position();
        Vec3 tgt;
        if (idle) {
            tgt = awayTarget(p);
            awayTicks++;
            if (here.distanceTo(p.position()) > 14 || awayTicks > 120) {
                remove();
                cameFromAway = true;
                return;
            }
        } else {
            awayTicks = 0;
            tgt = target(p, cfg);
        }
        if (here.distanceTo(tgt) > 60) {
            mini.setPos(tgt.x, tgt.y, tgt.z);
            vel = Vec3.ZERO;
            here = tgt;
        }

        vel = vel.scale(0.8).add(tgt.subtract(here).scale(idle ? 0.03 : 0.05));
        double sp = vel.length();
        if (sp > 1.0) vel = vel.scale(1.0 / sp);
        mini.move(MoverType.SELF, vel);

        float yaw = (float) (Mth.atan2(p.getZ() - mini.getZ(), p.getX() - mini.getX()) * 57.29578) - 90f;
        mini.setYRot(yaw);
        mini.setYHeadRot(yaw);
        mini.setXRot(0f);
    }

    private static Vec3 flatForward(LocalPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private static Vec3 target(LocalPlayer p, Config cfg) {
        double bob = Math.sin(System.currentTimeMillis() / 450.0) * 0.08;
        Vec3 fwd = flatForward(p);
        switch (cfg.position) {
            case "side": {
                Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
                return p.position().add(fwd.scale(0.9)).add(right.scale(1.0)).add(0, 1.2 + bob, 0);
            }
            case "back":
                return p.position().add(fwd.scale(-1.3)).add(0, 1.5 + bob, 0);
            default:
                return p.getEyePosition().add(p.getLookAngle().scale(2.2)).add(0, 0.15 + bob, 0);
        }
    }

    private static Vec3 awayTarget(LocalPlayer p) {
        return p.position().add(flatForward(p).scale(-14)).add(0, 9, 0);
    }

    /** Удар: лопается эффектом из настроек и звуком, возвращается через respawnSeconds. */
    public static void pop() {
        Minecraft mc = Minecraft.getInstance();
        if (mini == null || mc.level == null) return;
        Config cfg = MiniMeClient.config;
        Vec3 c = mini.position().add(0, 0.5 * baseHeight() * cfg.scale, 0);
        burst(mc.level, c, 40, cfg.popEffect);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.4f));
        remove();
        cameFromAway = false;
        respawnAt = System.currentTimeMillis() + cfg.respawnSeconds * 1000L;
    }

    private static void burst(ClientLevel level, Vec3 c, int n, String effect) {
        ParticleOptions main;
        switch (effect) {
            case "snow" -> main = ParticleTypes.SNOWFLAKE;
            case "cherry" -> main = ParticleTypes.CHERRY_LEAVES;
            case "sand" -> main = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
            case "souls" -> main = ParticleTypes.SOUL;
            case "hearts" -> main = ParticleTypes.HEART;
            default -> main = ParticleTypes.POOF;
        }
        for (int i = 0; i < n; i++) {
            double vx = (RND.nextDouble() - 0.5) * 0.3, vy = (RND.nextDouble() - 0.2) * 0.3, vz = (RND.nextDouble() - 0.5) * 0.3;
            level.addParticle(main, c.x, c.y, c.z, vx, vy, vz);
            if (i % 3 == 0) level.addParticle(ParticleTypes.CLOUD, c.x, c.y, c.z, vx * 0.5, vy * 0.5, vz * 0.5);
        }
    }

    public static void remove() {
        if (mini != null) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) level.removeEntity(mini.getId(), Entity.RemovalReason.DISCARDED);
            mini = null;
            vel = Vec3.ZERO;
        }
    }
}
