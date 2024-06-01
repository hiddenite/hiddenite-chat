package eu.hiddenite.chat.managers;

import com.google.gson.*;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class LoginMessageManager extends Manager {
    private final HashSet<UUID> knownUsers = new HashSet<>();

    public LoginMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        loadKnownUsers();
        getPlugin().registerListener(this);
    }

    @Override
    public void onLoad() {

    }

    @Subscribe
    public void onPostLoginEvent(PostLoginEvent event) {
        boolean hasPlayedBefore = true;
        synchronized (knownUsers) {
            if (!knownUsers.contains(event.getPlayer().getUniqueId())) {
                getLogger().info(event.getPlayer().getUsername() + " (" + event.getPlayer().getUniqueId() + ") joined for the first time");
                hasPlayedBefore = false;
                knownUsers.add(event.getPlayer().getUniqueId());
                getProxy().getScheduler().buildTask(getPlugin(), this::saveKnownUsers).schedule();
            }
        }

        if (getConfig().hello != null && !getConfig().hello.isEmpty()) {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(getConfig().hello));
        }

        if (!hasPlayedBefore) {
            formatAndBroadcastMessage(getConfig().login.welcomeMessage, event.getPlayer());
        } else if (getProxy().getPlayerCount() < getConfig().login.onlinePlayersLimit) {
            formatAndBroadcastMessage(getConfig().login.loginMessage, event.getPlayer());
        }
    }

    @Subscribe
    public void onPlayerDisconnectEvent(DisconnectEvent event) {
        if (getProxy().getPlayerCount() < getConfig().login.onlinePlayersLimit) {
            if (event.getPlayer().getCurrentServer().isEmpty()) {
                return;
            }

            formatAndBroadcastMessage(getConfig().login.logoutMessage, event.getPlayer());
        }
    }

    private void formatAndBroadcastMessage(String rawMessage, Player player) {
        Component messageComponent = formatText(rawMessage, player);

        Collection<Player> allPlayers = getProxy().getAllPlayers();
        allPlayers.forEach((receiver) ->
                receiver.sendMessage(messageComponent)
        );

        String discordMessage = PlainTextComponentSerializer.plainText().serialize(messageComponent);
        getPlugin().getDiscordManager().sendMessage(discordMessage, DiscordManager.Style.ITALIC, PublicChatManager.GLOBAL_CHANNEL);
    }

    private Component formatText(String format, Player player) {
        String message = format
                //.replace("{DISPLAY_NAME}", player.getDisplayName())
                .replace("{NAME}", player.getUsername());
        return MiniMessage.miniMessage().deserialize(message);
    }

    private void loadKnownUsers() {
        knownUsers.clear();

        File file = new File(getPlugin().getDataDirectory(), "known-users.json");
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
            File file = new File(getPlugin().getDataDirectory(), "known-users.json");

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
