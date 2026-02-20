package com.koolda.mineTgBridge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
    private String telegramMessage;
    private String serverMessage;


    @Override
    public void onEnable() {
        saveDefaultConfig();

        token = getConfig().getString("telegram.token");
        chatId = getConfig().getString("telegram.chat-id");
        telegramMessage = getConfig().getString("message.telegram");
        serverMessage = getConfig().getString("message.server");

        if (token == null || chatId == null) {
            getLogger().severe("Telegram token or chat-id not set!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("JoinSendTg enabled");

        startTelegramListener();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().startsWith("/")) return;

        String player = event.getPlayer().getName();
        String message = event.getMessage();

        String text = serverMessage.replace("{player}", player).replace("{message}", message);

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
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            while (true) {
                try {
                    checkTelegramUpdates();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    getLogger().warning("Telegram listener error: " + e.getMessage());
                }
            }
        });
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

            // ❌ игнорируем не текст
            if (!message.has("text")) continue;

            // ❌ только нужная группа
            String chat = message.getJSONObject("chat").get("id").toString();
            if (!chat.equals(chatId)) continue;

            String text = message.getString("text");

            JSONObject from = message.getJSONObject("from");
            String name = from.optString("username",
                    from.optString("first_name", "TG"));

            sendToMinecraftChat(name, text);
        }
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