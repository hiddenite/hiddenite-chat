package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.PrivateChatManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ReplyCommand implements SimpleCommand {
    private final PrivateChatManager manager;

    private Configuration getConfig() {
        return manager.getConfig();
    }

    public ReplyCommand(PrivateChatManager manager) {
        this.manager = manager;
    }
    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player sender)) {
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().privateChat.replyUsage));
            return;
        }

        Player receiver = manager.getLastPrivateMessageSender(sender);
        if (receiver == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().privateChat.errorNoReply));
            return;
        }

        if (!manager.canSendPrivateMessage(sender, receiver)) {
            if (!getConfig().moderation.mute.errorMutedPrivate.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().moderation.mute.errorMutedPrivate));
            }
            return;
        }

        String message = String.join(" ", args);

        manager.sendPrivateMessage(sender, receiver, message);
    }

}
