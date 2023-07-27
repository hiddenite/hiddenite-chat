package eu.hiddenite.chat.managers;

import eu.hiddenite.chat.ChatPlugin;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AutoMessageManager extends Manager {
    private final Random random = new Random();
    private final ArrayList<String> currentMessages = new ArrayList<>();

    public AutoMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!getConfig().autoMessages.enabled || getConfig().autoMessages.messages.isEmpty()) {
            return;
        }

        getProxy().getScheduler().buildTask(getPlugin(), this::sendRandomMessage).repeat(getConfig().autoMessages.interval, TimeUnit.SECONDS).schedule();

        getLogger().info("Loaded " + getConfig().autoMessages.messages.size() + " messages, sending every " + getConfig().autoMessages.interval + " seconds.");
    }

    private void sendRandomMessage() {
        String message = getConfig().autoMessages.header + getRandomMessage();
        getProxy().sendMessage(Component.text(message));
    }

    private String getRandomMessage() {
        if (currentMessages.size() == 0) {
            currentMessages.addAll(getConfig().autoMessages.messages);
        }
        return currentMessages.remove(random.nextInt(currentMessages.size()));
    }
}
