package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import eu.hiddenite.chat.ChatPlugin;
import net.kyori.adventure.text.Component;

public class ReloadCommand implements SimpleCommand {
    private final ChatPlugin plugin;

    public ReloadCommand(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (plugin.loadConfiguration()) {
            source.sendMessage(Component.text("Reloaded successfully."));
        } else {
            source.sendMessage(Component.text("Could not reload the configuration."));
        }
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.chat.reload");
    }

}
