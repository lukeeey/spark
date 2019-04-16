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
import me.lucko.spark.util.LoadingMap;
import org.bukkit.Server;
import org.bukkit.World;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

public class WorldDataProvider implements DataProvider {
    private final Server server;

    private final Map<World, IntSupplier> chunkCounters = LoadingMap.of(new HashMap<>(), WorldDataProvider::getChunkCounter);
    private final Map<World, IntSupplier> entityCounters = LoadingMap.of(new HashMap<>(), WorldDataProvider::getEntityCounter);
    private final Map<World, IntSupplier> tickableTileEntityCounters = LoadingMap.of(new HashMap<>(), WorldDataProvider::getTickableTileEntityCounter);

    public WorldDataProvider(Server server) {
        this.server = server;
    }

    @Override
    public JsonElement gather() {
        JsonObject worlds = new JsonObject();

        int globalChunks = 0;
        int globalEntities = 0;
        int globalTickableTileEntities = 0;

        for (World world : this.server.getWorlds()) {
            JsonObject worldData = new JsonObject();

            try {
                int chunks = this.chunkCounters.get(world).getAsInt();
                globalChunks += chunks;
                worldData.add("chunkCount", new JsonPrimitive(chunks));
            } catch (Exception e) {
                // ignore
            }

            try {
                int entities = this.entityCounters.get(world).getAsInt();
                globalEntities += entities;
                worldData.add("entityCount", new JsonPrimitive(entities));
            } catch (Exception e) {
                // ignore
            }

            try {
                int tickableTileEntities = this.tickableTileEntityCounters.get(world).getAsInt();
                globalTickableTileEntities += tickableTileEntities;
                worldData.add("tickableTileEntityCount", new JsonPrimitive(tickableTileEntities));
            } catch (Exception e) {
                // ignore
            }

            worlds.add(world.getName(), worldData);
        }

        JsonObject data = new JsonObject();
        data.add("worlds", worlds);

        // form global json data
        JsonObject globalData = new JsonObject();
        globalData.add("chunkCount", new JsonPrimitive(globalChunks));
        globalData.add("entityCount", new JsonPrimitive(globalEntities));
        globalData.add("tickableTileEntityCount", new JsonPrimitive(globalTickableTileEntities));

        data.add("global", globalData);

        return data;
    }

    private static IntSupplier getChunkCounter(World world) {
        Class<? extends World> worldImplClass = world.getClass();

        // paper
        try {
            Method getChunkCountMethod = worldImplClass.getMethod("getChunkCount");
            return () -> {
                try {
                    return (int) getChunkCountMethod.invoke(world);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            // ignore
        }

        // craftbukkit
        try {
            Method getHandleMethod = worldImplClass.getMethod("getHandle");
            Object worldHandle = getHandleMethod.invoke(world);

            Field chunkProviderField;
            try {
                chunkProviderField = worldHandle.getClass().getField("chunkProviderServer");
            } catch (NoSuchFieldException e) {
                chunkProviderField = worldHandle.getClass().getSuperclass().getDeclaredField("chunkProvider");
                chunkProviderField.setAccessible(true);
            }

            Object chunkProvider = chunkProviderField.get(worldHandle);

            Field chunksField = chunkProvider.getClass().getField("chunks");
            Object chunks = chunksField.get(chunkProvider);
            Method chunksSizeMethod = chunks.getClass().getMethod("size");

            return () -> {
                try {
                    return (int) chunksSizeMethod.invoke(chunks);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            // ignore
        }

        return () -> {
            throw new UnsupportedOperationException();
        };
    }

    private static IntSupplier getEntityCounter(World world) {
        Class<? extends World> worldImplClass = world.getClass();

        // paper
        try {
            Method getEntityCountMethod = worldImplClass.getMethod("getEntityCount");
            return () -> {
                try {
                    return (int) getEntityCountMethod.invoke(world);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            // ignore
        }

        // craftbukkit
        try {
            Method getHandleMethod = worldImplClass.getMethod("getHandle");
            Object worldHandle = getHandleMethod.invoke(world);

            Field entityListField = worldHandle.getClass().getField("entityList");
            Collection<?> entityList = (Collection<?>) entityListField.get(worldHandle);
            return entityList::size;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            // ignore
        }

        return () -> {
            throw new UnsupportedOperationException();
        };
    }

    private static IntSupplier getTickableTileEntityCounter(World world) {
        Class<? extends World> worldImplClass = world.getClass();

        // paper
        try {
            Method getTickableTileEntityCountMethod = worldImplClass.getMethod("getTickableTileEntityCount");
            return () -> {
                try {
                    return (int) getTickableTileEntityCountMethod.invoke(world);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            // ignore
        }

        // craftbukkit
        try {
            Method getHandleMethod = worldImplClass.getMethod("getHandle");
            Object worldHandle = getHandleMethod.invoke(world);

            Field tileEntityTickListField = worldHandle.getClass().getField("tileEntityTickList");
            Collection<?> tileEntityTickList = (Collection<?>) tileEntityTickListField.get(worldHandle);
            return tileEntityTickList::size;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            // ignore
        }

        return () -> {
            throw new UnsupportedOperationException();
        };
    }

    @Override
    public boolean requiresSync() {
        return true;
    }
}
