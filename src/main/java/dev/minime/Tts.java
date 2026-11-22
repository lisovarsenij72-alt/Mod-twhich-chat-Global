package dev.minime;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Озвучка сообщений через встроенный в Minecraft синтезатор речи (тот же, что у экранного диктора, Ctrl+B).
 * Работает через рефлексию: если синтезатора нет, мод просто молчит.
 * Язык и голос берутся из системных голосов Windows.
 */
public class Tts {
    private static final ExecutorService EX = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "minime-tts"); t.setDaemon(true); return t;
    });
    private static final AtomicInteger PENDING = new AtomicInteger();
    private static Object narrator;
    private static Method say;
    private static boolean threeArgs;
    private static boolean failed;

    private static synchronized boolean init() {
        if (narrator != null) return true;
        if (failed) return false;
        try {
            Class<?> c = Class.forName("com.mojang.text2speech.Narrator");
            narrator = c.getMethod("getNarrator").invoke(null);
            try { say = c.getMethod("say", String.class, boolean.class, float.class); threeArgs = true; }
            catch (NoSuchMethodException e) { say = c.getMethod("say", String.class, boolean.class); threeArgs = false; }
            return true;
        } catch (Throwable t) { failed = true; return false; }
    }

    public static void speak(String user, String text) {
        Config c = MiniMeClient.config;
        if (c == null || !c.ttsEnabled || PENDING.get() >= 4) return;   // не копим очередь при флуде
        String clean = text.replaceAll("https?://\\S+", "ссылка").replaceAll("[\\p{So}\\p{Cs}]", "").trim();
        if (clean.isEmpty()) return;
        String phrase = c.ttsReadNames ? user + ": " + clean : clean;
        float vol = c.ttsVolume;
        PENDING.incrementAndGet();
        EX.execute(() -> {
            try {
                if (init()) {
                    if (threeArgs) say.invoke(narrator, phrase, false, vol);
                    else say.invoke(narrator, phrase, false);
                    Thread.sleep(Math.min(6000, 700 + phrase.length() * 60L)); // примерно дожидаемся конца фразы
                }
            } catch (Throwable ignored) {
            } finally { PENDING.decrementAndGet(); }
        });
    }
}
