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
import org.slf4j.Logger;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Plugin(id="hiddenite-chat", name="HiddeniteChat", version="1.3.0", authors={"Hiddenite"})
public class ChatPlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final File dataDirectory;

    private Configuration config = new Configuration();

    private final ArrayList<Manager> managers = new ArrayList<>();

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

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
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
        managers.add(new GeneralChatManager(this));
        managers.add(new PrivateMessageManager(this));
        managers.add(new LoginMessageManager(this));
        managers.add(new DiscordManager(this));
        managers.add(new TabListManager(this));
        managers.add(new AutoMessageManager(this));
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
}
