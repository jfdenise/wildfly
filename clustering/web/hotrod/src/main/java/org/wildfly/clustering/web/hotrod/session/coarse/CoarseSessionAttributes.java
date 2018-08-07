/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session.coarse;

import java.io.NotSerializableException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.Immutability;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.web.cache.session.SessionAttributes;

/**
 * @author Paul Ferraro
 */
public class CoarseSessionAttributes extends CoarseImmutableSessionAttributes implements SessionAttributes {

    private final Map<String, Object> attributes;
    private final Set<String> mutations = ConcurrentHashMap.newKeySet();
    private final Mutator mutator;
    private final Marshallability marshallability;

    public CoarseSessionAttributes(Map<String, Object> attributes, Mutator mutator, Marshallability marshallability) {
        super(attributes);
        this.attributes = attributes;
        this.mutator = mutator;
        this.marshallability = marshallability;
    }

    @Override
    public Object removeAttribute(String name) {
        Object value = this.attributes.remove(name);
        this.mutator.mutate();
        this.mutations.remove(name);
        return value;
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        if (!this.marshallability.isMarshallable(value)) {
            throw new IllegalArgumentException(new NotSerializableException(value.getClass().getName()));
        }
        Object old = this.attributes.put(name, value);
        this.mutator.mutate();
        this.mutations.remove(name);
        return old;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.attributes.keySet();
    }

    @Override
    public Object getAttribute(String name) {
        Object value = this.attributes.get(name);
        if (!Immutability.INSTANCE.test(value)) {
            this.mutations.add(name);
        }
        return value;
    }

    @Override
    public void close() {
        if (!this.mutations.isEmpty()) {
            this.mutator.mutate();
            this.mutations.clear();
        }
    }
}
