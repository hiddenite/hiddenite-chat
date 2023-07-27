package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.PrivateMessageManager;
import net.kyori.adventure.text.Component;

public class ReplyCommand implements SimpleCommand {
    private final PrivateMessageManager manager;

    private Configuration getConfig() {
        return manager.getConfig();
    }

    public ReplyCommand(PrivateMessageManager manager) {
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
            sender.sendMessage(Component.text(getConfig().privateMessages.replyUsage));
            return;
        }

        Player receiver = manager.getLastPrivateMessageSender(sender);
        if (receiver == null) {
            sender.sendMessage(Component.text(getConfig().privateMessages.errorNoReply));
            return;
        }

        String message = String.join(" ", args);

        manager.sendPrivateMessage(sender, receiver, message);
    }

}
