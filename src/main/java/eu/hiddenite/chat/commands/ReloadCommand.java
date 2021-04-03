package eu.hiddenite.chat.commands;

import com.google.common.collect.ImmutableSet;
import eu.hiddenite.chat.ChatPlugin;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class ReloadCommand extends Command implements TabExecutor {
    private final ChatPlugin plugin;

    public ReloadCommand(ChatPlugin plugin) {
        super("hiddenite:chat:reload", "hiddenite.chat.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (plugin.reloadConfiguration()) {
            commandSender.sendMessage(new TextComponent("Reloaded successfully."));
        } else {
            commandSender.sendMessage(new TextComponent("Could not reload the configuration."));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return ImmutableSet.of();
    }
}
