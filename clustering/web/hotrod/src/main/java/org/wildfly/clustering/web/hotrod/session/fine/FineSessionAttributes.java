/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.NotSerializableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryMutator;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.SessionAttributeImmutability;
import org.wildfly.clustering.web.cache.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<V> extends FineImmutableSessionAttributes<V> implements SessionAttributes, Function<String, Integer> {
    private final AtomicInteger sequence;
    private final Map<String, Integer> names;
    private final Mutator namesMutator;
    private final RemoteCache<SessionAttributeKey, V> cache;
    private final Map<String, Mutator> mutations = new ConcurrentHashMap<>();
    private final Marshaller<Object, V> marshaller;
    private final CacheProperties properties;

    public FineSessionAttributes(String id, AtomicInteger sequence, Map<String, Integer> names, Mutator namesMutator, RemoteCache<SessionAttributeKey, V> cache, Marshaller<Object, V> marshaller, CacheProperties properties) {
        super(id, names, cache, marshaller);
        this.sequence = sequence;
        this.names = names;
        this.namesMutator = namesMutator;
        this.cache = cache;
        this.marshaller = marshaller;
        this.properties = properties;
    }

    @Override
    public Integer apply(String key) {
        return this.sequence.incrementAndGet();
    }

    @Override
    public Object removeAttribute(String name) {
        Integer attributeId = this.names.remove(name);
        if (attributeId == null) return null;
        this.namesMutator.mutate();
        Object result = this.read(name, this.cache.withFlags(Flag.FORCE_RETURN_VALUE).remove(this.apply(attributeId)));
        this.mutations.remove(name);
        return result;
    }

    @Override
    public Object setAttribute(String name, Object attribute) {
        if (attribute == null) {
            return this.removeAttribute(name);
        }
        if (!this.marshaller.isMarshallable(attribute)) {
            throw new IllegalArgumentException(new NotSerializableException(attribute.getClass().getName()));
        }
        V value = this.marshaller.write(attribute);
        int currentId = this.sequence.get();
        int attributeId = this.names.computeIfAbsent(name, this);
        if (attributeId > currentId) {
            this.namesMutator.mutate();
        }
        Object result = this.read(name, this.cache.withFlags(Flag.FORCE_RETURN_VALUE).put(this.apply(attributeId), value));
        this.mutations.remove(name);
        return result;
    }

    @Override
    public Object getAttribute(String name) {
        Integer attributeId = this.names.get(name);
        if (attributeId == null) return null;
        SessionAttributeKey key = this.apply(attributeId);
        V value = this.cache.get(key);
        Object attribute = this.read(name, value);
        if (attribute != null) {
            // If the object is mutable, we need to indicate that the attribute should be replicated
            if (!SessionAttributeImmutability.INSTANCE.test(attribute)) {
                Mutator mutator = new RemoteCacheEntryMutator<>(this.cache, key, value);
                // If cache is not transactional, mutate on close instead.
                if ((this.mutations.putIfAbsent(name, mutator) == null) && this.properties.isTransactional()) {
                    mutator.mutate();
                }
            }
        }
        return attribute;
    }

    @Override
    public void close() {
        if (!this.properties.isTransactional()) {
            for (Mutator mutator : this.mutations.values()) {
                mutator.mutate();
            }
        }
        this.mutations.clear();
    }
}
