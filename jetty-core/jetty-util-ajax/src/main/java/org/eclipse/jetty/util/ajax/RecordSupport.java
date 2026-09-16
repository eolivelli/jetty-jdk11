//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.util.ajax;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Reflective access to the record APIs introduced in Java 16
 * ({@code Class.isRecord()}, {@code Class.getRecordComponents()}),
 * so that this code can be compiled with, and run on, older Java versions.</p>
 * <p>On Java versions that do not support records, {@link #isRecord(Class)}
 * always returns {@code false}.</p>
 */
final class RecordSupport
{
    private static final Method IS_RECORD;
    private static final Method GET_RECORD_COMPONENTS;
    private static final Method COMPONENT_GET_NAME;
    private static final Method COMPONENT_GET_TYPE;
    private static final Method COMPONENT_GET_ACCESSOR;

    static
    {
        Method isRecord = null;
        Method getRecordComponents = null;
        Method getName = null;
        Method getType = null;
        Method getAccessor = null;
        try
        {
            isRecord = Class.class.getMethod("isRecord");
            getRecordComponents = Class.class.getMethod("getRecordComponents");
            Class<?> recordComponentClass = Class.forName("java.lang.reflect.RecordComponent");
            getName = recordComponentClass.getMethod("getName");
            getType = recordComponentClass.getMethod("getType");
            getAccessor = recordComponentClass.getMethod("getAccessor");
        }
        catch (Throwable ignored)
        {
            // Records are available since Java 16.
            isRecord = null;
            getRecordComponents = null;
            getName = null;
            getType = null;
            getAccessor = null;
        }
        IS_RECORD = isRecord;
        GET_RECORD_COMPONENTS = getRecordComponents;
        COMPONENT_GET_NAME = getName;
        COMPONENT_GET_TYPE = getType;
        COMPONENT_GET_ACCESSOR = getAccessor;
    }

    private RecordSupport()
    {
    }

    /**
     * @param klass the class to test
     * @return whether the given class is a record class
     */
    static boolean isRecord(Class<?> klass)
    {
        if (IS_RECORD == null)
            return false;
        try
        {
            return (Boolean)IS_RECORD.invoke(klass);
        }
        catch (Throwable x)
        {
            return false;
        }
    }

    /**
     * @param klass a record class
     * @return the components of the given record class, in declaration order
     * @throws ReflectiveOperationException if the components cannot be retrieved
     */
    static List<Component> getRecordComponents(Class<?> klass) throws ReflectiveOperationException
    {
        if (GET_RECORD_COMPONENTS == null)
            throw new UnsupportedOperationException("Records are available starting from Java 16");
        Object[] components = (Object[])GET_RECORD_COMPONENTS.invoke(klass);
        List<Component> result = new ArrayList<>(components.length);
        for (Object component : components)
        {
            String name = (String)COMPONENT_GET_NAME.invoke(component);
            Class<?> type = (Class<?>)COMPONENT_GET_TYPE.invoke(component);
            Method accessor = (Method)COMPONENT_GET_ACCESSOR.invoke(component);
            result.add(new Component(name, type, accessor));
        }
        return result;
    }

    /**
     * A record component: its name, type and accessor method.
     */
    static final class Component
    {
        private final String name;
        private final Class<?> type;
        private final Method accessor;

        private Component(String name, Class<?> type, Method accessor)
        {
            this.name = name;
            this.type = type;
            this.accessor = accessor;
        }

        String getName()
        {
            return name;
        }

        Class<?> getType()
        {
            return type;
        }

        Method getAccessor()
        {
            return accessor;
        }
    }
}
