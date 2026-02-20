
[Русский](/README.md) | **English**

---

# MineTgBridge

**MineTgBridge** is a Minecraft (Paper / Spigot) plugin that **synchronizes chat between a Minecraft server and a Telegram group**.

Messages sent in Minecraft appear in Telegram, and messages from Telegram are broadcast to the Minecraft chat in real time.

---

## ✨ Features

- 🔁 Two-way chat synchronization:
  - Minecraft → Telegram
  - Telegram → Minecraft
- 👤 Displays Telegram usernames in Minecraft chat
- 🛑 Message loop protection (bot ignores its own messages)
- 🖼️ Support for non-text messages (photos, stickers, voice, etc.)
- ⚙️ Customizable message formats
- ⏱ Configurable update interval

---

## ⚙️ Configuration (`config.yml`)

```yaml
# v1.4 - config version, update if you use an older one

telegram:
  token: "BOT_TOKEN"        # Telegram bot token (BotFather)
  chat-id: "@group_link"    # @group_link or numeric ID
  group-link: "group_link"  # Group username WITHOUT @
  send-all-than-text: true  # Send non-text messages as text
  time-check: 5             # Update interval (seconds)

message:
  telegram: "<{user}> "     # Telegram → Minecraft format
  tg-check: "[TG"           # Loop protection prefix
  server: "<{player}> {message}" # Minecraft → Telegram format
````

---

## 🔧 Configuration Options

### `telegram.token`

Your Telegram bot token.
Get it from **@BotFather**.

---

### `telegram.chat-id`

Chat where messages will be sent.

Examples:

* Group: `@group_link`
* Private chat: `123456789`

---

### `telegram.group-link`

Telegram group username **without `@`**.
Used to filter incoming messages.

---

### `telegram.send-all-than-text`

Controls handling of **non-text messages**:

* `true` — send description (`Image`, `Sticker`, `Voice message`)
* `false` — ignore them

---

### `telegram.time-check`

Interval (in seconds) between Telegram API checks.

Recommended: `3–10` seconds.

---

### `message.telegram`

Format for messages **from Telegram to Minecraft**.

Placeholders:

* `{user}` — Telegram username

---

### `message.server`

Format for messages **from Minecraft to Telegram**.

Placeholders:

* `{player}` — Minecraft nickname
* `{message}` — chat message

---

### `message.tg-check`

Marker string to prevent message loops
(bot will ignore messages starting with this prefix).

---

## 📦 Installation

1. Download `MineTgBridge.jar`
2. Put it into the `plugins` folder
3. Start the server (config will be generated)
4. Edit `config.yml`
5. Restart the server

---

## 🧩 Requirements

* Minecraft **Paper / Spigot**
* Java **17+**
* Access to `api.telegram.org`

---

## 📄 License

This project is licensed under the **MIT License**.
