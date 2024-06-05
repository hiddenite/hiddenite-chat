package eu.hiddenite.chat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.managers.PublicChatManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MeCommand implements SimpleCommand {
    private final PublicChatManager manager;

    public MeCommand(PublicChatManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            return;
        }

        String message = String.join(" ", args);

        if (player.getCurrentServer().isEmpty()) {
            return;
        }

        if (manager.getConfig().moderation.mute.enabled && player.hasPermission(ChatPlugin.IS_MUTED_PERMISSION) && !player.hasPermission(ChatPlugin.SUPER_CHAT_PERMISSION)) {
            manager.sendActionMessage(player, message, PublicChatManager.MUTED_CHANNEL);

            if (!manager.getConfig().moderation.mute.errorMutedPublic.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(manager.getConfig().moderation.mute.errorMutedPublic));
            }
            return;
        }

        manager.sendActionMessage(player, message, manager.getChannel(player.getCurrentServer().get().getServerInfo().getName()));
    }

}
