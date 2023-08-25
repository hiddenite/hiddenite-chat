package eu.hiddenite.chat.managers;

import com.velocitypowered.api.proxy.ProxyServer;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.Configuration;
import org.slf4j.Logger;

public abstract class Manager  {
    private final ChatPlugin plugin;

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
