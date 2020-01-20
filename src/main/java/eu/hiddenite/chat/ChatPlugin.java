package eu.hiddenite.chat;

import eu.hiddenite.chat.managers.DiscordManager;
import eu.hiddenite.chat.managers.GeneralChatManager;
import eu.hiddenite.chat.managers.Manager;
import eu.hiddenite.chat.managers.PrivateMessageManager;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;

public class ChatPlugin extends Plugin implements Listener {
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
    }
    private void registerManagers() {
        managers.add(new GeneralChatManager(this));
        managers.add(new PrivateMessageManager(this));
        managers.add(new DiscordManager(this));
    }
}
