package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.Configuration;
import net.md_5.bungee.api.ProxyServer;

import java.util.logging.Logger;

public abstract class Manager  {
    private ChatPlugin plugin;

    public Manager(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    public ChatPlugin getPlugin() {
        return plugin;
    }

    public ProxyServer getProxy() {
        return plugin.getProxy();
    }

    public Configuration getConfig() {
        return plugin.getConfig();
    }

    protected Logger getLogger() {
        return plugin.getLogger();
    }

    public abstract void onEnable();
}
