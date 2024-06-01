package eu.hiddenite.chat.managers;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class PublicChatManager extends Manager {
    public static final String GLOBAL_CHANNEL = "@global";
    public static final String VOID_CHANNEL = "@void";
    public static final String MUTED_CHANNEL = "@muted";

    private static final String URL_REGEX = "^(http:\\/\\/|https:\\/\\/)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$";

    public PublicChatManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().registerListener(this);

        getPlugin().registerCommand("chat", new ChatCommand(this));
        getPlugin().registerCommand("me", new MeCommand(this));
    }

    @Override
    public void onLoad() {
        List<String> servers = new ArrayList<>();

        for (String channel : getConfig().publicChat.channels.keySet()) {
            if (channel.contains("@")) {
                getConfig().publicChat.channels.remove(channel);
                getLogger().warn("Chat channels can't contain \"@\" characters. Channel: {} has not been loaded.", channel);

                continue;
            }

            for (String server : getConfig().publicChat.channels.get(channel)) {
                if (servers.contains(server)) {
                    getConfig().publicChat.channels.get(channel).remove(server);

                    getLogger().warn("Servers can't be in more than one channel. Server: {} has not been added to channel: {}.", server, channel);
                    continue;
                }

                servers.add(server);
            }
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.getMessage().startsWith("/")) {
            return;
        }

        event.setResult(PlayerChatEvent.ChatResult.denied());

        Player player = event.getPlayer();
        String message = event.getMessage();

        boolean isGlobalMessage = false;
        if (message.startsWith("!") && player.hasPermission(ChatPlugin.GLOBAL_CHAT_PERMISSION)) {
            isGlobalMessage = true;
            message = message.substring(1).trim();
        }

        String upperMessage = message.toUpperCase();
        for (String blockedMessage : getConfig().moderation.blockedMessages) {
            if (upperMessage.contains(blockedMessage.toLowerCase())) {
                return;
            }
        }

        if (player.getCurrentServer().isEmpty()) {
            return;
        }

        if (getConfig().moderation.mute.enabled && player.hasPermission(ChatPlugin.IS_MUTED_PERMISSION) && !player.hasPermission(ChatPlugin.BYPASS_PERMISSION)) {
            sendMessage(player, message, MUTED_CHANNEL);

            if (!getConfig().moderation.mute.errorMutedPublic.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().moderation.mute.errorMutedPublic));
            }
            return;
        }

        sendMessage(player, message, isGlobalMessage ? GLOBAL_CHANNEL : getChannel(player.getCurrentServer().get().getServerInfo().getName()));
    }

    public void sendMessage(CommandSource source, String message, String channel) {
        message = formatURLs(source, message);

        String formattedMessage;
        if (source instanceof Player player) {
            formattedMessage = formatMessage(player, getChatFormat(player), message);
        } else {
            formattedMessage = message;
        }

        sendFormattedMessage(formattedMessage, channel);
    }

    public void sendActionMessage(Player source, String message, String channel) {
        String actionFormat = getActionFormat(source);
        String formattedMessage = formatMessage(source, actionFormat, formatURLs(source, message));

        sendFormattedMessage(formattedMessage, channel);
    }

    public void sendFormattedMessage(String message, String channel) {
        Collection<Player> allPlayers = getProxy().getAllPlayers();

        String detailedMessage = "(" + channel + ") " + message;
        Component component = MiniMessage.miniMessage().deserialize(message);
        Component detailedComponent = MiniMessage.miniMessage().deserialize(detailedMessage);

        // Send message to console
        getLogger().info(PlainTextComponentSerializer.plainText().serialize(detailedComponent));

        // Send message to players
        List<String> channels = getConfig().publicChat.channels.getOrDefault(channel, Collections.emptyList());
        for (Player target : allPlayers) {
            if (target.getCurrentServer().isEmpty()) {
                continue;
            }
            if (channel.equals(GLOBAL_CHANNEL) || channels.contains(target.getCurrentServer().get().getServerInfo().getName())) {
                target.sendMessage(component);
            } else if (target.hasPermission(ChatPlugin.GLOBAL_CHAT_PERMISSION)) {
                target.sendMessage(detailedComponent);
            }
        }

        // Send message to Discord
        String discordMessage = PlainTextComponentSerializer.plainText().serialize((getConfig().discord.detailed ? detailedComponent : component));
        getPlugin().getDiscordManager().sendMessage(discordMessage, DiscordManager.Style.NORMAL, channel);
    }

    public String getChannel(String server) {
        for (Map.Entry<String, List<String>> entry : getConfig().publicChat.channels.entrySet()) {
            if (entry.getValue().contains(server)) {
                return entry.getKey();
            }
        }

        return VOID_CHANNEL;
    }

    private String getChatFormat(Player player) {
        return getFormat(player, getConfig().publicChat.chatFormat);
    }

    private String getActionFormat(Player player) {
        return getFormat(player, getConfig().publicChat.actionFormat);
    }

    private static String formatMessage(Player sender, String format, String message) {
        return format
                .replace("{NAME}", sender.getUsername())
                .replace("{MESSAGE}", message);
    }

    private static String getFormat(Player player, LinkedHashMap<String, String> formats) {
        String last = null;
        for (String key : formats.keySet()) {
            last = key;
            if (player.hasPermission("hiddenite.chat." + key) || player.hasPermission("yoctochat." + key)) {
                return formats.get(key);
            }
        }
        return formats.get(last);
    }

    public String formatURLs(CommandSource source, String message) {
        if (!getConfig().moderation.urls.enabled) {
            return message;
        }

        List<String> formattedMessage = new ArrayList<>();
        for (String part : message.split(" ")) {
            if (!part.matches(URL_REGEX)) {
                formattedMessage.add(part);
                continue;
            }

            try {
                URI uri = new URI(part);

                if (getConfig().moderation.urls.restricted && !source.hasPermission(ChatPlugin.BYPASS_PERMISSION) && !getConfig().moderation.urls.allowedHosts.contains(uri.getHost())) {
                    formattedMessage.add(part);
                    continue;
                }

                formattedMessage.add(MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(part).clickEvent(ClickEvent.openUrl(uri.toURL()))));
            } catch (URISyntaxException | MalformedURLException exception) {
                formattedMessage.add(part);
            }
        }

        return String.join(" ", formattedMessage);
    }

}
