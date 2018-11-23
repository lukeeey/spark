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

package me.lucko.spark.common.command.modules;

import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.memory.MemoryAnalysisAgent;
import me.lucko.spark.memory.ObjectSearchFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MemoryAnalysisModule<S> implements CommandModule<S> {

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("memoryanalysis")
                .executor((platform, sender, arguments) -> {
                    platform.runAsync(() -> {
                        platform.sendPrefixedMessage(sender, "&7Attempting to load VM instrumentation agent, please wait...");

                        MemoryAnalysisAgent usageAnalysis;
                        try {
                            usageAnalysis = MemoryAnalysisAgent.obtain();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                            platform.sendPrefixedMessage(sender, "&eError: Unable to obtain instrumentation instance.");
                            return;
                        }

                        platform.sendPrefixedMessage("&7Performing memory usage analysis, please wait...");
                        long lol = usageAnalysis.deepMemoryUsage(platform);
                        System.out.println(formatBytes(lol));
                    });
                })
                .build()
        );
    }

    public Map<String, Long> analyzePlugins(MemoryAnalysisAgent usageAnalysis, Map<String, Object> plugins, ObjectSearchFilter filter) {
        Map<String, Long> sizes = new HashMap<>();
        for (Map.Entry<String, Object> plugin : plugins.entrySet()) {
            long usage = usageAnalysis.deepMemoryUsage(plugin.getValue(), filter);
            sizes.put(plugin.getKey(), usage);
        }
        return sizes;
    }

    public static String formatBytes(long bytes) {
        if (bytes == 0) {
            return "0 bytes";
        }
        String[] sizes = new String[]{"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f", bytes / Math.pow(1024, sizeIndex)) + " " + sizes[sizeIndex];
    }

}
