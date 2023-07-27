package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.hiddenite.chat.managers.GeneralChatManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerMessageCommand implements SimpleCommand {
    private final GeneralChatManager manager;

    public ServerMessageCommand(GeneralChatManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            return;
        }
        if (args.length < 2) {
            return;
        }

        List<String> targetServers = new ArrayList<>();

        List<String> proxyServers = new ArrayList<>();
        for (RegisteredServer registeredServer : manager.getProxy().getAllServers()) {
            proxyServers.add(registeredServer.getServerInfo().getName());
        }

        for (String targetServer : args[0].split(",")) {
            if (!proxyServers.contains(targetServer)) {
                return;
            }
            targetServers.add(targetServer);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            builder.append(args[i]).append(" ");
        }
        String message = builder.toString();

        manager.sendServerMessage(player, message, targetServers, false);
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.chat.global-chat");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            List<String> chosenServers = Arrays.stream(args[0].split(",")).toList();
            chosenServers.subList(0, chosenServers.size()-1);
            String suggestion = String.join(",", chosenServers);

            for (RegisteredServer registeredServer : manager.getProxy().getAllServers()) {
                if (!suggestion.equals("")) {
                    suggestions.add(suggestion + "," + registeredServer.getServerInfo().getName());
                } else {
                    suggestions.add(registeredServer.getServerInfo().getName());
                }
            }
        }
        return suggestions;
    }

}
