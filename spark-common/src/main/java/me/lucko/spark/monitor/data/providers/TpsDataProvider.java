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

import me.lucko.spark.monitor.TpsCalculator;
import me.lucko.spark.monitor.data.DataProvider;
import me.lucko.spark.sampler.TickCounter;

public class TpsDataProvider implements DataProvider, AutoCloseable {
    private final TickCounter tickCounter;
    private final TpsCalculator tpsCalculator;

    public TpsDataProvider(TickCounter tickCounter) {
        this.tickCounter = tickCounter;
        this.tpsCalculator = new TpsCalculator();
        tickCounter.addTickTask(this.tpsCalculator);
    }

    public TpsCalculator getTpsCalculator() {
        return this.tpsCalculator;
    }

    @Override
    public JsonElement gather() {
        JsonObject data = new JsonObject();
        data.add("5s", new JsonPrimitive(this.tpsCalculator.avg5Sec().getAverage()));
        data.add("10s", new JsonPrimitive(this.tpsCalculator.avg10Sec().getAverage()));
        data.add("1m", new JsonPrimitive(this.tpsCalculator.avg1Min().getAverage()));
        data.add("5m", new JsonPrimitive(this.tpsCalculator.avg5Min().getAverage()));
        data.add("15m", new JsonPrimitive(this.tpsCalculator.avg15Min().getAverage()));
        return data;
    }

    @Override
    public void close() throws Exception {
        this.tickCounter.removeTickTask(this.tpsCalculator);
    }
}
