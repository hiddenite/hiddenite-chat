package eu.hiddenite.chat.managers;

import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.PrivateMessageCommand;
import eu.hiddenite.chat.commands.ReplyCommand;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.UUID;

public class PrivateMessageManager extends Manager {
    private HashMap<UUID, UUID> lastPrivateMessages = new HashMap<>();

    public PrivateMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().registerCommand("msg", new PrivateMessageCommand(this), "w", "m", "tell", "t");
        getPlugin().registerCommand("r", new ReplyCommand(this));
    }

    public void sendPrivateMessage(Player sender, Player receiver, String message) {
        lastPrivateMessages.put(receiver.getUniqueId(), sender.getUniqueId());

        String senderMessage = getConfig().privateMessages.sent
                .replace("{NAME}", receiver.getUsername())
                //.replace("{DISPLAY_NAME}", receiver.getDisplayName())
                .replace("{MESSAGE}", message);
        String receiverMessage = getConfig().privateMessages.received
                .replace("{NAME}", sender.getUsername())
                //.replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);

        sender.sendMessage(sender, Component.text(senderMessage));
        receiver.sendMessage(sender, Component.text(receiverMessage));
    }

    public Player getLastPrivateMessageSender(Player player) {
        UUID lastSender = lastPrivateMessages.get(player.getUniqueId());
        if (lastSender != null && getProxy().getPlayer(lastSender).isPresent()) {
            return getProxy().getPlayer(lastSender).get();
        }
        return null;
    }
}
