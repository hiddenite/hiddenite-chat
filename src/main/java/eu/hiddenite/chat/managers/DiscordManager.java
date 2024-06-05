package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.Configuration;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;

import java.util.*;
import java.util.List;

public class DiscordManager extends Manager {
    private final Map<String, TextChannel> textChannels = new HashMap<>();
    private final Map<String, List<String>> chatChannelTargets = new HashMap<>();

    public enum Style {
        NORMAL,
        ITALIC
    }

    public DiscordManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onLoad() {
        textChannels.clear();
        chatChannelTargets.clear();

        if (!getConfig().discord.enabled) {
            return;
        }

        getLogger().info("Discord bot enabled, logging in.");
        new DiscordApiBuilder().setToken(getConfig().discord.botToken).login().thenAccept(api -> {
            for (Configuration.Discord.Channel channel : getConfig().discord.channels) {
                for (String chatChannel : channel.chatChannels) {
                    List<String> targets = chatChannelTargets.getOrDefault(chatChannel, new ArrayList<>());
                    targets.add(channel.id);
                    chatChannelTargets.put(chatChannel, targets);
                }

                api.getChannelById(channel.id).ifPresentOrElse(discordChannel -> {
                    discordChannel.asTextChannel().ifPresentOrElse(discordTextChannel -> {
                        textChannels.put(channel.id, discordTextChannel);
                    }, () -> {
                        getLogger().warn("Discord channel " + channel.id + " is not a text channel.");
                    });
                }, () -> {
                    getLogger().warn("Discord channel " + channel.id + " could not be found.");
                });
            }
        });
    }

    public void sendMessage(String message, Style style, String chatChannel) {
        if (!getConfig().discord.enabled) {
            return;
        }

        String escapedMessage = escapeMarkdown(message);
        String formattedMessage;
        if (style == Style.ITALIC) {
            formattedMessage = "_" + escapedMessage + "_";
        } else {
            formattedMessage = escapedMessage;
        }

        if (chatChannelTargets.get(chatChannel) != null) {
            for (String channelId : chatChannelTargets.get(chatChannel)) {
                TextChannel textChannel = textChannels.get(channelId);
                textChannel.sendMessage(formattedMessage);
            }
        }
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

}
