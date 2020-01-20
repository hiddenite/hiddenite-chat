package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.PrivateMessageCommand;
import eu.hiddenite.chat.commands.ReplyCommand;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.UUID;

public class PrivateMessageManager extends Manager {
    private HashMap<UUID, UUID> lastPrivateMessages = new HashMap<>();

    public PrivateMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(getPlugin(), new PrivateMessageCommand(this));
        getProxy().getPluginManager().registerCommand(getPlugin(), new ReplyCommand(this));
    }

    public void sendPrivateMessage(ProxiedPlayer sender, ProxiedPlayer receiver, String message) {
        lastPrivateMessages.put(receiver.getUniqueId(), sender.getUniqueId());

        String senderMessage = getConfig().pmSentFormat
                .replace("{NAME}", receiver.getName())
                .replace("{DISPLAY_NAME}", receiver.getDisplayName())
                .replace("{MESSAGE}", message);
        String receiverMessage = getConfig().pmReceivedFormat
                .replace("{NAME}", sender.getName())
                .replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);

        sender.sendMessage(TextComponent.fromLegacyText(senderMessage));
        receiver.sendMessage(TextComponent.fromLegacyText(receiverMessage));
    }

    public ProxiedPlayer getLastPrivateMessageSender(ProxiedPlayer player) {
        UUID lastSender = lastPrivateMessages.get(player.getUniqueId());
        if (lastSender != null) {
            return getProxy().getPlayer(lastSender);
        }
        return null;
    }
}
