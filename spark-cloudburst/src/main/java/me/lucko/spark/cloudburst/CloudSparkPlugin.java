/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.cloudburst;

import com.google.inject.Inject;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.server.ServerCommandEvent;
import org.cloudburstmc.server.event.server.ServerInitializationEvent;
import org.cloudburstmc.server.event.server.ServerShutdownEvent;
import org.cloudburstmc.server.plugin.Plugin;
import org.cloudburstmc.server.plugin.PluginDescription;
import org.cloudburstmc.server.scheduler.AsyncTask;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

@Plugin(id = "spark",
        name = "spark",
        version = "@version@",
        description = "@desc@",
        authors = {"Luck", "sk89q", "lukeeey"})
public class CloudSparkPlugin implements SparkPlugin {
    private SparkPlatform platform;

    private Server server;
    private PluginDescription description;
    private Path dataDirectory;

    @Inject
    public CloudSparkPlugin(Server server, PluginDescription description, Path dataDirectory) {
        this.server = server;
        this.description = description;
        this.dataDirectory = dataDirectory;
    }

    @Listener
    public void onInitialization(ServerInitializationEvent event) {
        this.platform = new SparkPlatform(this);
        this.platform.enable();

        //Server.getInstance().getCommandRegistry().register(container, new CloudSparkCommand(platform));
    }

    @Listener
    public void onShutdown(ServerShutdownEvent event) {
        this.platform.disable();
    }

    @Listener
    public void onServerCommand(ServerCommandEvent event) {
        ArrayList<String> parsed = parseArguments(event.getCommand());
        if (parsed.size() == 0) {
            return;
        }
        String sentCmd = parsed.remove(0).toLowerCase();
        if (sentCmd.equalsIgnoreCase("spark")) {
            this.platform.executeCommand(new CloudCommandSender(event.getSender()), parsed.toArray(new String[0]));
        }
    }

    private ArrayList<String> parseArguments(String cmdLine) {
        StringBuilder sb = new StringBuilder(cmdLine);
        ArrayList<String> args = new ArrayList<>();
        boolean notQuoted = true;
        int start = 0;

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\\') {
                sb.deleteCharAt(i);
                continue;
            }

            if (sb.charAt(i) == ' ' && notQuoted) {
                String arg = sb.substring(start, i);
                if (!arg.isEmpty()) {
                    args.add(arg);
                }
                start = i + 1;
            } else if (sb.charAt(i) == '"') {
                sb.deleteCharAt(i);
                --i;
                notQuoted = !notQuoted;
            }
        }

        String arg = sb.substring(start);
        if (!arg.isEmpty()) {
            args.add(arg);
        }
        return args;
    }

    @Override
    public String getVersion() {
        return description.getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return dataDirectory;
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<CloudCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                server.getOnlinePlayers().values().stream().filter(player -> player.hasPermission(permission)),
                Stream.of(server.getConsoleSender())
        ).map(CloudCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        server.getScheduler().scheduleAsyncTask(this, new AsyncTask() {
            @Override
            public void onRun() {
                task.run();
            }
        });
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new CloudPlatformInfo(server);
    }
}
