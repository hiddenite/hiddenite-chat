package eu.hiddenite.chat.managers;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import eu.hiddenite.chat.ChatPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AutoMessageManager extends Manager {
    private final Random random = new Random();
    private final ArrayList<String> currentMessages = new ArrayList<>();
    private ScheduledTask autoMessageTask;

    public AutoMessageManager(ChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onLoad() {
        if (autoMessageTask != null && autoMessageTask.status() == TaskStatus.SCHEDULED) {
            autoMessageTask.cancel();
        }

        if (!getConfig().autoMessages.enabled || getConfig().autoMessages.messages.isEmpty()) {
            return;
        }

        autoMessageTask = getProxy().getScheduler().buildTask(getPlugin(), this::sendRandomMessage).repeat(getConfig().autoMessages.interval, TimeUnit.SECONDS).schedule();

        getLogger().info("Loaded " + getConfig().autoMessages.messages.size() + " messages, sending every " + getConfig().autoMessages.interval + " seconds.");
    }

    private void sendRandomMessage() {
        String message = getConfig().autoMessages.header + getRandomMessage();
        getProxy().sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    private String getRandomMessage() {
        if (currentMessages.isEmpty()) {
            currentMessages.addAll(getConfig().autoMessages.messages);
        }
        return currentMessages.remove(random.nextInt(currentMessages.size()));
    }
}
