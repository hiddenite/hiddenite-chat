package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;

import java.util.Optional;

public class DiscordManager extends Manager {
    private TextChannel discordTextChannel = null;

    public DiscordManager(ChatPlugin plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!getConfig().discordEnabled) {
            return;
        }

        getLogger().info("Discord bot enabled, logging in.");
        new DiscordApiBuilder().setToken(getConfig().discordBotToken).login().thenAccept(api -> {
            Optional<Channel> channel = api.getChannelById(getConfig().discordChannelId);
            if (channel.isPresent()) {
                Optional<TextChannel> textChannel = channel.get().asTextChannel();
                if (textChannel.isPresent()) {
                    discordTextChannel = textChannel.get();
                } else {
                    getLogger().warning("The specified Discord channel is not a text channel.");
                }
            } else {
                getLogger().warning("The specified Discord channel could not be found.");
            }
        });
    }

    public void sendMessage(String message) {
        if (discordTextChannel == null) {
            return;
        }

        discordTextChannel.sendMessage(message.replace("_", "\\_"));
    }

    private static DiscordManager instance;
    public static DiscordManager getInstance() {
        return instance;
    }
}
