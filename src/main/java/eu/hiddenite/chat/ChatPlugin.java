package eu.hiddenite.chat;

import com.google.inject.Inject;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.hiddenite.chat.commands.*;
import eu.hiddenite.chat.managers.*;
import eu.hiddenite.chat.managers.PrivateChatManager;
import eu.hiddenite.chat.managers.PublicChatManager;
import org.slf4j.Logger;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Plugin(id="hiddenite-chat", name="HiddeniteChat", version="2.0.0", authors={"Hiddenite"})
public class ChatPlugin {
    public static final String RELOAD_PERMISSION = "hiddenite.chat.reload";
    public static final String GLOBAL_CHAT_PERMISSION = "hiddenite.chat.global-chat";
    public static final String BYPASS_PERMISSION = "hiddenite.chat.bypass";
    public static final String IS_MUTED_PERMISSION = "hiddenite.chat.is-muted";

    private final ProxyServer proxy;
    private final Logger logger;
    private final File dataDirectory;

    private Configuration config = new Configuration();

    private final ArrayList<Manager> managers = new ArrayList<>();
    private PublicChatManager publicChatManager;
    private DiscordManager discordManager;

    @Inject
    public ChatPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (!loadConfiguration()) {
            logger.warn("Invalid configuration, plugin not enabled.");
            return;
        }

        registerCommand("hiddenite:chat:reload", new ReloadCommand(this));

        registerManagers();
        for (Manager manager : managers) {
            manager.onEnable();
            manager.onLoad();
        }

        logger.info("Plugin enabled, " + managers.size() + " managers registered.");
    }

    public boolean loadConfiguration() {
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdir()) {
                logger.warn("Could not create the configuration folder.");
                return false;
            }
        }

        File file = new File(dataDirectory, "config.yml");
        if (!file.exists()) {
            logger.warn("No configuration file found, creating a default one.");

            try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(input, file.toPath());
            } catch (IOException exception) {
                exception.printStackTrace();
                return false;
            }
        }

        YamlConfigurationLoader reader = YamlConfigurationLoader.builder().path(dataDirectory.toPath().resolve("config.yml")).build();

        try {
            config = reader.load().get(Configuration.class);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean reload() {
        if (loadConfiguration()) {
            for (Manager manager : managers) {
                manager.onLoad();
            }
            return true;
        } else {
            return false;
        }
    }

    public void registerCommand(String name, Command command, String... aliases) {
        CommandManager manager = proxy.getCommandManager();

        CommandMeta meta = manager.metaBuilder(name).plugin(this).aliases(aliases).build();
        manager.register(meta, command);
    }

    public void registerListener(Object listener) {
        EventManager eventManager = proxy.getEventManager();

        eventManager.register(this, listener);
    }

    private void registerManagers() {
        publicChatManager = new PublicChatManager(this);
        managers.add(publicChatManager);
        managers.add(new PrivateChatManager(this));
        managers.add(new LoginMessageManager(this));
        managers.add(new AutoMessageManager(this));
        managers.add(new TabListManager(this));
        discordManager = new DiscordManager(this);
        managers.add(discordManager);
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    public Configuration getConfig() {
        return config;
    }

    public PublicChatManager getPublicChatManager() {
        return publicChatManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

}
