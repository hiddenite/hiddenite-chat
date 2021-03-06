package eu.hiddenite.chat;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Configuration {
    public String helloMessage;

    public LinkedHashMap<String, String> chatFormats = new LinkedHashMap<>();
    public LinkedHashMap<String, String> actionFormats = new LinkedHashMap<>();

    public String pmSentFormat;
    public String pmReceivedFormat;
    public String pmUsage;
    public String pmReplyUsage;
    public String pmErrorNotFound;
    public String pmErrorNoReply;

    public String welcomeMessageFormat;
    public String loginMessageFormat;
    public String logoutMessageFormat;
    public int onlinePlayersLimit;

    public boolean autoMessagesEnabled;
    public int autoMessagesInterval;
    public String autoMessagesHeader;
    public List<String> autoMessagesList = new ArrayList<>();

    public boolean globalTabEnabled;
    public LinkedHashMap<String, String> globalTabColors = new LinkedHashMap<>();

    public boolean discordEnabled;
    public String discordBotToken;
    public long discordChannelId;

    public boolean load(Plugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdir()) {
                return false;
            }
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

            net.md_5.bungee.config.Configuration chatSection = configuration.getSection("chat_format");
            for (String key : chatSection.getKeys()) {
                chatFormats.put(key, chatSection.getString(key));
            }

            net.md_5.bungee.config.Configuration actionSection = configuration.getSection("action_format");
            for (String key : actionSection.getKeys()) {
                actionFormats.put(key, actionSection.getString(key));
            }

            pmUsage = configuration.getString("private_messages.usage");
            pmReplyUsage = configuration.getString("private_messages.reply_usage");
            pmSentFormat = configuration.getString("private_messages.sent");
            pmReceivedFormat = configuration.getString("private_messages.received");
            pmErrorNotFound = configuration.getString("private_messages.error_not_found");
            pmErrorNoReply = configuration.getString("private_messages.error_no_reply");

            helloMessage = configuration.getString("hello");

            welcomeMessageFormat = configuration.getString("login.welcome_message");
            loginMessageFormat = configuration.getString("login.login_message");
            logoutMessageFormat = configuration.getString("login.logout_message");
            onlinePlayersLimit = configuration.getInt("login.online_players_limit");

            autoMessagesEnabled = configuration.getBoolean("auto_messages.enabled");
            autoMessagesInterval = configuration.getInt("auto_messages.interval");
            autoMessagesHeader = configuration.getString("auto_messages.header");
            autoMessagesList = configuration.getStringList("auto_messages.messages");

            globalTabEnabled = configuration.getBoolean("global_tab.enabled");
            net.md_5.bungee.config.Configuration globalTabColorsSection = configuration.getSection("global_tab.colors");
            for (String key : globalTabColorsSection.getKeys()) {
                globalTabColors.put(key, globalTabColorsSection.getString(key));
            }

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
