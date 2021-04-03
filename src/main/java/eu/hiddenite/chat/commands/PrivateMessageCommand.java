package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableSet;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.PrivateMessageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrivateMessageCommand extends Command implements TabExecutor {
    private final PrivateMessageManager manager;

    private Configuration getConfig() {
        return manager.getConfig();
    }

    public PrivateMessageCommand(PrivateMessageManager manager) {
        super("msg", null, "w", "m", "tell", "t");
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer)commandSender;
        if (args.length < 2) {
            sender.sendMessage(TextComponent.fromLegacyText(getConfig().pmUsage));
            return;
        }

        ProxiedPlayer receiver = ProxyServer.getInstance().getPlayer(args[0]);
        if (receiver == null) {
            String errorMessage = getConfig().pmErrorNotFound.replace("{RECEIVER}", args[0]);
            sender.sendMessage(TextComponent.fromLegacyText(errorMessage));
            return;
        }

        String[] messageWords = Arrays.copyOfRange(args, 1, args.length);
        String message = String.join(" ", messageWords);

        manager.sendPrivateMessage(sender, receiver, message);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            Set<String> matches = new HashSet<>();
            String search = args[0].toUpperCase();
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.getName().toUpperCase().startsWith(search)) {
                    matches.add(player.getName());
                }
            }
            return matches;
        }

        return ImmutableSet.of();
    }
}
