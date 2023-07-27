package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.managers.GeneralChatManager;

public class MeCommand implements SimpleCommand {
    private final GeneralChatManager manager;

    public MeCommand(GeneralChatManager manager) {
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

        manager.sendActionMessage(player, message);
    }

}
