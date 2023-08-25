package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;

import java.util.Optional;

public class DiscordManager extends Manager {
    private TextChannel discordTextChannel = null;

    public enum Style {
        NORMAL,
        ITALIC
    }

    public DiscordManager(ChatPlugin plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!getConfig().discord.enabled) {
            return;
        }

        getLogger().info("Discord bot enabled, logging in.");
        new DiscordApiBuilder().setToken(getConfig().discord.botToken).login().thenAccept(api -> {
            Optional<Channel> channel = api.getChannelById(getConfig().discord.channelId);
            if (channel.isPresent()) {
                Optional<TextChannel> textChannel = channel.get().asTextChannel();
                if (textChannel.isPresent()) {
                    discordTextChannel = textChannel.get();
                } else {
                    getLogger().warn("The specified Discord channel is not a text channel.");
                }
            } else {
                getLogger().warn("The specified Discord channel could not be found.");
            }
        });
    }

    public void sendMessage(String message, Style style) {
        if (discordTextChannel == null) {
            return;
        }
        String escapedMessage = escapeMarkdown(message);
        String formattedMessage;
        if (style == Style.ITALIC) {
            formattedMessage = "*" + escapedMessage + "*";
        } else {
            formattedMessage = escapedMessage;
        }
        discordTextChannel.sendMessage(formattedMessage);
    }

    private String escapeMarkdown(String rawMessage) {
        return rawMessage
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace("|", "\\|")
                .replace("<", "\\<")
                .replace(">", "\\>")
                .replace("-", "\\-")
                .replace("+", "\\+")
                .replace("#", "\\#")
                .replace(":", "\\:")
                .replace("@", "\\@");
    }

    private static DiscordManager instance;
    public static DiscordManager getInstance() {
        return instance;
    }
}
