package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.ExcludedMessageCommand;
import eu.hiddenite.chat.commands.GlobalMessageCommand;
import eu.hiddenite.chat.commands.IncludedMessageCommand;
import eu.hiddenite.chat.commands.MeCommand;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class GeneralChatManager extends Manager implements Listener {
    public GeneralChatManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (getConfig().chatFormats.size() == 0 || getConfig().actionFormats.size() == 0) {
            getLogger().warning("No chat or no action format found in the configuration.");
            return;
        }

        getProxy().getPluginManager().registerListener(getPlugin(), this);
        getProxy().getPluginManager().registerCommand(getPlugin(), new GlobalMessageCommand(this));
        getProxy().getPluginManager().registerCommand(getPlugin(), new IncludedMessageCommand(this));
        getProxy().getPluginManager().registerCommand(getPlugin(), new ExcludedMessageCommand(this));
        getProxy().getPluginManager().registerCommand(getPlugin(), new MeCommand(this));

    }

    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer sender)) {
            return;
        }

        if (event.getMessage().startsWith("/")) {
            return;
        }
        event.setCancelled(true);

        String message = event.getMessage();

        boolean isGlobalMessage = false;
        if (message.startsWith("!") && sender.hasPermission("hiddenite.chat.global_message")) {
            isGlobalMessage = true;
            message = message.substring(1);
        }

        String upperMessage = message.toUpperCase();
        if (getConfig().blockedMessages.stream().anyMatch(upperMessage::contains)) {
            return;
        }

        String senderServerName = sender.getServer().getInfo().getName();

        if (isGlobalMessage) {
            sendGlobalMessage(sender, message, false);
        } else if (getConfig().excludedServers.contains(senderServerName)) {
            sendExcludedMessage(sender, message, false);
        } else {
            sendIncludedMessage(sender, message, false);
        }
    }

    public void sendActionMessage(ProxiedPlayer sender, String message) {
        String actionFormat = getActionFormat(sender);
        String formattedMessage = formatMessage(sender, actionFormat, message);

        String senderServerName = sender.getServer().getInfo().getName();
        if (getConfig().excludedServers.contains(senderServerName)) {
            sendExcludedMessage(sender, formattedMessage, true);
        } else {
            sendIncludedMessage(sender, formattedMessage, true);
        }
    }

    public void sendGlobalMessage(ProxiedPlayer sender, String message, boolean formatted) {
        String formattedMessage;
        if (!formatted) formattedMessage = formatMessage(sender, getChatFormat(sender), message);
        else formattedMessage = message;


        List<String> targetServers = new ArrayList<>();
        for (ServerInfo serverInfo : getProxy().getServers().values()) {
            targetServers.add(serverInfo.getName());
        }
        sendToEveryone(sender, formattedMessage, targetServers, "global");
    }

    public void sendIncludedMessage(ProxiedPlayer sender, String message, boolean formatted) {
        String formattedMessage;
        if (!formatted) formattedMessage = formatMessage(sender, getChatFormat(sender), message);
        else formattedMessage = message;

        List<String> excludedServers = getConfig().excludedServers;

        List<String> targetServers = new ArrayList<>();
        for (ServerInfo serverInfo : getProxy().getServers().values()) {
            if (!excludedServers.contains(serverInfo.getName())) {
                targetServers.add(serverInfo.getName());
            }
        }
        sendToEveryone(sender, formattedMessage, targetServers, "included");
    }

    public void sendExcludedMessage(ProxiedPlayer sender, String message, String targetServer, boolean formatted) {
        String formattedMessage;
        if (!formatted) formattedMessage = formatMessage(sender, getChatFormat(sender), message);
        else formattedMessage = message;

        List<String> targetServers = new ArrayList<>();
        targetServers.add(targetServer);

        sendToEveryone(sender, formattedMessage, targetServers, targetServer);
    }

    public void sendExcludedMessage(ProxiedPlayer sender, String message, boolean formatted) {
        String targetServer = sender.getServer().getInfo().getName();
        sendExcludedMessage(sender, message, targetServer, formatted);
    }

    private void sendToEveryone(ProxiedPlayer sender, String formattedMessage, List<String> targetServers, String messageOrigin) {
        Collection<ProxiedPlayer> allPlayers = getProxy().getPlayers();
        BaseComponent[] messageComponents = TextComponent.fromLegacyText(formattedMessage);

        getLogger().info("(" + messageOrigin + ") " + formattedMessage);
        for (ProxiedPlayer receiver : allPlayers) {
            if (targetServers.contains(receiver.getServer().getInfo().getName())) {
                receiver.sendMessage(sender.getUniqueId(), messageComponents);
            } else if (receiver.hasPermission("hiddenite.chat.global_message")) {
                formattedMessage = "(" + messageOrigin + ") " + formattedMessage;
                messageComponents = TextComponent.fromLegacyText(formattedMessage);
                receiver.sendMessage(sender.getUniqueId(), messageComponents);
            }
        }

        String discordMessage = TextComponent.toPlainText(messageComponents);
        DiscordManager.getInstance().sendMessage(discordMessage, DiscordManager.Style.NORMAL);
    }

    private String getChatFormat(ProxiedPlayer player) {
        return getFormat(player, getConfig().chatFormats);

    }

    private String getActionFormat(ProxiedPlayer player) {
        return getFormat(player, getConfig().actionFormats);
    }

    private static String formatMessage(ProxiedPlayer sender, String format, String message) {
        return format
                .replace("{NAME}", sender.getName())
                .replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);
    }

    private static String getFormat(ProxiedPlayer player, LinkedHashMap<String, String> formats) {
        String last = null;
        for (String key : formats.keySet()) {
            last = key;
            if (player.hasPermission("hiddenite.chat." + key) || player.hasPermission("yoctochat." + key)) {
                return formats.get(key);
            }
        }
        return formats.get(last);
    }
}
