package eu.hiddenite;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;

public class YoctoChatPlugin extends Plugin implements Listener {
    private String chatMessageFormat;

    @Override
    public void onEnable() {
        loadConfiguration();

        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        if (event.getMessage().startsWith("/")) {
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer)event.getSender();
        Collection<ProxiedPlayer> allPlayers = getProxy().getPlayers();

        String message = event.getMessage();
        String formattedMessage = chatMessageFormat
                .replace("{NAME}", sender.getName())
                .replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);

        BaseComponent[] messageComponents = TextComponent.fromLegacyText(formattedMessage);

        getLogger().info(formattedMessage);
        allPlayers.forEach((receiver) -> {
            receiver.sendMessage(messageComponents);
        });

        event.setCancelled(true);
    }

    private void loadConfiguration() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

            chatMessageFormat = configuration.getString("chat_format");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
