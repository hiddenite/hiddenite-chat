package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableSet;
import eu.hiddenite.chat.Configuration;
import eu.hiddenite.chat.managers.GeneralChatManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class MeCommand extends Command implements TabExecutor {
    private final GeneralChatManager manager;
    private final Configuration config;

    public MeCommand(GeneralChatManager manager) {
        super("me", null);
        this.manager = manager;
        config = manager.getConfig();
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            return;
        }

        String message = String.join(" ", args);

        manager.sendActionMessage((ProxiedPlayer) commandSender, message);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return ImmutableSet.of();
    }
}
