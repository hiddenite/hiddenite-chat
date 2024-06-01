package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.PrivateChatManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class PrivateMessageCommand implements SimpleCommand {
    private final PrivateChatManager manager;

    private Configuration getConfig() {
        return manager.getConfig();
    }

    public PrivateMessageCommand(PrivateChatManager manager) {
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().privateChat.usage));
            return;
        }

        if (manager.getProxy().getPlayer(args[0]).isEmpty()) {
            String errorMessage = getConfig().privateChat.errorNotFound.replace("{RECEIVER}", args[0]);
            sender.sendMessage(MiniMessage.miniMessage().deserialize(errorMessage));
            return;
        }

        Player receiver = manager.getProxy().getPlayer(args[0]).get();

        if (!manager.canSendPrivateMessage(sender, receiver)) {
            if (!getConfig().moderation.mute.errorMutedPrivate.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().moderation.mute.errorMutedPrivate));
            }
            return;
        }

        String[] messageWords = Arrays.copyOfRange(args, 1, args.length);
        String message = String.join(" ", messageWords);

        manager.sendPrivateMessage(sender, receiver, message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            List<String> players = new ArrayList<>();
            for (Player player : manager.getProxy().getAllPlayers()) {
                players.add(player.getUsername());
            }
            return players;
        }

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
