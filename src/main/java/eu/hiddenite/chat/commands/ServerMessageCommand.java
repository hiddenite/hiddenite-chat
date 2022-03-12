package eu.hiddenite.chat.commands;

import eu.hiddenite.chat.managers.GeneralChatManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerMessageCommand extends Command implements TabExecutor {
    private final GeneralChatManager manager;

    public ServerMessageCommand(GeneralChatManager manager) {
        super("servermessage", "hiddenite.chat.global_chat", "smsg");
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

        List<String> targetServers = new ArrayList<>();

        List<String> proxyServers = new ArrayList<>();
        for (ServerInfo serverInfo : manager.getProxy().getServers().values()) {
            proxyServers.add(serverInfo.getName());
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

        manager.sendServerMessage((ProxiedPlayer) commandSender, message, targetServers, false);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            List<String> chosenServers = Arrays.stream(args[0].split(",")).toList();
            chosenServers.subList(0, chosenServers.size()-1);
            String suggestion = String.join(",", chosenServers);

            for (ServerInfo serverInfo : manager.getProxy().getServers().values()) {
                if (!suggestion.equals("")) {
                    suggestions.add(suggestion + "," + serverInfo.getName());
                } else {
                    suggestions.add(serverInfo.getName());
                }
            }
        }
        return suggestions;
    }

}
