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

package me.lucko.spark.monitor.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.spark.common.SparkPlatform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MonitoringManager<S> implements Runnable, AutoCloseable {
    private final SparkPlatform<S> platform;
    private final MonitoringPublisher<S> publisher;

    private final LinkedList<Data> data = new LinkedList<>();
    private final Map<String, DataProvider> providers = new HashMap<>();
    private final List<DataListener> listeners = new ArrayList<>();

    private final int historyLength;

    public MonitoringManager(SparkPlatform<S> platform, int historyLength) {
        this.platform = platform;
        this.historyLength = historyLength;
        this.publisher = new MonitoringPublisher<>(platform, this);
    }

    public MonitoringPublisher<S> getPublisher() {
        return this.publisher;
    }

    public void addDataProvider(String id, DataProvider dataProvider) {
        this.providers.put(id, dataProvider);
    }

    public void addDataListener(DataListener dataListener) {
        this.listeners.add(dataListener);
    }

    public void removeDataListener(DataListener dataListener) {
        this.listeners.remove(dataListener);
    }

    public Map<String, DataProvider> getProviders() {
        return Collections.unmodifiableMap(this.providers);
    }

    public List<DataListener> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    @Override
    public void run() {
        // collect the data
        long time = System.currentTimeMillis();
        JsonObject data = new JsonObject();

        // gather data from providers that must be called sync
        for (Map.Entry<String, DataProvider> provider : this.providers.entrySet()) {
            if (provider.getValue().requiresSync()) {
                data.add(provider.getKey(), provider.getValue().gather());
            }
        }

        // complete the rest of the action async
        this.platform.runAsync(() -> {
            // gather data from providers that can be called async
            for (Map.Entry<String, DataProvider> provider : this.providers.entrySet()) {
                if (!provider.getValue().requiresSync()) {
                    data.add(provider.getKey(), provider.getValue().gather());
                }
            }

            // add it to the data list
            this.data.addFirst(new Data(time, data));

            // pass it onto the listeners
            for (DataListener dataListener : this.listeners) {
                dataListener.onDataCollection(time, data);
            }

            // ensure we only keep the required amount of data around in memory
            while (this.data.size() > this.historyLength) {
                this.data.removeLast();
            }
        });
    }

    public JsonArray export() {
        /*
        [
            {
                "time": 1554828641,
                "data": {
                    "tps": 20.0,
                    "mem": 100000
                }
            },
            {
                "time": 1554828646,
                "data": {
                    "tps": 19.9,
                    "mem": 100000
                }
            },
        ]
         */
        JsonArray exported = new JsonArray();
        for (Data entry : this.data) {
            JsonObject entryObject = new JsonObject();
            entryObject.add("time", new JsonPrimitive(entry.time));
            entryObject.add("data", entry.data);
            exported.add(entryObject);
        }
        return exported;
    }

    @Override
    public void close() throws Exception {
        for (DataListener listener : this.listeners) {
            if (listener instanceof AutoCloseable) {
                ((AutoCloseable) listener).close();
            }
        }
        for (DataProvider provider : this.providers.values()) {
            if (provider instanceof AutoCloseable) {
                ((AutoCloseable) provider).close();
            }
        }
        this.listeners.clear();
        this.providers.clear();
    }

    private static final class Data {
        private final long time;
        private final JsonObject data;

        Data(long time, JsonObject data) {
            this.time = time;
            this.data = data;
        }
    }

}
