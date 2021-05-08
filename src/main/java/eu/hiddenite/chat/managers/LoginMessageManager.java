package eu.hiddenite.chat.managers;

import com.google.gson.*;
import eu.hiddenite.chat.ChatPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class LoginMessageManager extends Manager implements Listener {
    private final HashSet<UUID> knownUsers = new HashSet<>();

    public LoginMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        loadKnownUsers();
        getProxy().getPluginManager().registerListener(getPlugin(), this);
    }

    @EventHandler
    public void onPostLoginEvent(PostLoginEvent event) {
        boolean hasPlayedBefore = true;
        synchronized (knownUsers) {
            if (!knownUsers.contains(event.getPlayer().getUniqueId())) {
                getLogger().info(event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ") joined for the first time");
                hasPlayedBefore = false;
                knownUsers.add(event.getPlayer().getUniqueId());
                getProxy().getScheduler().runAsync(getPlugin(), this::saveKnownUsers);
            }
        }

        if (getConfig().helloMessage != null && getConfig().helloMessage.length() > 0) {
            event.getPlayer().sendMessage(TextComponent.fromLegacyText(getConfig().helloMessage));
        }

        if (!hasPlayedBefore) {
            formatAndBroadcastMessage(getConfig().welcomeMessageFormat, event.getPlayer());
        } else if (getProxy().getOnlineCount() < getConfig().onlinePlayersLimit) {
            formatAndBroadcastMessage(getConfig().loginMessageFormat, event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        if (getProxy().getOnlineCount() < getConfig().onlinePlayersLimit) {
            formatAndBroadcastMessage(getConfig().logoutMessageFormat, event.getPlayer());
        }
    }

    private void formatAndBroadcastMessage(String rawMessage, ProxiedPlayer player) {
        BaseComponent[] messageComponents = formatText(rawMessage, player);

        Collection<ProxiedPlayer> allPlayers = getProxy().getPlayers();
        allPlayers.forEach((receiver) ->
                receiver.sendMessage(player.getUniqueId(), messageComponents)
        );

        String discordMessage = TextComponent.toPlainText(messageComponents);
        DiscordManager.getInstance().sendMessage(discordMessage, DiscordManager.Style.ITALIC);
    }

    private BaseComponent[] formatText(String format, ProxiedPlayer player) {
        String message = format
                .replace("{NAME}", player.getName())
                .replace("{DISPLAY_NAME}", player.getDisplayName());
        return TextComponent.fromLegacyText(message);
    }

    private void loadKnownUsers() {
        knownUsers.clear();

        File file = new File(getPlugin().getDataFolder(), "known-users.json");
        if (file.exists()) {
            try {
                JsonObject root = new JsonParser().parse(new FileReader(file)).getAsJsonObject();
                JsonArray users = root.getAsJsonArray("users");

                for (JsonElement jsonUser : users) {
                    UUID uuid = UUID.fromString(jsonUser.getAsString());
                    knownUsers.add(uuid);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        getLogger().info("Loaded known-users.json: " + knownUsers.size());
    }

    private void saveKnownUsers() {
        synchronized (knownUsers) {
            File file = new File(getPlugin().getDataFolder(), "known-users.json");

            JsonArray users = new JsonArray();
            for (UUID uuid : knownUsers) {
                users.add(uuid.toString());
            }

            JsonObject root = new JsonObject();
            root.add("users", users);

            try (Writer writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().create();
                gson.toJson(root, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        getLogger().info("Saved known-users.json");
    }
}
