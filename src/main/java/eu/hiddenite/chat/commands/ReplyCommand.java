package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableSet;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.PrivateMessageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class ReplyCommand extends Command implements TabExecutor {
    private PrivateMessageManager manager;
    private Configuration config;

    public ReplyCommand(PrivateMessageManager manager) {
        super("r");
        this.manager = manager;
        config = manager.getConfig();
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer)commandSender;
        if (args.length < 1) {
            sender.sendMessage(TextComponent.fromLegacyText(config.pmReplyUsage));
            return;
        }

        ProxiedPlayer receiver = manager.getLastPrivateMessageSender(sender);
        if (receiver == null) {
            sender.sendMessage(TextComponent.fromLegacyText(config.pmErrorNoReply));
            return;
        }

        String message = String.join(" ", args);

        manager.sendPrivateMessage(sender, receiver, message);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return ImmutableSet.of();
    }
}
