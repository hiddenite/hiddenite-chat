package eu.hiddenite.chat;

import eu.hiddenite.chat.commands.ReloadCommand;
import eu.hiddenite.chat.managers.*;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;

public class ChatPlugin extends Plugin {
    private Configuration config = new Configuration();

    private final ArrayList<Manager> managers = new ArrayList<>();

    public Configuration getConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        if (!reloadConfiguration()) {
            getLogger().warning("Invalid configuration, plugin not enabled.");
            return;
        }

        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));

        registerManagers();
        for (Manager manager : managers) {
            manager.onEnable();
        }

        getLogger().info("Plugin enabled, " + managers.size() + " managers registered.");
    }

    public boolean reloadConfiguration() {
        Configuration config = new Configuration();
        if (!config.load(this)) {
            return false;
        }
        this.config = config;
        return true;
    }

    private void registerManagers() {
        managers.add(new GeneralChatManager(this));
        managers.add(new PrivateMessageManager(this));
        managers.add(new LoginMessageManager(this));
        managers.add(new DiscordManager(this));
        managers.add(new TabListManager(this));
        managers.add(new AutoMessageManager(this));
    }
}
