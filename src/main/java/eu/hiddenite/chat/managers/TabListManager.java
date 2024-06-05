package eu.hiddenite.chat.managers;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import eu.hiddenite.chat.ChatPlugin;
import eu.hiddenite.chat.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TabListManager extends Manager {
    public static class TabPlayer {
        private final TabListManager manager;
        private final ProxyServer server;
        private final Configuration config;
        private final Player player;

        private int lastPlayerCount = 0;
        private final HashMap<UUID, Integer> originalGamemodes = new HashMap<>();

        public TabPlayer(TabListManager manager, Player player) {
            this.manager = manager;
            this.server = manager.getProxy();
            this.config = manager.getConfig();
            this.player = player;
        }

        public void reset() {
            this.lastPlayerCount = 0;
        }

        public void sendHeaderAndFooter() {
            int players = server.getPlayerCount();

            if (lastPlayerCount == players) {
                // Header unchanged, nothing to do.
                return;
            }
            lastPlayerCount = players;

            int limit = server.getConfiguration().getShowMaxPlayers();

            player.sendPlayerListHeaderAndFooter(
                    MiniMessage.miniMessage().deserialize(config.globalTab.header,
                            Placeholder.unparsed("players", String.valueOf(players)),
                            Placeholder.unparsed("limit", String.valueOf(limit))
                    ),
                    MiniMessage.miniMessage().deserialize(config.globalTab.footer,
                            Placeholder.unparsed("players", String.valueOf(players)),
                            Placeholder.unparsed("limit", String.valueOf(limit))
                    )
            );
        }

        public void updateEntries() {
            int myGamemode = getMyGamemode();

            for (TabListEntry entry : player.getTabList().getEntries()) {
                Player otherPlayer = server.getPlayer(entry.getProfile().getId()).orElse(null);
                if (otherPlayer == null) {
                    player.getTabList().removeEntry(entry.getProfile().getId());
                    continue;
                }

                String format = getTabFormatForPlayer(otherPlayer);
                Component displayName = MiniMessage.miniMessage().deserialize(format == null ? entry.getProfile().getName() : format,
                        Placeholder.unparsed("name", entry.getProfile().getName())
                );
                if (manager.isAfk(otherPlayer.getUniqueId())) {
                    displayName = MiniMessage.miniMessage().deserialize(config.globalTab.afkFormat,
                            Placeholder.component("display-name", displayName)
                    );
                }
                entry.setDisplayName(displayName);

                if (entry.getGameMode() == 3 && myGamemode != 3 && entry.getChatSession() != null) {
                    originalGamemodes.put(entry.getProfile().getId(), entry.getGameMode());
                    entry.setGameMode(0);
                } else if (myGamemode == 3 && originalGamemodes.getOrDefault(entry.getProfile().getId(), 0) == 3) {
                    originalGamemodes.remove(entry.getProfile().getId());
                    entry.setGameMode(3);
                }
            }

            this.createFakeEntriesForOtherServers();
        }

        private void createFakeEntriesForOtherServers() {
            for (Player otherPlayer : server.getAllPlayers()) {
                if (otherPlayer.getCurrentServer().equals(player.getCurrentServer())) {
                    continue;
                }
                if (player.getTabList().containsEntry(otherPlayer.getUniqueId())) {
                    continue;
                }

                GameProfile gameProfile = new GameProfile(otherPlayer.getUniqueId(), otherPlayer.getUsername(), new ArrayList<>());
                player.getTabList().addEntry(TabListEntry.builder()
                        .tabList(player.getTabList())
                        .profile(gameProfile)
                        .gameMode(3)
                        .build()
                );
            }
        }

        private int getMyGamemode() {
            TabListEntry myEntry = player.getTabList().getEntry(player.getUniqueId()).orElse(null);
            if (myEntry != null) {
                return myEntry.getGameMode();
            }
            return 0;
        }

        private String getTabFormatForPlayer(Player player) {
            for (var entry : config.globalTab.displayNameFormats.entrySet()) {
                if (player.hasPermission("hiddenite.chat." + entry.getKey())) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    private final HashMap<UUID, TabPlayer> playerTabs = new HashMap<>();
    private final HashMap<UUID, Boolean> afkPlayers = new HashMap<>();

    public TabListManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!getConfig().globalTab.enabled) {
            return;
        }

        getPlugin().registerListener(this);
        getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from("hiddenite:afk"));

        getProxy().getScheduler()
                .buildTask(getPlugin(), this::updateAllPlayers)
                .repeat(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Override
    public void onLoad() {

    }

    @Subscribe(order = PostOrder.LATE)
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        playerTabs.put(player.getUniqueId(), new TabPlayer(this, player));
        afkPlayers.remove(player.getUniqueId());
        updatePlayer(player);
    }

    @Subscribe(order = PostOrder.LATE)
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        TabPlayer tabPlayer = playerTabs.get(player.getUniqueId());
        if (tabPlayer != null) {
            tabPlayer.reset();
        }
        updatePlayer(player);
    }

    @Subscribe(order = PostOrder.LATE)
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        playerTabs.remove(player.getUniqueId());
        afkPlayers.remove(player.getUniqueId());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals("hiddenite:afk")) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getTarget() instanceof Player player)) {
            return;
        }

        boolean isAfk = event.getData()[0] != 0;
        afkPlayers.put(player.getUniqueId(), isAfk);
    }

    public boolean isAfk(UUID uniqueId) {
        return afkPlayers.getOrDefault(uniqueId, false);
    }

    private void updateAllPlayers()  {
        for (Player player : getProxy().getAllPlayers()) {
            updatePlayer(player);
        }
    }

    private void updatePlayer(Player player)  {
        TabPlayer tabPlayer = playerTabs.get(player.getUniqueId());
        if (tabPlayer == null) {
            getLogger().warn("Tried to update the tablist of the non existing player " + player.getUsername());
            tabPlayer = new TabPlayer(this, player);
            playerTabs.put(player.getUniqueId(), tabPlayer);
        }

        tabPlayer.sendHeaderAndFooter();
        tabPlayer.updateEntries();
    }
}
