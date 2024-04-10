package eu.hiddenite.chat.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.ServerMessageCommand;
import eu.hiddenite.chat.commands.GlobalMessageCommand;
import eu.hiddenite.chat.commands.MainMessageCommand;
import eu.hiddenite.chat.commands.MeCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class GeneralChatManager extends Manager {
    public GeneralChatManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (getConfig().chatFormat.size() == 0 || getConfig().actionFormat.size() == 0) {
            getLogger().warn("No chat or no action format found in the configuration.");
            return;
        }

        getPlugin().registerListener(this);
        getPlugin().registerCommand("gmsg", new GlobalMessageCommand(this));
        getPlugin().registerCommand("mmsg", new MainMessageCommand(this));
        getPlugin().registerCommand("smsg", new ServerMessageCommand(this));
        getPlugin().registerCommand("me", new MeCommand(this));
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player sender = event.getPlayer();

        if (event.getMessage().startsWith("/")) {
            return;
        }

        event.setResult(PlayerChatEvent.ChatResult.denied());

        String message = event.getMessage();

        boolean isGlobalMessage = false;
        if (message.startsWith("!") && sender.hasPermission("hiddenite.chat.global-chat")) {
            isGlobalMessage = true;
            message = message.substring(1);
        }

        String upperMessage = message.toUpperCase();
        if (getConfig().blockedMessages.stream().map(String::toUpperCase).anyMatch(upperMessage::contains)) {
            return;
        }

        String senderServerName = sender.getCurrentServer().get().getServerInfo().getName();

        if (isGlobalMessage) {
            sendGlobalMessage(sender, message, false);
        } else if (getConfig().excludedServers.contains(senderServerName)) {
            sendExcludedMessage(sender, message, false);
        } else {
            sendMainMessage(sender, message, false);
        }
    }

    public void sendActionMessage(Player sender, String message) {
        String actionFormat = getActionFormat(sender);
        String formattedMessage = formatMessage(sender, actionFormat, message);

        String senderServerName = sender.getCurrentServer().get().getServerInfo().getName();
        if (getConfig().excludedServers.contains(senderServerName)) {
            sendExcludedMessage(sender, formattedMessage, true);
        } else {
            sendMainMessage(sender, formattedMessage, true);
        }
    }

    public void sendGlobalMessage(Player sender, String message, boolean formatted) {
        String formattedMessage;
        if (!formatted) formattedMessage = formatMessage(sender, getChatFormat(sender), message);
        else formattedMessage = message;


        List<String> targetServers = new ArrayList<>();
        for (RegisteredServer registeredServer : getProxy().getAllServers()) {
            targetServers.add(registeredServer.getServerInfo().getName());
        }
        sendToEveryone(sender, formattedMessage, targetServers, "global");
    }

    public void sendConsoleMessage(String message) {
        getPlugin().getProxy().sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void sendMainMessage(Player sender, String message, boolean formatted) {
        String formattedMessage;
        if (!formatted) formattedMessage = formatMessage(sender, getChatFormat(sender), message);
        else formattedMessage = message;

        List<String> excludedServers = getConfig().excludedServers;

        List<String> targetServers = new ArrayList<>();
        for (RegisteredServer registeredServer : getProxy().getAllServers()) {
            if (!excludedServers.contains(registeredServer.getServerInfo().getName())) {
                targetServers.add(registeredServer.getServerInfo().getName());
            }
        }
        sendToEveryone(sender, formattedMessage, targetServers, "main");
    }

    public void sendServerMessage(Player sender, String message, List<String> targetServers, boolean formatted) {
        String formattedMessage;
        if (!formatted) formattedMessage = formatMessage(sender, getChatFormat(sender), message);
        else formattedMessage = message;

        String messageOrigin = String.join(",", targetServers);

        sendToEveryone(sender, formattedMessage, targetServers, messageOrigin);
    }

    public void sendExcludedMessage(Player sender, String message, boolean formatted) {
        List<String> targetServers = new ArrayList<>();
        targetServers.add(sender.getCurrentServer().get().getServerInfo().getName());
        sendServerMessage(sender, message, targetServers, formatted);
    }

    private void sendToEveryone(Player sender, String formattedMessage, List<String> targetServers, String messageOrigin) {
        Collection<Player> allPlayers = getProxy().getAllPlayers();
        TextComponent messageComponent = Component.text(formattedMessage);

        String consoleMessage = "(" + messageOrigin + ") " + formattedMessage;
        TextComponent consoleMessageComponent = Component.text(consoleMessage);

        getLogger().info(consoleMessage);
        for (Player receiver : allPlayers) {
            if (targetServers.contains(receiver.getCurrentServer().get().getServerInfo().getName())) {
                receiver.sendMessage(sender, messageComponent);
            } else if (receiver.hasPermission("hiddenite.chat.global-chat")) {
                receiver.sendMessage(sender, consoleMessageComponent);
            }
        }

        String discordMessage = (getConfig().discord.showServerGroup ? consoleMessageComponent : messageComponent).content().replaceAll("§.", "");
        DiscordManager.getInstance().sendMessage(discordMessage, DiscordManager.Style.NORMAL);
    }

    private String getChatFormat(Player player) {
        return getFormat(player, getConfig().chatFormat);
    }

    private String getActionFormat(Player player) {
        return getFormat(player, getConfig().actionFormat);
    }

    private static String formatMessage(Player sender, String format, String message) {
        return format
                .replace("{NAME}", sender.getUsername())
                //.replace("{DISPLAY_NAME}", sender.getDisplayName())
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
}
