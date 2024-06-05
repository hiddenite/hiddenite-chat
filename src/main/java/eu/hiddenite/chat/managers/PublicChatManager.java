package eu.hiddenite.chat.managers;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PublicChatManager extends Manager {
    public static final String GLOBAL_CHANNEL = "@global";
    public static final String VOID_CHANNEL = "@void";
    public static final String MUTED_CHANNEL = "@muted";

    private static final String URL_REGEX = "(http:\\/\\/|https:\\/\\/)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.[^\\s]*)?(?=\\s|$)";

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
        if (message.startsWith("!") && player.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION)) {
            isGlobalMessage = true;
            message = message.substring(1).trim();
        }

        String upperMessage = message.toUpperCase();
        for (String blockedMessage : getConfig().moderation.blockedMessages) {
            if (upperMessage.contains(blockedMessage.toUpperCase())) {
                return;
            }
        }

        if (getConfig().moderation.mute.enabled && player.hasPermission(ChatPlugin.IS_MUTED_PERMISSION) && !player.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION)) {
            sendMessage(player, message, MUTED_CHANNEL);

            if (!getConfig().moderation.mute.errorMutedPublic.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().moderation.mute.errorMutedPublic));
            }
            return;
        }

        String channel = player.getCurrentServer().isEmpty() ? VOID_CHANNEL : (isGlobalMessage ? GLOBAL_CHANNEL : getChannel(player.getCurrentServer().get().getServerInfo().getName()));

        sendMessage(player, message, channel);
    }

    public void sendMessage(Player source, String message, String channel) {
        Component formattedMessage = formatMessage(source, getChatFormat(source), formatURLs(source, message));
        sendFormattedMessage(formattedMessage, channel);
    }

    public void sendActionMessage(Player source, String message, String channel) {
        Component formattedMessage = formatMessage(source, getActionFormat(source), formatURLs(source, message));
        sendFormattedMessage(formattedMessage, channel);
    }

    public void sendFormattedMessage(Component component, String channel) {
        Collection<Player> allPlayers = getProxy().getAllPlayers();

        TextComponent message = Component.text().append(component).build();
        TextComponent detailedMessage = Component.text("(" + channel + ") ").append(component);

        // Send message to console
        getLogger().info(PlainTextComponentSerializer.plainText().serialize(detailedMessage));

        // Send message to players
        List<String> channels = getConfig().publicChat.channels.getOrDefault(channel, Collections.emptyList());
        for (Player target : allPlayers) {
            if (target.getCurrentServer().isEmpty()) {
                continue;
            }
            if (channel.equals(GLOBAL_CHANNEL) || channels.contains(target.getCurrentServer().get().getServerInfo().getName())) {
                target.sendMessage(message);
            } else if (target.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION)) {
                target.sendMessage(detailedMessage);
            }
        }

        // Send message to Discord
        String discordMessage = PlainTextComponentSerializer.plainText().serialize(getConfig().discord.detailed ? detailedMessage : message);
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

    public String getChatFormat(Player player) {
        return getFormat(player, getConfig().publicChat.chatFormat);
    }

    public String getActionFormat(Player player) {
        return getFormat(player, getConfig().publicChat.actionFormat);
    }

    public static Component formatMessage(Player sender, String format, Component message) {
        return MiniMessage.miniMessage().deserialize(format,
                Placeholder.unparsed("name", sender.getUsername()),
                Placeholder.component("message", message)
        );
    }

    public static String getFormat(Player player, LinkedHashMap<String, String> formats) {
        String last = null;
        for (String key : formats.keySet()) {
            last = key;
            if (player.hasPermission("hiddenite.chat." + key) || player.hasPermission("yoctochat." + key)) {
                return formats.get(key);
            }
        }
        return formats.get(last);
    }

    public TextComponent formatURLs(CommandSource source, String message) {
        if (!getConfig().moderation.urls.enabled) {
            return Component.text(message);
        }

        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(message);

        TextComponent component = Component.text().build();

        int lastEnd = 0;
        while (matcher.find()) {
            // Add the text before the detected URL to the component
            component = component.append(Component.text(message.substring(lastEnd, matcher.start())));
            lastEnd = matcher.end();

            // Add the URL if it's possible, else just add the string of the URL
            String uriString = message.substring(matcher.start(), matcher.end());
            try {
                URI uri = new URI(uriString);
                if (!(getConfig().moderation.urls.restricted && !source.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION) && !getConfig().moderation.urls.allowedHosts.contains(uri.getHost()))) {
                    component = component.append(Component.text(uriString).clickEvent(ClickEvent.openUrl(uri.toURL())));
                    continue;
                }
            } catch (URISyntaxException | MalformedURLException ignored) {}

            component = component.append(Component.text(uriString));
        }

        if (message.length() > lastEnd) {
            component = component.append(Component.text(message.substring(lastEnd)));
        }

        return component;
    }

}
