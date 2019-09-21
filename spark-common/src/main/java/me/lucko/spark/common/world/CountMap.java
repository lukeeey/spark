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

package me.lucko.spark.common.world;

import com.google.common.collect.ImmutableMap;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class CountMap<E extends Enum<E>> {
    public static final Function<Enum<?>, String> GENERIC_NAMER = e -> e.name().toLowerCase().replace('_', ' ');

    private final EnumMap<E, AtomicInteger> counts;

    public CountMap(Class<E> type) {
        this.counts = new EnumMap<>(type);
    }

    public void increment(E type) {
        AtomicInteger counter = this.counts.get(type);
        if (counter == null) {
            counter = new AtomicInteger(0);
            this.counts.put(type, counter);
        }
        counter.incrementAndGet();
    }

    public Map<String, Integer> export(Function<? super E, String> nameFunction) {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (Map.Entry<E, AtomicInteger> entry : this.counts.entrySet()) {
            builder.put(nameFunction.apply(entry.getKey()), entry.getValue().get());
        }
        return builder.build();
    }
}
