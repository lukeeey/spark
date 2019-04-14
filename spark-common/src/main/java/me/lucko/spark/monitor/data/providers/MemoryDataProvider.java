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

package me.lucko.spark.monitor.data.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.management.GarbageCollectionNotificationInfo;

import me.lucko.spark.monitor.GarbageCollectionMonitor;
import me.lucko.spark.monitor.data.DataProvider;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class MemoryDataProvider implements DataProvider, GarbageCollectionMonitor.Listener, AutoCloseable {
    private final GarbageCollectionMonitor gcMonitor;
    private String loggedGc = null;

    public MemoryDataProvider() {
        this.gcMonitor = new GarbageCollectionMonitor();
        this.gcMonitor.addListener(this);
    }

    @Override
    public JsonElement gather() {
        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();

        JsonObject data = new JsonObject();
        data.add("heap", serializeUsageData(bean.getHeapMemoryUsage()));
        data.add("nonHeap", serializeUsageData(bean.getNonHeapMemoryUsage()));

        if (this.loggedGc != null) {
            data.add("gc", new JsonPrimitive(this.loggedGc));
            this.loggedGc = null;
        }

        return data;
    }

    private static JsonElement serializeUsageData(MemoryUsage usageData) {
        JsonObject data = new JsonObject();
        data.add("used", new JsonPrimitive(usageData.getUsed()));
        data.add("committed", new JsonPrimitive(usageData.getCommitted()));
        data.add("max", new JsonPrimitive(usageData.getMax()));
        return data;
    }

    @Override
    public void onGc(GarbageCollectionNotificationInfo data) {
        String gcType = data.getGcAction();
        if (gcType.equals("end of minor GC")) {
            if (this.loggedGc == null) {
                this.loggedGc = "Young Gen GC";
            }
        } else if (gcType.equals("end of major GC")) {
            if (this.loggedGc == null || this.loggedGc.equals("Young Gen GC")) {
                this.loggedGc = "Old Gen GC";
            }
        } else {
            this.loggedGc = gcType;
        }
    }

    @Override
    public void close() throws Exception {
        this.gcMonitor.close();
    }

}
