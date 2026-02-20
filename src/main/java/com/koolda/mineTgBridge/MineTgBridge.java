package com.koolda.mineTgBridge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String text = telegramMessage.replace("{player}", playerName);

        // Отправка в Telegram
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> sendToTelegram(text));

        // Сообщение в чат сервера
        sendToServerChat();
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

    private void sendToServerChat() {
        Component message = Component.text(serverMessage, NamedTextColor.YELLOW);

        Bukkit.getServer().broadcast(message);
    }

    //    @Override
    //    public void onDisable() {
    //        Plugin shutdown logic
    //    }
}