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

import me.lucko.spark.monitor.data.DataProvider;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class CpuDataProvider implements DataProvider {

    /** The object name of the com.sun.management.OperatingSystemMXBean */
    private static final String OPERATING_SYSTEM_BEAN = "java.lang:type=OperatingSystem";

    /** The beans */
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;

    public CpuDataProvider() {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(OPERATING_SYSTEM_BEAN);
            this.osBean = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
    }

    @Override
    public JsonElement gather() {
        JsonObject data = new JsonObject();
        data.add("availableProcessors", new JsonPrimitive(this.osBean.getAvailableProcessors()));
        data.add("systemLoadAverage", new JsonPrimitive(this.osBean.getSystemLoadAverage()));
        data.add("systemCpuLoad", new JsonPrimitive(this.osBean.getSystemCpuLoad()));
        data.add("processCpuLoad", new JsonPrimitive(this.osBean.getProcessCpuLoad()));
        data.add("processCpuTime", new JsonPrimitive(this.osBean.getProcessCpuTime()));
        data.add("vmStart", new JsonPrimitive(this.runtimeBean.getStartTime()));
        data.add("vmUptime", new JsonPrimitive(this.runtimeBean.getUptime()));
        return data;
    }

    public interface OperatingSystemMXBean {
        int getAvailableProcessors();
        double getSystemLoadAverage();
        double getSystemCpuLoad();
        double getProcessCpuLoad();
        long getProcessCpuTime();
    }

}
