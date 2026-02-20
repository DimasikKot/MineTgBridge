package com.koolda.mineTgBridge;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MineTgBridge extends JavaPlugin implements Listener {

    private String token;
    private String chatId;
    private String groupLink;
    private Boolean sendAllThanText;
    private int timeCheck;
    private String telegramMessage;
    private String tgCheckMessage;
    private String serverMessage;


    @Override
    public void onEnable() {
        saveDefaultConfig();

        token = getConfig().getString("telegram.token");
        chatId = getConfig().getString("telegram.chat-id");
        groupLink = getConfig().getString("telegram.group-link");
        sendAllThanText = getConfig().getBoolean("telegram.send-all-than-text", false);
        timeCheck = getConfig().getInt("telegram.send-all-than-text", 5); // 5 секунд
        telegramMessage = getConfig().getString("message.telegram");
        tgCheckMessage = getConfig().getString("message.tg-check");
        serverMessage = getConfig().getString("message.server");

        if (token == null || chatId == null) {
            getLogger().severe("Telegram token or chat-id not set!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MineTgBridge enabled");

        startTelegramListener();
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText()
                .serialize(event.message());

        if (message.startsWith("/")) return;

        String player = event.getPlayer().getName();

        String text = serverMessage
                .replace("{player}", player)
                .replace("{message}", message);

        Bukkit.getScheduler().runTaskAsynchronously(this,
                () -> sendToTelegram(text)
        );
    }

    private void sendToTelegram(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String urlString =
                    "https://api.telegram.org/bot" + token +
                            "/sendMessage?chat_id=" + chatId +
                            "&text=" + encodedMessage;

            URL url = URI.create(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            connection.getInputStream().close();
            connection.disconnect();
        } catch (Exception e) {
            getLogger().warning("Failed to send Telegram message: " + e.getMessage());
        }
    }

    private long lastUpdateId = 0;

    private void startTelegramListener() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                this::safeCheckTelegramUpdates,
                0L,
                20L * timeCheck
        );
    }

    private void safeCheckTelegramUpdates() {
        try {
            checkTelegramUpdates();
        } catch (Exception e) {
            getLogger().warning("Telegram listener error: " + e.getMessage());
        }
    }

    private void checkTelegramUpdates() throws Exception {
        String url = "https://api.telegram.org/bot" + token +
                "/getUpdates?offset=" + (lastUpdateId + 1);

        HttpURLConnection connection =
                (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");

        String response = new String(connection.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        JSONObject json = new JSONObject(response);
        JSONArray results = json.getJSONArray("result");

        for (int i = 0; i < results.length(); i++) {
            JSONObject update = results.getJSONObject(i);
            lastUpdateId = update.getLong("update_id");

            if (!update.has("message")) continue;

            JSONObject message = update.getJSONObject("message");

            // игнорируем не текст
            if (!message.has("text") && sendAllThanText) continue;

            // пропускаем только нужную группу
            String chat = message.getJSONObject("chat").get("username").toString();
            if (!chat.equals(groupLink)) continue;

            String text = describeTelegramMessage(message);

            // предотвращение петли, проверка: "не отправлено ли сообщение ботом?"
            if (text.startsWith(tgCheckMessage)) return;

            JSONObject from = message.getJSONObject("from");
            String name = from.optString("username",
                    from.optString("first_name", "TG"));

            sendToMinecraftChat(name, text);
        }
    }

    private String describeTelegramMessage(JSONObject message) {
        if (message.has("text")) {
            return message.getString("text");
        }
        if (message.has("photo")) {
            return "Изображение";
        }
        if (message.has("video")) {
            return "Видео";
        }
        if (message.has("audio")) {
            return "Аудио";
        }
        if (message.has("voice")) {
            return "Голосовое сообщение";
        }
        if (message.has("sticker")) {
            return "Стикер";
        }
        if (message.has("animation")) {
            return "GIF";
        }
        if (message.has("document")) {
            return "Документ";
        }
        return "Сообщение";
    }

    private void sendToMinecraftChat(String user, String message) {
        Bukkit.getScheduler().runTask(this, () -> {
            Component text = Component.text(telegramMessage.replace("{user}", user),
                            NamedTextColor.AQUA)
                    .append(Component.text(message, NamedTextColor.WHITE));

            Bukkit.broadcast(text);
        });
    }
}