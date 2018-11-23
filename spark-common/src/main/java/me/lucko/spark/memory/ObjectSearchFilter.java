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

import java.lang.reflect.Field;

/**
 * Defines the bounds for the deep object search.
 */
public interface ObjectSearchFilter {

    /**
     * Processes and follows all potential children.
     */
    ObjectSearchFilter SEARCH_ALL = new ObjectSearchFilter(){};

    /**
     * Returns if the calculation should process the given object.
     *
     * <p>Note that instead of checking the {@link Object#getClass()} of the given object,
     * simply use the {@link #shouldProcess(Class)} check.</p>
     *
     * @param object the object
     * @return if the calculation should process
     */
    default boolean shouldProcess(Object object) {
        return true;
    }

    /**
     * Returns if the calculation should process objects of the given class.
     *
     * @param clazz the class
     * @return if the calculation should process
     */
    default boolean shouldProcess(Class<?> clazz) {
        return true;
    }

    /**
     * Returns if the deep object search algorithm should "follow" the given field looking
     * for children.
     *
     * @param field the field
     * @return if the algorithms should follow
     */
    default boolean shouldFollow(Field field) {
        return true;
    }

    /**
     * Returns if the deep object search algorithm should "follow" an array of the given type
     * looking for children.
     *
     * @param componentType the array component type
     * @return if the algorithms should follow
     * @see Class#getComponentType()
     */
    default boolean shouldFollowArrayElements(Class<?> componentType) {
        return true;
    }
}
