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

import com.google.common.collect.ImmutableMap;
import me.lucko.spark.common.world.ChunkPosition;
import me.lucko.spark.common.world.ChunkStats;
import me.lucko.spark.common.world.CountMap;
import me.lucko.spark.common.world.SparkWorld;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class BukkitWorld implements SparkWorld {
    private final World world;

    public BukkitWorld(World world) {
        this.world = world;
    }

    @Override
    public Map<ChunkPosition, ChunkStats> getChunkStats() {
        ImmutableMap.Builder<ChunkPosition, ChunkStats> chunks = ImmutableMap.builder();



        for (Chunk chunk : this.world.getLoadedChunks()) {
            ChunkPosition position = new ChunkPosition(chunk.getX(), chunk.getZ());

            CountMap<EntityType> entities = new CountMap<>(EntityType.class);
            CountMap<Material> tiles = new CountMap<>(Material.class);

            for (Entity entity : chunk.getEntities()) {
                entities.increment(entity.getType());
            }

            for (BlockState tileEntity : chunk.getTileEntities()) {
                tiles.increment(tileEntity.getType());
            }

            chunks.put(position, new ChunkStats(position, entities.export(CountMap.GENERIC_NAMER), tiles.export(CountMap.GENERIC_NAMER)));
        }

        return chunks.build();
    }


}
