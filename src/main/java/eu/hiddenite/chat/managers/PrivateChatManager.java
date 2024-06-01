package eu.hiddenite.chat.managers;

import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.PrivateMessageCommand;
import eu.hiddenite.chat.commands.ReplyCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.UUID;

public class PrivateChatManager extends Manager {
    public static final String PRIVATE_CHANNEL = "@private";

    private final HashMap<UUID, UUID> lastPrivateMessages = new HashMap<>();

    public PrivateChatManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().registerCommand("msg", new PrivateMessageCommand(this), "w", "m", "tell", "t");
        getPlugin().registerCommand("r", new ReplyCommand(this));
    }

    @Override
    public void onLoad() {

    }

    public void sendPrivateMessage(Player sender, Player receiver, String message) {
        lastPrivateMessages.put(receiver.getUniqueId(), sender.getUniqueId());

        message = getPlugin().getPublicChatManager().formatURLs(sender, message);

        String senderMessage = getConfig().privateChat.sent.replace("{SENDER}", sender.getUsername()).replace("{RECEIVER}", receiver.getUsername()).replace("{MESSAGE}", message);
        String receiverMessage = getConfig().privateChat.received.replace("{SENDER}", sender.getUsername()).replace("{RECEIVER}", receiver.getUsername()).replace("{MESSAGE}", message);

        sender.sendMessage(MiniMessage.miniMessage().deserialize(senderMessage));
        receiver.sendMessage(MiniMessage.miniMessage().deserialize(receiverMessage));

        String spyMessage = getConfig().privateChat.spy.replace("{SENDER}", sender.getUsername()).replace("{RECEIVER}", receiver.getUsername()).replace("{MESSAGE}", message);
        getPlugin().getDiscordManager().sendMessage(PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(spyMessage)), DiscordManager.Style.NORMAL, PRIVATE_CHANNEL);
    }

    public Player getLastPrivateMessageSender(Player player) {
        UUID lastSender = lastPrivateMessages.get(player.getUniqueId());
        if (lastSender != null && getProxy().getPlayer(lastSender).isPresent()) {
            return getProxy().getPlayer(lastSender).get();
        }

        return null;
    }

    public boolean canSendPrivateMessage(Player sender, Player receiver) {
        if (getConfig().moderation.mute.enabled) {
            boolean bypass = sender.hasPermission(ChatPlugin.BYPASS_PERMISSION) || receiver.hasPermission(ChatPlugin.BYPASS_PERMISSION);
            if (!bypass) {
                if (sender.hasPermission(ChatPlugin.IS_MUTED_PERMISSION)) {
                    return false;
                }

                return !receiver.hasPermission(ChatPlugin.IS_MUTED_PERMISSION) || getConfig().moderation.mute.receivePrivateMessages;
            }
        }

        return true;
    }

}
