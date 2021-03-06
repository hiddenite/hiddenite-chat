package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AutoMessageManager extends Manager implements Listener {
    private final Random random = new Random();
    private final ArrayList<String> currentMessages = new ArrayList<>();

    public AutoMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!getConfig().autoMessagesEnabled || getConfig().autoMessagesList.isEmpty()) {
            return;
        }

        getProxy().getScheduler().schedule(getPlugin(),
                this::sendRandomMessage,
                getConfig().autoMessagesInterval,
                getConfig().autoMessagesInterval,
                TimeUnit.SECONDS);

        getLogger().info("Loaded " + getConfig().autoMessagesList.size() + " messages, sending every " + getConfig().autoMessagesInterval + " seconds.");
    }

    private void sendRandomMessage() {
        String message = getConfig().autoMessagesHeader + getRandomMessage();
        getProxy().broadcast(TextComponent.fromLegacyText(message));
    }

    private String getRandomMessage() {
        if (currentMessages.size() == 0) {
            currentMessages.addAll(getConfig().autoMessagesList);
        }
        return currentMessages.remove(random.nextInt(currentMessages.size()));
    }
}
