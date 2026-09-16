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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * <p>Tests for the reflective {@link RecordSupport} helper and for the record
 * support of {@link JSON} and {@link AsyncJSON}.</p>
 * <p>Records cannot be declared in sources compiled for Java 11, so the record
 * used by the tests is compiled at runtime and only on Java 16 or later.</p>
 */
public class RecordSupportTest
{
    private static final String PERSON_CLASS_NAME = "org.eclipse.jetty.util.ajax.records.Person";

    @TempDir
    public Path workDir;

    @Test
    public void testIsRecordOfNonRecordClass()
    {
        // Must work on every Java version, without needing getRecordComponents().
        assertFalse(RecordSupport.isRecord(String.class));
        assertFalse(RecordSupport.isRecord(Object.class));
        assertFalse(RecordSupport.isRecord(int.class));
        assertFalse(RecordSupport.isRecord(String[].class));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_16)
    public void testRecordComponents() throws Exception
    {
        try (URLClassLoader loader = compilePersonRecord())
        {
            Class<?> personClass = loader.loadClass(PERSON_CLASS_NAME);
            assertTrue(RecordSupport.isRecord(personClass));

            List<RecordSupport.Component> components = RecordSupport.getRecordComponents(personClass);
            assertThat(components.size(), is(2));
            assertThat(components.get(0).getName(), is("name"));
            assertThat(components.get(0).getType(), is(String.class));
            assertThat(components.get(1).getName(), is("age"));
            assertThat(components.get(1).getType(), is(Integer.TYPE));

            Object person = newPerson(personClass, "Jetty", 30);
            assertThat(components.get(0).getAccessor().invoke(person), is("Jetty"));
            assertThat(components.get(1).getAccessor().invoke(person), is(30));
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_16)
    public void testGenerateParseRecord() throws Exception
    {
        try (URLClassLoader loader = compilePersonRecord())
        {
            Class<?> personClass = loader.loadClass(PERSON_CLASS_NAME);

            // No configuration necessary for records.
            JSON json = new JSON();
            Object original = newPerson(personClass, "Jetty", 30);
            String jsonString = json.toJSON(original);
            assertThat(jsonString, is("{\"class\":\"" + PERSON_CLASS_NAME + "\",\"name\":\"Jetty\",\"age\":30}"));

            // JSON resolves the "class" field via the thread context ClassLoader.
            Object object = parseWithContextClassLoader(loader, () -> json.parse(new JSON.StringSource(jsonString)));
            assertInstanceOf(personClass, object);
            assertThat(object, is(original));

            // Test null values.
            Object originalWithNull = newPerson(personClass, null, 30);
            String jsonStringWithNull = json.toJSON(originalWithNull);

            object = parseWithContextClassLoader(loader, () -> json.parse(new JSON.StringSource(jsonStringWithNull)));
            assertInstanceOf(personClass, object);
            assertThat(object, is(originalWithNull));
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_16)
    public void testParseRecord() throws Exception
    {
        try (URLClassLoader loader = compilePersonRecord())
        {
            Class<?> personClass = loader.loadClass(PERSON_CLASS_NAME);

            // No configuration necessary for records.
            AsyncJSON parser = new AsyncJSON.Factory().newAsyncJSON();
            String name = "Jetty";
            int age = 30;
            ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(String.format("{\n" +
                "  \"class\": \"%s\",\n" +
                "  \"name\": \"%s\",\n" +
                "  \"age\": %d\n" +
                "}\n", PERSON_CLASS_NAME, name, age));

            // AsyncJSON resolves the "class" field via the thread context ClassLoader.
            ByteBuffer buffer1 = byteBuffer;
            Object object = parseWithContextClassLoader(loader, () ->
            {
                assertTrue(parser.parse(buffer1));
                return parser.complete();
            });
            assertInstanceOf(personClass, object);
            assertThat(personClass.getMethod("name").invoke(object), is(name));
            assertThat(personClass.getMethod("age").invoke(object), is(age));

            // Test null values.
            byteBuffer = StandardCharsets.UTF_8.encode(String.format("{\n" +
                "  \"class\": \"%s\",\n" +
                "  \"name\": null,\n" +
                "  \"age\": %d\n" +
                "}\n", PERSON_CLASS_NAME, age));

            ByteBuffer buffer2 = byteBuffer;
            object = parseWithContextClassLoader(loader, () ->
            {
                assertTrue(parser.parse(buffer2));
                return parser.complete();
            });
            assertInstanceOf(personClass, object);
            assertNull(personClass.getMethod("name").invoke(object));
            assertThat(personClass.getMethod("age").invoke(object), is(age));
        }
    }

    private static Object newPerson(Class<?> personClass, String name, int age) throws Exception
    {
        Constructor<?> constructor = personClass.getConstructor(String.class, Integer.TYPE);
        return constructor.newInstance(name, age);
    }

    private static <T> T parseWithContextClassLoader(ClassLoader loader, Supplier<T> parse)
    {
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try
        {
            return parse.get();
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * <p>Compiles {@code public record Person(String name, int age) {}} into a temporary
     * directory and returns a ClassLoader that can load it.</p>
     *
     * @return a ClassLoader for the compiled record class
     */
    private URLClassLoader compilePersonRecord() throws Exception
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "No system Java compiler available");

        Path sourceDir = Files.createDirectories(workDir.resolve("src"));
        Path source = sourceDir.resolve("Person.java");
        Files.write(source, Arrays.asList(
            "package org.eclipse.jetty.util.ajax.records;",
            "",
            "public record Person(String name, int age)",
            "{",
            "}"), StandardCharsets.UTF_8);

        Path classesDir = Files.createDirectories(workDir.resolve("classes"));
        int result = compiler.run(null, null, null, "--release", "16", "-d", classesDir.toString(), source.toString());
        assertThat("compilation of " + source + " failed", result, is(0));
        assertThat(Files.exists(classesDir.resolve("org/eclipse/jetty/util/ajax/records/Person.class")), is(true));

        return new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, RecordSupportTest.class.getClassLoader());
    }
}
