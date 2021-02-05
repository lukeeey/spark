package me.lucko.spark.cloudburst;

import me.lucko.spark.common.SparkPlatform;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;

public class CloudSparkCommand extends Command {
    private final SparkPlatform platform;

    public CloudSparkCommand(SparkPlatform platform) {
        super("spark", CommandData.builder("spark").build());
        this.platform = platform;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        this.platform.executeCommand(new CloudCommandSender(sender), args);
        return true;
    }
}
