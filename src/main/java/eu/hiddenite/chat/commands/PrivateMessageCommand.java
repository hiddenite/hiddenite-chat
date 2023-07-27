package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.PrivateMessageManager;
import net.kyori.adventure.text.Component;

import java.util.*;

public class PrivateMessageCommand implements SimpleCommand {
    private final PrivateMessageManager manager;

    private Configuration getConfig() {
        return manager.getConfig();
    }

    public PrivateMessageCommand(PrivateMessageManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player sender)) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text(getConfig().privateMessages.usage));
            return;
        }

        if (manager.getProxy().getPlayer(args[0]).isEmpty()) {
            String errorMessage = getConfig().privateMessages.errorNotFound.replace("{RECEIVER}", args[0]);
            sender.sendMessage(Component.text(errorMessage));
            return;
        }

        Player receiver = manager.getProxy().getPlayer(args[0]).get();

        String[] messageWords = Arrays.copyOfRange(args, 1, args.length);
        String message = String.join(" ", messageWords);

        manager.sendPrivateMessage(sender, receiver, message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            String search = args[0].toUpperCase();
            for (Player player : manager.getProxy().getAllPlayers()) {
                if (player.getUsername().toUpperCase().startsWith(search)) {
                    matches.add(player.getUsername());
                }
            }
            return matches;
        }

        return ImmutableList.of();
    }

}
