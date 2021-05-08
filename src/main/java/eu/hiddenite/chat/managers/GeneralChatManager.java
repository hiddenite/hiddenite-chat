package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.MeCommand;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.UUID;

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
        getProxy().getPluginManager().registerCommand(getPlugin(), new MeCommand(this));
    }

    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        if (event.getMessage().startsWith("/")) {
            return;
        }
        event.setCancelled(true);

        ProxiedPlayer sender = (ProxiedPlayer)event.getSender();

        String chatFormat = getChatFormat(sender);
        String message = event.getMessage();
        String formattedMessage = formatMessage(sender, chatFormat, message);

        sendToEveryone(sender.getUniqueId(), formattedMessage);
    }

    public void sendActionMessage(ProxiedPlayer sender, String message) {
        String actionFormat = getActionFormat(sender);
        String formattedMessage = formatMessage(sender, actionFormat, message);

        sendToEveryone(sender.getUniqueId(), formattedMessage);
    }

    private void sendToEveryone(UUID sender, String formattedMessage) {
        Collection<ProxiedPlayer> allPlayers = getProxy().getPlayers();
        BaseComponent[] messageComponents = TextComponent.fromLegacyText(formattedMessage);

        getLogger().info(formattedMessage);
        allPlayers.forEach((receiver) ->
                receiver.sendMessage(sender, messageComponents)
        );

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
