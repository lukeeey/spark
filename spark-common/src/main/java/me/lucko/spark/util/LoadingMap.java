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

package me.lucko.spark.util;

import com.google.common.collect.ForwardingMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LoadingMap<K, V> extends ForwardingMap<K, V> implements Map<K, V> {
    public static <K, V> LoadingMap<K, V> of(Map<K, V> map, Function<K, V> function) {
        return new LoadingMap<>(map, function);
    }

    public static <K, V> LoadingMap<K, V> of(Function<K, V> function) {
        return of(new ConcurrentHashMap<>(), function);
    }

    private final Map<K, V> map;
    private final Function<K, V> function;

    private LoadingMap(Map<K, V> map, Function<K, V> function) {
        this.map = map;
        this.function = function;
    }

    @Override
    protected Map<K, V> delegate() {
        return this.map;
    }

    public V getIfPresent(K key) {
        return this.map.get(key);
    }

    @Override
    public V get(Object key) {
        V value = this.map.get(key);
        if (value != null) {
            return value;
        }
        //noinspection unchecked
        return this.map.computeIfAbsent((K) key, this.function);
    }
}