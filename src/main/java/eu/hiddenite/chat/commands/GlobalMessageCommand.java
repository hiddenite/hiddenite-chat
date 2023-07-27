package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.managers.GeneralChatManager;

public class GlobalMessageCommand implements SimpleCommand {
    private final GeneralChatManager manager;

    public GlobalMessageCommand(GeneralChatManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            return;
        }

        String message = String.join(" ", args);

        manager.sendGlobalMessage(player, message, false);
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.chat.global-chat");
    }

}
