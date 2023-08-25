package eu.hiddenite.chat;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.List;

@ConfigSerializable
public class Configuration {
    public String hello;
    public LinkedHashMap<String, String> chatFormat;
    public LinkedHashMap<String, String> actionFormat;
    public List<String> blockedMessages;
    public Login login;
    public PrivateMessages privateMessages;
    public AutoMessages autoMessages;
    public GlobalTab globalTab;
    public List<String> excludedServers;
    public Discord discord;

    @ConfigSerializable
    public static class Login {
        public String welcomeMessage;
        public String loginMessage;
        public String logoutMessage;
        public int onlinePlayersLimit;
    }

    @ConfigSerializable
    public static class PrivateMessages {
        public String usage;
        public String replyUsage;
        public String sent;
        public String received;
        public String errorNotFound;
        public String errorNoReply;
    }

    @ConfigSerializable
    public static class AutoMessages {
        public boolean enabled;
        public int interval;
        public String header;
        public List<String> messages;
    }

    @ConfigSerializable
    public static class GlobalTab {
        public boolean enabled;
        public String header;
        public String footer;
        public String afkFormat;
        public LinkedHashMap<String, String> displayNameFormats;
    }

    @ConfigSerializable
    public static class Discord {
        public boolean enabled;
        public String botToken;
        public String channelId;
        public boolean showServerGroup;
    }
}
