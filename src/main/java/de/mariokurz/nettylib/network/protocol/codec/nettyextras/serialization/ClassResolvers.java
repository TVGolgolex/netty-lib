/*
 * Copyright 2021 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package de.mariokurz.nettylib.network.protocol.codec.nettyextras.serialization;

import io.netty5.util.internal.PlatformDependent;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassResolvers {

    private ClassResolvers() {
        // Unused
    }

    /**
     * cache disabled
     *
     * @param classLoader - specific classLoader to use, or null if you want to revert to default
     * @return new instance of class resolver
     */
    public static ClassResolver cacheDisabled(ClassLoader classLoader) {
        return new ClassLoaderClassResolver(defaultClassLoader(classLoader));
    }

    /**
     * non-aggressive non-concurrent cache
     * good for non-shared default cache
     *
     * @param classLoader - specific classLoader to use, or null if you want to revert to default
     * @return new instance of class resolver
     */
    public static ClassResolver weakCachingResolver(ClassLoader classLoader) {
        return new CachingClassResolver(
                new ClassLoaderClassResolver(defaultClassLoader(classLoader)),
                new WeakReferenceMap<>(new HashMap<>()));
    }

    /**
     * aggressive non-concurrent cache
     * good for non-shared cache, when we're not worried about class unloading
     *
     * @param classLoader - specific classLoader to use, or null if you want to revert to default
     * @return new instance of class resolver
     */
    public static ClassResolver softCachingResolver(ClassLoader classLoader) {
        return new CachingClassResolver(
                new ClassLoaderClassResolver(defaultClassLoader(classLoader)),
                new SoftReferenceMap<>(new HashMap<>()));
    }

    /**
     * non-aggressive concurrent cache
     * good for shared cache, when we're worried about class unloading
     *
     * @param classLoader - specific classLoader to use, or null if you want to revert to default
     * @return new instance of class resolver
     */
    public static ClassResolver weakCachingConcurrentResolver(ClassLoader classLoader) {
        return new CachingClassResolver(
                new ClassLoaderClassResolver(defaultClassLoader(classLoader)),
                new WeakReferenceMap<>(new ConcurrentHashMap<>()));
    }

    /**
     * aggressive concurrent cache
     * good for shared cache, when we're not worried about class unloading
     *
     * @param classLoader - specific classLoader to use, or null if you want to revert to default
     * @return new instance of class resolver
     */
    public static ClassResolver softCachingConcurrentResolver(ClassLoader classLoader) {
        return new CachingClassResolver(
                new ClassLoaderClassResolver(defaultClassLoader(classLoader)),
                new SoftReferenceMap<>(new ConcurrentHashMap<>()));
    }

    static ClassLoader defaultClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }

        final ClassLoader contextClassLoader = PlatformDependent.getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }

        return PlatformDependent.getClassLoader(ClassResolvers.class);
    }
}