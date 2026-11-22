package dev.minime;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Чат Twitch через IRC. Токен необязателен (без него — анонимное чтение). Одновременно живёт только одно подключение. */
public class TwitchChat {
    private final AtomicInteger generation = new AtomicInteger();
    private volatile Socket socket;
    private final BiConsumer<String, String> onMessage;
    private final Consumer<String> onStatus;

    public TwitchChat(BiConsumer<String, String> onMessage, Consumer<String> onStatus) {
        this.onMessage = onMessage; this.onStatus = onStatus;
    }

    public synchronized void connect(String channel, String nick, String token) {
        int my = generation.incrementAndGet();   // все старые потоки сами завершатся
        closeSocket();
        if (channel == null || channel.isBlank() || channel.equals("your_channel")) {
            onStatus.accept("Twitch: укажите channel (меню на клавише O или config/minime.json)");
            return;
        }
        String ch = channel.toLowerCase().replace("#", "").trim();
        boolean anon = token == null || token.isBlank() || nick == null || nick.isBlank();
        String nk = anon ? "justinfan" + (10000 + (int) (Math.random() * 80000)) : nick.toLowerCase().trim();
        String tk = anon ? null : token.trim().replaceFirst("(?i)^oauth:", "");
        Thread t = new Thread(() -> loop(my, ch, nk, tk), "minime-twitch-" + my);
        t.setDaemon(true);
        t.start();
    }

    public synchronized void disconnect() {
        generation.incrementAndGet();
        closeSocket();
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
    }

    private void loop(int my, String channel, String nick, String token) {
        int delay = 2000;
        boolean announced = false;
        while (generation.get() == my) {
            try (Socket s = SSLSocketFactory.getDefault().createSocket("irc.chat.twitch.tv", 6697)) {
                synchronized (this) { if (generation.get() != my) return; socket = s; }
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                Writer out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
                send(out, token == null ? "PASS SCHMOOCHIEPOO" : "PASS oauth:" + token);
                send(out, "NICK " + nick);
                send(out, "JOIN #" + channel);
                String line;
                while (generation.get() == my && (line = in.readLine()) != null) {
                    if (line.startsWith("PING")) { send(out, "PONG" + line.substring(4)); continue; }
                    if (line.contains("authentication failed") || line.contains("Improperly formatted auth")) {
                        onStatus.accept("Twitch: неверный токен или логин");
                        return;
                    }
                    if (line.contains(" 001 ")) {
                        delay = 2000;
                        if (!announced) { announced = true; onStatus.accept("Twitch: подключено к #" + channel); }
                    }
                    int p = line.indexOf(" PRIVMSG ");
                    if (p > 0 && line.startsWith(":") && generation.get() == my) {
                        String user = line.substring(1, line.indexOf('!'));
                        int c = line.indexOf(" :", p);
                        if (c > 0) onMessage.accept(user, line.substring(c + 2));
                    }
                }
            } catch (Exception ignored) {}
            if (generation.get() != my) return;
            try { Thread.sleep(delay); } catch (InterruptedException e) { return; }
            delay = Math.min(delay * 2, 30000);
        }
    }

    private static void send(Writer w, String s) throws IOException { w.write(s + "\r\n"); w.flush(); }
}
