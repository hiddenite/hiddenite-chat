package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.PlayerListHeaderFooter;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.tab.TabList;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class TabListManager extends Manager implements Listener {
    private record TabListPlayer(UUID uuid, int gameMode, boolean isAfk, int latency) {}

    private static class CustomTabList extends TabList {
        private final TabListManager manager;
        private final HashMap<UUID, TabListPlayer> players = new HashMap<>();
        private final HashMap<UUID, Integer> gameModes = new HashMap<>();

        public CustomTabList(TabListManager manager, ProxiedPlayer player) {
            super(player);
            this.manager = manager;
        }

        @Override
        public void onConnect() {}

        @Override
        public void onDisconnect() {}

        @Override
        public void onUpdate(PlayerListItem playerListItem) {
            this.handleGameModeChanges(playerListItem);
            this.sendUpdate(false);
            if (playerListItem.getAction() == PlayerListItem.Action.UPDATE_LATENCY) {
                player.unsafe().sendPacket(playerListItem);
            }
        }

        private void handleGameModeChanges(PlayerListItem playerListItem) {
            if (playerListItem.getAction() == PlayerListItem.Action.ADD_PLAYER ||
                    playerListItem.getAction() == PlayerListItem.Action.UPDATE_GAMEMODE) {
                for (PlayerListItem.Item item : playerListItem.getItems()) {
                    gameModes.put(item.getUuid(), item.getGamemode());
                }
            }
            if (playerListItem.getAction() == PlayerListItem.Action.REMOVE_PLAYER) {
                for (PlayerListItem.Item item : playerListItem.getItems()) {
                    gameModes.remove(item.getUuid());
                }
            }
        }

        @Override
        public void onPingChange(int latency) {
            this.sendUpdate(false);
        }

        @Override
        public void onServerChange() {
            this.sendUpdate(true);
        }

        private void sendUpdate(boolean force) {
            this.sendHeaderAndFooter();
            if (force) {
                this.clearEverything();
            }
            this.updateEverything();
        }

        private void clearEverything() {
            PlayerListItem packet = new PlayerListItem();
            packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
            packet.setItems(players.values().stream().map(x -> {
                PlayerListItem.Item item = new PlayerListItem.Item();
                item.setUuid(x.uuid);
                return item;
            }).toArray(PlayerListItem.Item[]::new));
            player.unsafe().sendPacket(packet);
            players.clear();
        }

        private void updateEverything() {
            Collection<ProxiedPlayer> allPlayers = ProxyServer.getInstance().getPlayers();
            Set<UUID> onlineIds = allPlayers.stream().map(ProxiedPlayer::getUniqueId).collect(Collectors.toSet());
            int myGamemode = gameModes.getOrDefault(this.player.getUniqueId(), 0);

            // Compute removed players
            ArrayList<PlayerListItem.Item> removedPlayers = new ArrayList<>();

            for (TabListPlayer tabPlayer : players.values()) {
                if (onlineIds.contains(tabPlayer.uuid)) {
                    continue;
                }
                PlayerListItem.Item item = new PlayerListItem.Item();
                item.setUuid(tabPlayer.uuid);
                removedPlayers.add(item);
            }

            if (removedPlayers.size() > 0) {
                PlayerListItem packet = new PlayerListItem();
                packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
                packet.setItems(removedPlayers.toArray(PlayerListItem.Item[]::new));
                player.unsafe().sendPacket(packet);
            }

            // Compute added or changed players
            ArrayList<PlayerListItem.Item> addedPlayers = new ArrayList<>();

            for (ProxiedPlayer onlinePlayer : allPlayers) {
                String serverA = player.getServer() != null ? player.getServer().getInfo().getName() : "";
                String serverB = onlinePlayer.getServer() != null ? onlinePlayer.getServer().getInfo().getName() : "";

                UUID uuid = onlinePlayer.getUniqueId();

                int gameMode;
                if (serverA.equals(serverB)) {
                    gameMode = gameModes.getOrDefault(onlinePlayer.getUniqueId(), 0);
                    if (onlinePlayer != this.player && myGamemode != 3 && gameMode == 3) {
                        // Hide spectators from non-spectators.
                        gameMode = 0;
                    }
                } else {
                    // Players on other servers are seen as spectator.
                    gameMode = 3;
                }

                boolean isAfk = onlinePlayer.hasPermission("hiddenite.afk");
                int ping = onlinePlayer.getPing();

                TabListPlayer tabPlayer = players.get(onlinePlayer.getUniqueId());
                if (tabPlayer != null && tabPlayer.isAfk == isAfk && tabPlayer.gameMode == gameMode) {
                    continue;
                }

                PlayerListItem.Item item = new PlayerListItem.Item();
                item.setUuid(onlinePlayer.getUniqueId());
                item.setUsername(onlinePlayer.getName());
                item.setGamemode(gameMode);
                item.setPing(onlinePlayer.getPing());
                item.setProperties(new String[][] {});
                item.setDisplayName(getDisplayName(onlinePlayer, isAfk));
                addedPlayers.add(item);

                players.put(onlinePlayer.getUniqueId(), new TabListPlayer(uuid, gameMode, isAfk, ping));
            }

            if (addedPlayers.size() > 0) {
                PlayerListItem packet = new PlayerListItem();
                packet.setAction(PlayerListItem.Action.ADD_PLAYER);
                packet.setItems(addedPlayers.toArray(PlayerListItem.Item[]::new));
                player.unsafe().sendPacket(packet);
            }
        }

        private void sendHeaderAndFooter() {
            PlayerListHeaderFooter packet = new PlayerListHeaderFooter();

            String configHeader = manager.getConfig().globalTabHeader;
            String configFooter = manager.getConfig().globalTabFooter;

            if (configHeader != null) {
                packet.setHeader(ComponentSerializer.toString(TextComponent.fromLegacyText(configHeader)));
            }
            if (configFooter != null) {
                ProxyServer server = ProxyServer.getInstance();
                configFooter = configFooter.replace("{PLAYERS}", String.valueOf(server.getOnlineCount()));
                configFooter = configFooter.replace("{LIMIT}", String.valueOf(server.getConfig().getPlayerLimit()));
                packet.setFooter(ComponentSerializer.toString(TextComponent.fromLegacyText(configFooter)));
            }

            player.unsafe().sendPacket(packet);
        }

        private String getDisplayName(ProxiedPlayer player, boolean isAfk) {
            String customColor = getTabColor(player);
            String displayName = player.getDisplayName();
            if (isAfk) {
                displayName = manager.getConfig().afkFormat.replace("{NAME}", displayName);
            }

            if (customColor != null) {
                return "{\"extra\":[{\"color\":\"" +
                        customColor +
                        "\",\"text\":\"" +
                        displayName +
                        "\"}],\"text\":\"\"}";
            }
            return "{\"text\":\"" + displayName + "\"}";
        }

        private String getTabColor(ProxiedPlayer player) {
            for (String key : manager.getConfig().globalTabColors.keySet()) {
                if (player.hasPermission("hiddenite.chat." + key)) {
                    return manager.getConfig().globalTabColors.get(key);
                }
            }
            return null;
        }
    }

    public TabListManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!getConfig().globalTabEnabled) {
            return;
        }

        getProxy().getPluginManager().registerListener(getPlugin(), this);
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        TabList customTabList = new CustomTabList(this, player);
        try {
            Field f = UserConnection.class.getDeclaredField("tabListHandler");
            f.setAccessible(true);
            f.set(player, customTabList);
            customTabList.onConnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
