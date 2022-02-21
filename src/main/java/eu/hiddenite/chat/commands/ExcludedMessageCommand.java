package eu.hiddenite.chat.commands;

import eu.hiddenite.chat.managers.GeneralChatManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class ExcludedMessageCommand extends Command implements TabExecutor {
    private final GeneralChatManager manager;

    public ExcludedMessageCommand(GeneralChatManager manager) {
        super("excludedmessage", "hiddenite.chat.excluded_message", "emsg");
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            return;
        }
        if (args.length < 2) {
            return;
        }

        String targetServer = args[0];

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            builder.append(args[i]).append(" ");
        }
        String message = builder.toString();

        manager.sendExcludedMessage((ProxiedPlayer) commandSender, message, targetServer, false);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (ServerInfo serverInfo : manager.getProxy().getServers().values()) {
                suggestions.add(serverInfo.getName());
            }
        }
        return suggestions;
    }

}
