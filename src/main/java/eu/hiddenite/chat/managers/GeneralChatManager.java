package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class GeneralChatManager extends Manager implements Listener {
    public GeneralChatManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (getConfig().chatFormats.size() == 0) {
            getLogger().warning("No chat format found in the configuration.");
            return;
        }

        getProxy().getPluginManager().registerListener(getPlugin(), this);
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

        String discordMessage = TextComponent.toPlainText(messageComponents);
        DiscordManager.getInstance().sendMessage(discordMessage);
    }

    private String getChatFormat(ProxiedPlayer player) {
        String last = null;
        for (String key : getConfig().chatFormats.keySet()) {
            last = key;
            if (player.hasPermission("hiddenite.chat." + key)) {
                return getConfig().chatFormats.get(key);
            }
        }
        return getConfig().chatFormats.get(last);
    }
}
