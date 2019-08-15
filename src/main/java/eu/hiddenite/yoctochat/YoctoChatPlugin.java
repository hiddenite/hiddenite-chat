package eu.hiddenite.yoctochat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class YoctoChatPlugin extends Plugin implements Listener {
    private Configuration config = new Configuration();

    @Override
    public void onEnable() {
        if (!config.load(this)) {
            return;
        }

        if (config.chatFormats.size() == 0) {
            getLogger().warning("No chat format found in the configuration.");
            return;
        }

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new PrivateMessageCommand(config));
    }

    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        if (event.getMessage().startsWith("/")) {
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer)event.getSender();
        Collection<ProxiedPlayer> allPlayers = getProxy().getPlayers();

        String chatFormat = getChatFormat(sender);

        String message = event.getMessage();
        String formattedMessage = chatFormat
                .replace("{NAME}", sender.getName())
                .replace("{NAME}", sender.getName())
                .replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);

        BaseComponent[] messageComponents = TextComponent.fromLegacyText(formattedMessage);

        getLogger().info(formattedMessage);
        allPlayers.forEach((receiver) -> {
            receiver.sendMessage(messageComponents);
        });

        event.setCancelled(true);
    }

    private String getChatFormat(ProxiedPlayer player) {
        String last = null;
        for (String key : config.chatFormats.keySet()) {
            last = key;
            if (player.hasPermission("yoctochat." + key)) {
                return config.chatFormats.get(key);
            }
        }
        return config.chatFormats.get(last);
    }
}
