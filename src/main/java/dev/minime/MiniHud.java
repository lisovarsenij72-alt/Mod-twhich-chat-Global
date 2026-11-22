package dev.minime;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Комикс-пузыри над мини (на HUD, позиция берётся из мира).
 * Для плоских моделей (tank и т.п.) здесь же рисуется спрайт.
 */
public class MiniHud {
    record Bubble(String user, String text, long born) {}
    private static final List<Bubble> BUBBLES = new CopyOnWriteArrayList<>();

    public static void addMessage(String user, String text) {
        if (text.length() > 120) text = text.substring(0, 120) + "…";
        BUBBLES.add(new Bubble(user, text, System.currentTimeMillis()));
        while (BUBBLES.size() > 12) BUBBLES.remove(0);
    }

    private Vec3 spritePos = null;
    private long lastNs = System.nanoTime();

    public void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Config cfg = MiniMeClient.config;
        if (mc.player == null || !cfg.enabled) return;

        float pt = delta.getGameTimeDeltaPartialTick(false);
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;

        Vec3 eye = mc.player.getEyePosition(pt);
        double yaw = Math.toRadians(mc.player.getViewYRot(pt));
        double pitch = Math.toRadians(mc.player.getViewXRot(pt));
        Vec3 fwd = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 up = new Vec3(fwd.y * left.z, fwd.z * left.x - fwd.x * left.z, -fwd.y * left.x);

        if (cfg.isEntityModel() && MiniManager.mini == null) return; // мини «лопнул» и ещё не вернулся
        boolean entityMode = cfg.isEntityModel();
        Vec3 anchor;
        int size;
        if (entityMode) {
            anchor = MiniManager.mini.getPosition(pt).add(0, MiniManager.baseHeight() * cfg.scale + 0.2, 0);
            size = 16;
        } else {
            double t = System.currentTimeMillis() / 1000.0;
            Vec3 bodyFwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 target = eye.add(bodyFwd.scale(1.8)).add(left.scale(-1.1)).add(0, 0.25 + Math.sin(t * 2.2) * 0.12, 0);
            spritePos = (spritePos == null || spritePos.distanceTo(target) > 24) ? target
                    : spritePos.add(target.subtract(spritePos).scale(Math.min(1f, dt * 5f)));
            anchor = spritePos;
            size = -1;
        }

        Vec3 d = anchor.subtract(eye);
        double depth = d.dot(fwd);
        if (depth < 0.3) return;
        double tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        int w = g.guiWidth(), h = g.guiHeight();
        double ndcX = d.dot(left) / (depth * tanHalf * ((double) w / h));
        double ndcY = d.dot(up) / (depth * tanHalf);
        int sx = (int) (w / 2.0 - ndcX * w / 2.0);
        int sy = (int) (h / 2.0 - ndcY * h / 2.0);
        if (sx < -100 || sx > w + 100 || sy < -100 || sy > h + 100) return;

        if (!entityMode) {
            size = Mth.clamp((int) (90 / depth), 18, 64);
            drawSprite(g, cfg.model, sx - size / 2, sy - size / 2, size);
            drawBubbles(g, mc.font, cfg, sx, sy - size / 2, size, w);
        } else {
            drawBubbles(g, mc.font, cfg, sx, sy, size, w);
        }
    }

    private void drawSprite(GuiGraphicsExtractor g, String model, int x, int y, int s) {
        Identifier tex = Identifier.fromNamespaceAndPath("minime", "textures/mini/" + model + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, s, s, 32, 32, 32, 32);
    }

    private void drawBubbles(GuiGraphicsExtractor g, Font font, Config cfg, int sx, int topY, int size, int screenW) {
        long now = System.currentTimeMillis();
        long ttl = cfg.bubbleSeconds * 1000L;
        List<Bubble> live = new ArrayList<>();
        for (Bubble b : BUBBLES) if (now - b.born < ttl) live.add(b);
        if (live.size() > cfg.maxBubbles) live = live.subList(live.size() - cfg.maxBubbles, live.size());
        if (live.isEmpty()) return;

        boolean dark = "dark".equals(cfg.theme);
        int bFill = dark ? 0x161616 : 0xFFFFFF, bBorder = dark ? 0xFFFFFF : 0x000000;
        int bText = dark ? 0xFFFFFF : 0x000000, bName = dark ? 0xB388FF : 0x9146FF;
        boolean right = sx < screenW * 0.6;
        int maxW = 110;
        int cursorY = topY + size / 2;
        for (int i = live.size() - 1; i >= 0; i--) {
            Bubble b = live.get(i);
            List<FormattedCharSequence> lines = font.split(Component.literal(b.text()), maxW);
            int tw = font.width(b.user());
            for (FormattedCharSequence l : lines) tw = Math.max(tw, font.width(l));
            int bw = tw + 8, bh = (lines.size() + 1) * 10 + 6;

            float age = (now - b.born()) / 180f;
            float sc = age >= 1 ? 1f : 1f + 0.25f * (float) Math.sin(age * Math.PI) - (1 - age) * 0.5f;
            float fade = (ttl - (now - b.born())) < 800 ? (ttl - (now - b.born())) / 800f : 1f;
            if (fade < 0.15f) continue;

            int bx = right ? sx + size / 2 + 8 : sx - size / 2 - 8 - bw;
            int by = cursorY - bh;
            int a = (int) (255 * Mth.clamp(fade, 0f, 1f)) << 24;

            g.pose().pushMatrix();
            g.pose().translate(right ? bx : bx + bw, by + bh);
            g.pose().scale(sc, sc);
            g.pose().translate(right ? -bx : -(bx + bw), -(by + bh));

            g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, a | bBorder);
            g.fill(bx, by, bx + bw, by + bh, a | bFill);
            g.fill(bx - 2, by + 1, bx, by + bh - 1, a | bBorder);
            g.fill(bx + bw, by + 1, bx + bw + 2, by + bh - 1, a | bBorder);
            if (i == live.size() - 1) {
                int tx = right ? bx - 1 : bx + bw - 3, ty = by + bh - 1;
                for (int k = 0; k < 5; k++) {
                    int off = right ? -k : k;
                    g.fill(tx + off, ty + k, tx + off + 4, ty + k + 1, a | bBorder);
                    g.fill(tx + off + 1, ty + k, tx + off + 3 - (k > 2 ? 1 : 0), ty + k, a | bFill);
                }
            }
            g.text(font, b.user(), bx + 4, by + 3, a | bName, false);
            int ly = by + 13;
            for (FormattedCharSequence l : lines) { g.text(font, l, bx + 4, ly, a | bText, false); ly += 10; }
            g.pose().popMatrix();

            cursorY = by - 6;
        }
    }
}
