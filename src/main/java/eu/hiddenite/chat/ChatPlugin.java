package eu.hiddenite.chat;

import eu.hiddenite.chat.managers.*;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;

public class ChatPlugin extends Plugin {
    private Configuration config = new Configuration();

    private ArrayList<Manager> managers = new ArrayList<>();

    public Configuration getConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        if (!config.load(this)) {
            return;
        }

        registerManagers();
        for (Manager manager : managers) {
            manager.onEnable();
        }

        getLogger().info("Plugin enabled, " + managers.size() + " managers registered.");
    }

    private void registerManagers() {
        managers.add(new GeneralChatManager(this));
        managers.add(new PrivateMessageManager(this));
        managers.add(new LoginMessageManager(this));
        managers.add(new DiscordManager(this));
        managers.add(new TabListManager(this));
    }
}
