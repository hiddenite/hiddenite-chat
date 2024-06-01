package eu.hiddenite.chat;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.List;

@ConfigSerializable
public class Configuration {
    public String hello;
    public Login login;
    public PublicChat publicChat;
    public PrivateChat privateChat;
    public Moderation moderation;
    public AutoMessages autoMessages;
    public GlobalTab globalTab;
    public Discord discord;

    @ConfigSerializable
    public static class Login {
        public String welcomeMessage;
        public String loginMessage;
        public String logoutMessage;
        public int onlinePlayersLimit;
    }

    @ConfigSerializable
    public static class PublicChat {
        public LinkedHashMap<String, String> chatFormat;
        public LinkedHashMap<String, String> actionFormat;
        public LinkedHashMap<String, List<String>> channels;
        public String errorChannelNotFound;
    }

    @ConfigSerializable
    public static class PrivateChat {
        public String usage;
        public String replyUsage;
        public String sent;
        public String received;
        public String spy;
        public String errorNotFound;
        public String errorNoReply;
    }

    @ConfigSerializable
    public static class Moderation {
        public List<String> blockedMessages;
        public URLs urls;
        public Mute mute;

        @ConfigSerializable
        public static class URLs {
            public boolean enabled;
            public boolean restricted;
            public List<String> allowedHosts;
        }

        @ConfigSerializable
        public static class Mute {
            public boolean enabled;
            public boolean receivePrivateMessages;
            public String errorMutedPublic;
            public String errorMutedPrivate;
        }
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
        public List<Channel> channels;
        public boolean detailed;

        @ConfigSerializable
        public static class Channel {
            public String id;
            public List<String> chatChannels;
        }
    }
}
