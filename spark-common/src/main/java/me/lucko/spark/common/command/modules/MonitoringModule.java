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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.monitor.data.MonitoringManager;
import me.lucko.spark.monitor.data.providers.TpsDataProvider;

import java.io.IOException;
import java.util.function.Consumer;

public class MonitoringModule<S> implements CommandModule<S> {

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("monitoring")
                .executor((platform, sender, arguments) -> {
                    MonitoringManager<S> monitoringManager = platform.getMonitoringManager();
                    platform.runAsync(() -> {
                        try {
                            monitoringManager.getPublisher().ensureSocketSetup(sender);
                        } catch (IOException e) {
                            return;
                        }

                        platform.sendPrefixedMessage(sender, "&7Uploading initial data, please wait...");
                        try {
                            String key = monitoringManager.getPublisher().publish();
                            platform.sendPrefixedMessage("&bMonitoring viewer:");
                            platform.sendLink(SparkPlatform.VIEWER_URL + key);
                        } catch (IOException e) {
                            platform.sendPrefixedMessage(sender, "&cAn error occurred whilst uploading the initial data.");
                            e.printStackTrace();
                        }
                    });
                })
                .tabCompleter(Command.TabCompleter.empty())
                .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("tps")
                .executor((platform, sender, arguments) -> {
                    MonitoringManager monitoringManager = platform.getMonitoringManager();
                    TpsDataProvider tpsProvider = (TpsDataProvider) monitoringManager.getProviders().get("tps");
                    if (tpsProvider == null) {
                        platform.sendPrefixedMessage(sender, "TPS data is not available.");
                        return;
                    }

                    String formattedTpsString = tpsProvider.getTpsCalculator().toFormattedString();
                    platform.sendPrefixedMessage(sender, "TPS from last 5s, 10s, 1m, 5m, 15m");
                    platform.sendPrefixedMessage(sender, formattedTpsString);
                })
                .tabCompleter(Command.TabCompleter.empty())
                .build()
        );
    }




}
