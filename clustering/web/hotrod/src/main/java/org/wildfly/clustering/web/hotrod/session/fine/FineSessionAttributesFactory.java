/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryMutator;
import org.wildfly.clustering.marshalling.spi.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.hotrod.Logger;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory<V> implements SessionAttributesFactory<SessionAttributeNamesEntry> {

    private final RemoteCache<SessionAttributeNamesKey, SessionAttributeNamesEntry> namesCache;
    private final RemoteCache<SessionAttributeKey, V> attributeCache;
    private final Marshaller<Object, V> marshaller;
    private final CacheProperties properties;

    public FineSessionAttributesFactory(RemoteCache<SessionAttributeNamesKey, SessionAttributeNamesEntry> namesCache, RemoteCache<SessionAttributeKey, V> attributeCache, Marshaller<Object, V> marshaller, CacheProperties properties) {
        this.namesCache = namesCache;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
        this.properties = properties;
    }

    @Override
    public SessionAttributeNamesEntry createValue(String id, Void context) {
        SessionAttributeNamesEntry entry = new SessionAttributeNamesEntry(new AtomicInteger(), new ConcurrentHashMap<>());
        this.namesCache.put(new SessionAttributeNamesKey(id), entry);
        return entry;
    }

    @Override
    public SessionAttributeNamesEntry findValue(String id) {
        SessionAttributeNamesEntry entry = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (entry != null) {
            Map<String, Integer> names = entry.getNames();
            Set<SessionAttributeKey> keys = new HashSet<>(names.size());
            for (Integer attributeId : names.values()) {
                keys.add(new SessionAttributeKey(id, attributeId));
            }
            Map<SessionAttributeKey, V> attributes = this.attributeCache.getAll(keys);
            // If any attributes are invalid - remove them all
            for (Map.Entry<String, Integer> nameEntry : names.entrySet()) {
                V value = attributes.get(new SessionAttributeKey(id, nameEntry.getValue()));
                if (value != null) {
                    try {
                        this.marshaller.read(value);
                        continue;
                    } catch (InvalidSerializedFormException e) {
                        Logger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, nameEntry.getKey());
                    }
                } else {
                    Logger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, nameEntry.getKey());
                }
                this.remove(id);
                return null;
            }
        }
        return entry;
    }

    @Override
    public boolean remove(String id) {
        SessionAttributeNamesEntry entry = this.namesCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new SessionAttributeNamesKey(id));
        if (entry == null) return false;
        entry.getNames().values().forEach(attributeId -> this.attributeCache.remove(new SessionAttributeKey(id, attributeId)));
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, SessionAttributeNamesEntry entry) {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey(id);
        Mutator mutator = new RemoteCacheEntryMutator<>(this.namesCache, key, entry);
        return new FineSessionAttributes<>(id, entry.getSequence(), entry.getNames(), mutator, this.attributeCache, this.marshaller, this.properties);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, SessionAttributeNamesEntry entry) {
        return new FineImmutableSessionAttributes<>(id, entry.getNames(), this.attributeCache, this.marshaller);
    }
}
