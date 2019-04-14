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

package me.lucko.spark.bukkit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.spark.monitor.data.DataProvider;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityDataProvider implements DataProvider {
    private final Server server;

    public EntityDataProvider(Server server) {
        this.server = server;
    }

    @Override
    public JsonElement gather() {
        JsonObject worlds = new JsonObject();

        Map<EntityType, AtomicInteger> globalEntityCounts = new EnumMap<>(EntityType.class);
        int globalTotalEntities = 0;
        int globalLoadedChunks = 0;

        for (World world : this.server.getWorlds()) {
            Map<EntityType, AtomicInteger> entityCounts = new EnumMap<>(EntityType.class);
            int totalEntities = 0;

            // collect world data
            for (Entity entity : world.getEntities()) {
                entityCounts.computeIfAbsent(entity.getType(), x -> new AtomicInteger()).incrementAndGet();
                totalEntities++;
            }
            int loadedChunks = world.getLoadedChunks().length;

            // merge into global
            for (Map.Entry<EntityType, AtomicInteger> entry : entityCounts.entrySet()) {
                globalEntityCounts.computeIfAbsent(entry.getKey(), x -> new AtomicInteger()).addAndGet(entry.getValue().get());
            }
            globalTotalEntities += totalEntities;
            globalLoadedChunks += loadedChunks;

            // form json data
            JsonObject worldData = new JsonObject();
            worldData.add("entities", serializeEntityCounts(entityCounts));
            worldData.add("totalEntities", new JsonPrimitive(totalEntities));
            worldData.add("chunks", new JsonPrimitive(loadedChunks));

            worlds.add(world.getName(), worldData);
        }

        JsonObject data = new JsonObject();
        data.add("worlds", worlds);

        // form global json data
        JsonObject globalData = new JsonObject();
        globalData.add("entities", serializeEntityCounts(globalEntityCounts));
        globalData.add("totalEntities", new JsonPrimitive(globalTotalEntities));
        globalData.add("chunks", new JsonPrimitive(globalLoadedChunks));

        data.add("global", globalData);

        return data;
    }

    private static JsonObject serializeEntityCounts(Map<EntityType, AtomicInteger> map) {
        JsonObject data = new JsonObject();
        for (Map.Entry<EntityType, AtomicInteger> entry : map.entrySet()) {
            data.add(entry.getKey().name(), new JsonPrimitive(entry.getValue().get()));
        }
        return data;
    }

    @Override
    public boolean requiresSync() {
        return true;
    }
}
