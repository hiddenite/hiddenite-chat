package eu.hiddenite.chat.commands;

import eu.hiddenite.chat.managers.GeneralChatManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class IncludedMessageCommand  extends Command {
    private final GeneralChatManager manager;

    public IncludedMessageCommand(GeneralChatManager manager) {
        super("includedmessage", "hiddenite.chat.included_message", "imsg");
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            return;
        }

        String message = String.join(" ", args);

        manager.sendIncludedMessage((ProxiedPlayer) commandSender, message, false);
    }

}
