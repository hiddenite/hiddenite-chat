package eu.hiddenite.chat.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.hiddenite.chat.ChatPlugin;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public class TabListManager extends Manager {

    public TabListManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!getConfig().globalTab.enabled) {
            return;
        }

        getPlugin().registerListener(this);
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        update();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        update();
    }

    private void update() {
        for (Player target : getProxy().getAllPlayers()) {
            TabList tabList = target.getTabList();

            for (Player player : getProxy().getAllPlayers()) {
                Optional<TabListEntry> optionalEntry = tabList.getEntry(player.getUniqueId());
                TabListEntry entry = optionalEntry.orElseGet(() -> TabListEntry.builder().tabList(tabList).profile(player.getGameProfile()).build());

                entry.setListed(true);
                if (target != player) {
                    entry.setGameMode(0);
                }
                entry.setDisplayName(getDisplayName(player, player.hasPermission("hiddenite.afk")));
                entry.setLatency((int) (player.getPing() * 1000));

                tabList.addEntry(entry);
            }

            for (TabListEntry entry : tabList.getEntries()) {
                Optional<Player> optionalPlayer = getProxy().getPlayer(entry.getProfile().getId());
                if (optionalPlayer.isEmpty()) {
                    tabList.removeEntry(entry.getProfile().getId());
                }
            }
        }
    }

    private RegisteredServer getServer(Player player) {
        if (player.getCurrentServer().isPresent()) {
            return player.getCurrentServer().get().getServer();
        }

        return null;
    }

    private Component getDisplayName(Player player, boolean isAfk) {
        String displayNameFormat = getDisplayNameFormat(player);

        String displayName;
        if (displayNameFormat != null) {
            displayName = displayNameFormat.replace("{NAME}", player.getUsername());
        } else {
            displayName = player.getUsername();
        }

        if (isAfk) {
            displayName = getConfig().globalTab.afkFormat.replace("{DISPLAY_NAME}", displayName);
        }

        return Component.text(displayName);
    }

    private String getDisplayNameFormat(Player player) {
        for (String key : getConfig().globalTab.displayNameFormats.keySet()) {
            if (player.hasPermission("hiddenite.chat." + key)) {
                return getConfig().globalTab.displayNameFormats.get(key);
            }
        }
        return null;
    }

    private int getGamemode(Player player) {
        Optional<TabListEntry> tabListEntry = player.getTabList().getEntry(player.getUniqueId());

        if (tabListEntry.isPresent()) {
            return tabListEntry.get().getGameMode();
        }

        return 0;
    }

    /*
    private void updateHeaderAndFooter(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.text(getConfig().globalTab.header), Component.text(getConfig().globalTab.footer));
    }

    private void updateAllHeadersAndFooters() {
        for (Player player : getProxy().getAllPlayers()) {
            updateHeaderAndFooter(player);
        }
    }

    private void updateList(Player player) {
        for (Player onlinePlayer : getProxy().getAllPlayers()) {
            updatePlayerInList(onlinePlayer, player);
        }
    }

    private void updatePlayerInLists(Player player) {
        for (Player onlinePlayer : getProxy().getAllPlayers()) {
            updatePlayerInList(player, onlinePlayer);
        }
    }

    private void updatePlayerInList(Player player, Player target) {
        TabList targetList = target.getTabList();

        Optional<TabListEntry> optionalEntry = targetList.getEntry(player.getUniqueId());
        if (optionalEntry.isEmpty()) {
            targetList.addEntry(TabListEntry.builder().tabList(targetList).profile(player.getGameProfile()).build());
        }

        TabListEntry entry = targetList.getEntry(player.getUniqueId()).get();

        entry.setListed(true);

        if (player != target) {
            if (getServer(player) != getServer(target)) {
                entry.setGameMode(3);
            } else {
                if (getGamemode(player) == 3 && getGamemode(target) == 3) {
                    entry.setGameMode(3);
                } else {
                    entry.setGameMode(0);
                }
            }
        }

        entry.setDisplayName(getDisplayName(player, player.hasPermission("hiddenite.afk")));
    }

    private void removePlayerFromLists(Player player) {
        for (Player onlinePlayer : getProxy().getAllPlayers()) {
            removePlayerFromList(player, onlinePlayer);
        }
    }

    private void removePlayerFromList(Player player, Player target) {
        target.getTabList().removeEntry(player.getUniqueId());
    }
    */

}
