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

package me.lucko.spark.memory;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The spark Java Agent implementation.
 *
 * <p>Used to access and make approximations using the results of
 * {@link Instrumentation#getObjectSize(Object)}.</p>
 */
public class MemoryAnalysisAgent {

    private static MemoryAnalysisAgent instance = null;

    public static synchronized MemoryAnalysisAgent obtain() throws IllegalStateException {
        if (instance != null) {
            return instance;
        }
        return instance =  new MemoryAnalysisAgent(ByteBuddyAgent.install());
    }

    private final Instrumentation instrumentation;

    public MemoryAnalysisAgent(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     * Calculates an approximation of the amount of memory consumed by the specified object.
     *
     * @param obj the object to measure
     * @return an estimate of the memory usage of the object
     * @see Instrumentation#getObjectSize(Object)
     */
    public long memoryUsage(Object obj) {
        return this.instrumentation.getObjectSize(obj);
    }

    /**
     * Calculates an approximation of the deep amount of memory consumed by the specified object.
     *
     * @param obj the object to measure
     * @return an estimate of the deep memory usage of the object
     */
    public long deepMemoryUsage(Object obj) {
        return deepMemoryUsage(obj, ObjectSearchFilter.SEARCH_ALL);
    }

    /**
     * Calculates an approximation of the deep amount of memory consumed by the specified object.
     *
     * @param obj the object to measure
     * @param filter defines how the deep search should be performed
     * @return an estimate of the deep memory usage of the object
     */
    public long deepMemoryUsage(Object obj, ObjectSearchFilter filter) {
        return calculateDeepSize(new HashSet<>(), obj, filter);
    }

    private long calculateDeepSize(Set<Integer> seen, Object initialObject, ObjectSearchFilter filter) {
        Objects.requireNonNull(initialObject, "initialObject");

        // create a new stack, push initial object
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(initialObject);

        long total = 0L;

        // iterative traversal of the object tree.
        while (!stack.isEmpty()) {
            // pop the next object
            Object object = stack.pop();
            Class<?> clazz = object.getClass();

            // only process the object if we've not seen it before.
            if (!seen.add(System.identityHashCode(object))) {
                continue;
            }

            // check filters
            if (object != initialObject && (!filter.shouldProcess(clazz) || !filter.shouldProcess(object))) {
                continue;
            }

            // accumulate the objects size
            total += memoryUsage(object);

            // find child objects
            findChildObjects(stack, clazz, object, filter);
        }

        return total;
    }

    private static void findChildObjects(Deque<Object> stack, Class<?> clazz, Object object, ObjectSearchFilter filter) {
        // find children as array elements
        Class<?> componentType = clazz.getComponentType();
        if (componentType != null && !componentType.isPrimitive() && filter.shouldFollowArrayElements(componentType)) {
            Object[] array = (Object[]) object;
            for (Object el : array) {
                if (el != null) {
                    stack.push(el);
                }
            }
        }

        // find children in fields
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().isPrimitive() || !filter.shouldFollow(field)) {
                    continue;
                }

                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                Object value;
                try {
                    value = field.get(object);
                } catch (IllegalAccessException e) {
                    continue;
                }

                if (value != null) {
                    stack.push(value);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

}
