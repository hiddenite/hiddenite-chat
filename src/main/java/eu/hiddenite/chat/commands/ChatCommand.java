package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.managers.PublicChatManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatCommand implements SimpleCommand {
    private final PublicChatManager manager;

    public ChatCommand(PublicChatManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            return;
        }

        String channel = args[0];
        String message = String.join(" ", Arrays.stream(args).toList().subList(1, args.length));

        if (!manager.getConfig().publicChat.channels.containsKey(channel) && !channel.equalsIgnoreCase(PublicChatManager.GLOBAL_CHANNEL)) {
            source.sendMessage(MiniMessage.miniMessage().deserialize(manager.getConfig().publicChat.errorChannelNotFound));
            return;
        }

        manager.sendMessage(source, message, channel);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        List<String> channels = new ArrayList<>(manager.getConfig().publicChat.channels.keySet());
        channels.add(PublicChatManager.GLOBAL_CHANNEL);

        if (args.length == 0) {
            return channels;
        }

        if (args.length == 1) {
            String search = args[0].toUpperCase();
            List<String> matches = new ArrayList<>();

            for (String channel : manager.getConfig().publicChat.channels.keySet()) {
                if (channel.toUpperCase().startsWith(search)) {
                    matches.add(channel);
                }
            }
            return matches;
        }

        return ImmutableList.of();
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission(ChatPlugin.GLOBAL_CHAT_PERMISSION);
    }

}
