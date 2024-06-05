package eu.hiddenite.chat.managers;

import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.commands.PrivateMessageCommand;
import eu.hiddenite.chat.commands.ReplyCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

        TextComponent component = getPlugin().getPublicChatManager().formatURLs(sender, message);

        Component senderMessage = MiniMessage.miniMessage().deserialize(getConfig().privateChat.sent,
                Placeholder.unparsed("sender", sender.getUsername()),
                Placeholder.unparsed("receiver", receiver.getUsername()),
                Placeholder.component("message", component)
        );
        Component receiverMessage = MiniMessage.miniMessage().deserialize(getConfig().privateChat.received,
                Placeholder.unparsed("sender", sender.getUsername()),
                Placeholder.unparsed("receiver", receiver.getUsername()),
                Placeholder.component("message", component)
        );
        Component spyMessage = MiniMessage.miniMessage().deserialize(getConfig().privateChat.spy,
                Placeholder.unparsed("sender", sender.getUsername()),
                Placeholder.unparsed("receiver", receiver.getUsername()),
                Placeholder.component("message", component)
        );

        sender.sendMessage(senderMessage);
        receiver.sendMessage(receiverMessage);

        getPlugin().getDiscordManager().sendMessage(PlainTextComponentSerializer.plainText().serialize(spyMessage), DiscordManager.Style.NORMAL, PRIVATE_CHANNEL);
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
            boolean bypass = sender.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION) || receiver.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION);
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
