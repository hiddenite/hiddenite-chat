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
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class TabListManager extends Manager implements Listener {
    private static class CustomTabList extends TabList {
        private final TabListManager manager;
        private final Collection<UUID> realPlayers = new HashSet<>();
        private final Collection<UUID> fakePlayers = new HashSet<>();

        public CustomTabList(TabListManager manager, ProxiedPlayer player) {
            super(player);
            this.manager = manager;
        }

        @Override
        public void onUpdate(PlayerListItem playerListItem) {
            for (PlayerListItem.Item item : playerListItem.getItems()) {
                if (playerListItem.getAction() == PlayerListItem.Action.ADD_PLAYER) {
                    realPlayers.add(item.getUuid());
                    fakePlayers.remove(item.getUuid());
                    ProxiedPlayer addedPlayer = ProxyServer.getInstance().getPlayer(item.getUuid());
                    if (addedPlayer != null) {
                        String customColor = getTabColor(addedPlayer);
                        if (customColor != null) {
                            item.setDisplayName("{\"extra\":[{\"color\":\"" +
                                    customColor +
                                    "\",\"text\":\"" +
                                    addedPlayer.getDisplayName() +
                                    "\"}],\"text\":\"\"}");
                        }
                    }
                } else if (playerListItem.getAction() == PlayerListItem.Action.REMOVE_PLAYER) {
                    realPlayers.remove(item.getUuid());
                }
            }

            player.unsafe().sendPacket(playerListItem);

            this.updateFakePlayers();
            this.sendHeaderAndFooter();
        }

        @Override
        public void onPingChange(int latency) {
            this.updateFakePlayers();
        }

        @Override
        public void onServerChange() {
            PlayerListItem packet = new PlayerListItem();
            packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
            PlayerListItem.Item[] items = new PlayerListItem.Item[realPlayers.size()];
            int i = 0;
            for (UUID uuid : realPlayers) {
                PlayerListItem.Item item = items[i++] = new PlayerListItem.Item();
                item.setUuid(uuid);
            }
            packet.setItems(items);
            player.unsafe().sendPacket(packet);
            realPlayers.clear();

            this.updateFakePlayers();
        }

        private void updateFakePlayers() {
            Collection<ProxiedPlayer> allPlayers = ProxyServer.getInstance().getPlayers();
            HashSet<UUID> missingPlayers = new HashSet<>(fakePlayers);
            for (ProxiedPlayer otherPlayer : allPlayers) {
                missingPlayers.remove(otherPlayer.getUniqueId());
                if (player != otherPlayer && player.getServer() != otherPlayer.getServer() && !realPlayers.contains(otherPlayer.getUniqueId()) && !fakePlayers.contains(otherPlayer.getUniqueId())) {
                    fakePlayers.add(otherPlayer.getUniqueId());

                    PlayerListItem.Item item = new PlayerListItem.Item();
                    item.setUuid(otherPlayer.getUniqueId());
                    item.setUsername(otherPlayer.getName());
                    item.setGamemode(3);
                    item.setPing(0);
                    item.setProperties(new String[][] {});

                    String customColor = getTabColor(otherPlayer);
                    if (customColor != null) {
                        item.setDisplayName("{\"extra\":[{\"color\":\"" +
                                customColor +
                                "\",\"text\":\"" +
                                otherPlayer.getDisplayName() +
                                "\"}],\"text\":\"\"}");
                    }

                    PlayerListItem packet = new PlayerListItem();
                    packet.setAction(PlayerListItem.Action.ADD_PLAYER);
                    packet.setItems(new PlayerListItem.Item[] { item });
                    player.unsafe().sendPacket(packet);
                }
            }
            for (UUID uuid : missingPlayers) {
                fakePlayers.remove(uuid);

                PlayerListItem.Item item = new PlayerListItem.Item();
                item.setUuid(uuid);

                PlayerListItem packet = new PlayerListItem();
                packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
                packet.setItems(new PlayerListItem.Item[] { item });
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

        private String getTabColor(ProxiedPlayer player) {
            for (String key : manager.getConfig().globalTabColors.keySet()) {
                if (player.hasPermission("hiddenite.chat." + key) || player.hasPermission("yoctochat." + key)) {
                    return manager.getConfig().globalTabColors.get(key);
                }
            }
            return null;
        }

        @Override
        public void onConnect() {
            this.updateFakePlayers();
        }

        @Override
        public void onDisconnect() {}
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
