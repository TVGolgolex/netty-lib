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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map;

final class SoftReferenceMap<K, V> extends ReferenceMap<K, V> {

    SoftReferenceMap(Map<K, Reference<V>> delegate) {
        super(delegate);
    }

    @Override
    Reference<V> fold(V value) {
        return new SoftReference<>(value);
    }
}
