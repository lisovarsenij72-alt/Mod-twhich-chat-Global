package dev.minime;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/** Меню кастомизации (клавиша O). */
public class MiniScreen extends Screen {
    private static final float[] SIZES = {0.15f, 0.2f, 0.3f, 0.4f, 0.5f, 0.75f, 1.0f};
    private static final String[] FX = {"poof", "snow", "cherry", "sand", "souls", "hearts"};
    private static final String[] FXN = {"дым", "снег", "сакура", "песок", "души", "сердца"};
    private static final int[] IDLE = {5, 10, 20, 40};
    private static final String[] POS = {"front", "side", "back"};
    private static final int[] SECS = {4, 8, 12, 20};
    private EditBox channelBox;

    public MiniScreen() { super(Component.literal("Mini Me")); }

    private Config cfg() { return MiniMeClient.config; }

    private void add(int x, int y, int w, Supplier<String> label, Runnable onClick) {
        addRenderableWidget(Button.builder(Component.literal(label.get()), b -> {
            onClick.run();
            b.setMessage(Component.literal(label.get()));
        }).bounds(x, y, w, 20).build());
    }

    @Override
    protected void init() {
        int colW = 150, gap = 22;
        int xl = this.width / 2 - colW - 4, xr = this.width / 2 + 4;
        int y = this.height / 2 - 105;

        add(xl, y, colW, () -> "Модель: " + cfg().model, () -> {
            List<String> all = cfg().allModels();
            cfg().model = all.get((all.indexOf(cfg().model) + 1) % all.size());
        });
        add(xl, y + gap, colW, () -> "Скин (self): " + cfg().skin, () -> {
            List<String> all = cfg().allSkins();
            cfg().skin = all.get((all.indexOf(cfg().skin) + 1) % all.size());
        });
        add(xl, y + gap * 2, colW, () -> "Размер: " + cfg().scale, () -> {
            int i = 0;
            for (int k = 0; k < SIZES.length; k++) if (Math.abs(SIZES[k] - cfg().scale) < 0.001f) i = k;
            cfg().scale = SIZES[(i + 1) % SIZES.length];
        });
        add(xl, y + gap * 3, colW, () -> "Позиция: " + switch (cfg().position) { case "side" -> "сбоку"; case "back" -> "сзади"; default -> "спереди"; }, () -> {
            int i = 0;
            for (int k = 0; k < POS.length; k++) if (POS[k].equals(cfg().position)) i = k;
            cfg().position = POS[(i + 1) % POS.length];
        });
        add(xl, y + gap * 4, colW, () -> "Мини: " + (cfg().enabled ? "вкл" : "выкл"), () -> cfg().enabled = !cfg().enabled);
        add(xl, y + gap * 5, colW, () -> "Улетает в тишине: " + (cfg().autoAway ? "да" : "нет"), () -> cfg().autoAway = !cfg().autoAway);

        add(xr, y, colW, () -> "Пузыри, сек: " + cfg().bubbleSeconds, () -> {
            int i = 0;
            for (int k = 0; k < SECS.length; k++) if (SECS[k] == cfg().bubbleSeconds) i = k;
            cfg().bubbleSeconds = SECS[(i + 1) % SECS.length];
        });
        add(xr, y + gap, colW, () -> "Озвучка: " + (cfg().ttsEnabled ? "вкл" : "выкл"), () -> cfg().ttsEnabled = !cfg().ttsEnabled);
        add(xr, y + gap * 2, colW, () -> "Читать ники: " + (cfg().ttsReadNames ? "да" : "нет"), () -> cfg().ttsReadNames = !cfg().ttsReadNames);
        add(xr, y + gap * 3, colW, () -> "Возврат, сек: " + cfg().respawnSeconds, () ->
            cfg().respawnSeconds = cfg().respawnSeconds >= 10 ? 2 : cfg().respawnSeconds + 2);

        add(xr, y + gap * 4, colW, () -> "Тишина до ухода: " + cfg().idleSeconds + " с", () -> {
            int i = 0;
            for (int k = 0; k < IDLE.length; k++) if (IDLE[k] == cfg().idleSeconds) i = k;
            cfg().idleSeconds = IDLE[(i + 1) % IDLE.length];
        });

        add(xr, y + gap * 5, colW, () -> {
            int i = 0;
            for (int k = 0; k < FX.length; k++) if (FX[k].equals(cfg().popEffect)) i = k;
            return "Лопается: " + FXN[i];
        }, () -> {
            int i = 0;
            for (int k = 0; k < FX.length; k++) if (FX[k].equals(cfg().popEffect)) i = k;
            cfg().popEffect = FX[(i + 1) % FX.length];
        });
        add(xl, y + gap * 6, colW, () -> "Тема: " + ("dark".equals(cfg().theme) ? "чёрная" : "белая"), () -> {
            cfg().theme = "dark".equals(cfg().theme) ? "light" : "dark";
            this.rebuildWidgets();
        });

        int fullW = colW * 2 + 8;
        channelBox = new EditBox(this.font, xl, y + gap * 7 + 6, fullW - 84, 20, Component.literal("Канал Twitch"));
        channelBox.setMaxLength(40);
        channelBox.setValue(cfg().channel);
        addRenderableWidget(channelBox);

        addRenderableWidget(Button.builder(Component.literal("Connect"), b -> {
            cfg().channel = channelBox.getValue().trim();
            cfg().save();
            MiniMeClient.twitch.connect(cfg().channel, cfg().twitchNick, cfg().oauthToken);
        }).bounds(xl + fullW - 80, y + gap * 7 + 6, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(xl, y + gap * 8 + 10, fullW, 20).build());
    }

    @Override
    public void onClose() {
        cfg().save();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        boolean dark = "dark".equals(cfg().theme);
        // подложка под меню в выбранной теме
        int top = this.height / 2 - 131, bottom = this.height / 2 + 107;
        g.fill(this.width / 2 - 165, top, this.width / 2 + 165, bottom, dark ? 0xDD0B0B0B : 0xDDF4F4F4);
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.centeredText(this.font, Component.literal("Mini Me — настройки"), this.width / 2, this.height / 2 - 123,
                dark ? 0xFFFFFFFF : 0xFF111111);
    }
}
