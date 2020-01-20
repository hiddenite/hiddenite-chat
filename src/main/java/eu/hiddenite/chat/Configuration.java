package eu.hiddenite.chat;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;

public class Configuration {
    public LinkedHashMap<String, String> chatFormats = new LinkedHashMap<>();
    public String pmSentFormat;
    public String pmReceivedFormat;
    public String pmUsage;
    public String pmReplyUsage;
    public String pmErrorNotFound;
    public String pmErrorNoReply;

    public boolean discordEnabled;
    public String discordBotToken;
    public long discordChannelId;

    public boolean load(Plugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            net.md_5.bungee.config.Configuration configuration = ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(new File(plugin.getDataFolder(), "config.yml"));

            net.md_5.bungee.config.Configuration chatSection = configuration.getSection("chat");
            for (String key : chatSection.getKeys()) {
                chatFormats.put(key, chatSection.getString(key));
            }

            pmUsage = configuration.getString("private_messages.usage");
            pmReplyUsage = configuration.getString("private_messages.reply_usage");
            pmSentFormat = configuration.getString("private_messages.sent");
            pmReceivedFormat = configuration.getString("private_messages.received");
            pmErrorNotFound = configuration.getString("private_messages.error_not_found");
            pmErrorNoReply = configuration.getString("private_messages.error_no_reply");

            discordEnabled = configuration.getBoolean("discord.enabled");
            discordBotToken = configuration.getString("discord.bot_token");
            discordChannelId = configuration.getLong("discord.channel_id");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
