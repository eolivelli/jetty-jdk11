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

package org.eclipse.jetty.io;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

import org.eclipse.jetty.util.JavaVersion;

/**
 * <p>Reflective access to the Unix-Domain socket APIs introduced in Java 16
 * ({@code java.net.UnixDomainSocketAddress}, {@code StandardProtocolFamily.UNIX}),
 * so that this code can be compiled with, and run on, older Java versions.</p>
 * <p>On Java versions that do not support Unix-Domain sockets, {@link #isSupported()}
 * returns {@code false} and the other methods throw {@link UnsupportedOperationException}.</p>
 */
public final class UnixDomain
{
    private static final ProtocolFamily UNIX_FAMILY;
    private static final Class<?> ADDRESS_CLASS;
    private static final MethodHandle ADDRESS_OF;
    private static final MethodHandle ADDRESS_GET_PATH;
    private static final MethodHandle SOCKET_CHANNEL_OPEN;
    private static final MethodHandle SERVER_SOCKET_CHANNEL_OPEN;
    private static final MethodHandle DATAGRAM_CHANNEL_OPEN;

    static
    {
        ProtocolFamily family = null;
        Class<?> addressClass = null;
        MethodHandle addressOf = null;
        MethodHandle addressGetPath = null;
        MethodHandle socketChannelOpen = null;
        MethodHandle serverSocketChannelOpen = null;
        MethodHandle datagramChannelOpen = null;
        try
        {
            family = Enum.valueOf(StandardProtocolFamily.class, "UNIX");
            addressClass = Class.forName("java.net.UnixDomainSocketAddress");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            addressOf = lookup.findStatic(addressClass, "of", MethodType.methodType(addressClass, Path.class))
                .asType(MethodType.methodType(SocketAddress.class, Path.class));
            addressGetPath = lookup.findVirtual(addressClass, "getPath", MethodType.methodType(Path.class))
                .asType(MethodType.methodType(Path.class, SocketAddress.class));
            socketChannelOpen = lookup.findStatic(SocketChannel.class, "open", MethodType.methodType(SocketChannel.class, ProtocolFamily.class));
            serverSocketChannelOpen = lookup.findStatic(ServerSocketChannel.class, "open", MethodType.methodType(ServerSocketChannel.class, ProtocolFamily.class));
            datagramChannelOpen = lookup.findStatic(DatagramChannel.class, "open", MethodType.methodType(DatagramChannel.class, ProtocolFamily.class));
        }
        catch (Throwable ignored)
        {
            // Unix-Domain sockets are available since Java 16.
            family = null;
            addressClass = null;
            addressOf = null;
            addressGetPath = null;
            socketChannelOpen = null;
            serverSocketChannelOpen = null;
            datagramChannelOpen = null;
        }
        UNIX_FAMILY = family;
        ADDRESS_CLASS = addressClass;
        ADDRESS_OF = addressOf;
        ADDRESS_GET_PATH = addressGetPath;
        SOCKET_CHANNEL_OPEN = socketChannelOpen;
        SERVER_SOCKET_CHANNEL_OPEN = serverSocketChannelOpen;
        DATAGRAM_CHANNEL_OPEN = datagramChannelOpen;
    }

    private UnixDomain()
    {
    }

    /**
     * @return whether Unix-Domain sockets are supported by the current Java runtime
     */
    public static boolean isSupported()
    {
        return ADDRESS_OF != null;
    }

    /**
     * @param path the Unix-Domain path
     * @return a Unix-Domain {@link SocketAddress} for the given path
     * @throws UnsupportedOperationException if Unix-Domain sockets are not supported
     */
    public static SocketAddress addressOf(Path path)
    {
        ensureSupported();
        try
        {
            return (SocketAddress)ADDRESS_OF.invokeExact(path);
        }
        catch (RuntimeException | Error x)
        {
            throw x;
        }
        catch (Throwable x)
        {
            throw new IllegalArgumentException(x);
        }
    }

    /**
     * @param address a {@link SocketAddress}
     * @return whether the given address is a Unix-Domain address
     */
    public static boolean isUnixDomainAddress(SocketAddress address)
    {
        return ADDRESS_CLASS != null && ADDRESS_CLASS.isInstance(address);
    }

    /**
     * @param address a Unix-Domain {@link SocketAddress}
     * @return the path of the given Unix-Domain address
     * @throws IllegalArgumentException if the address is not a Unix-Domain address
     */
    public static Path getPath(SocketAddress address)
    {
        if (!isUnixDomainAddress(address))
            throw new IllegalArgumentException("Not a Unix-Domain address: " + address);
        try
        {
            return (Path)ADDRESS_GET_PATH.invokeExact(address);
        }
        catch (RuntimeException | Error x)
        {
            throw x;
        }
        catch (Throwable x)
        {
            throw new IllegalStateException(x);
        }
    }

    /**
     * @return a new Unix-Domain {@link SocketChannel}
     * @throws IOException if the channel cannot be opened
     * @throws UnsupportedOperationException if Unix-Domain sockets are not supported
     */
    public static SocketChannel openSocketChannel() throws IOException
    {
        ensureSupported();
        try
        {
            return (SocketChannel)SOCKET_CHANNEL_OPEN.invokeExact(UNIX_FAMILY);
        }
        catch (IOException | RuntimeException | Error x)
        {
            throw x;
        }
        catch (Throwable x)
        {
            throw new IOException(x);
        }
    }

    /**
     * @return a new Unix-Domain {@link ServerSocketChannel}
     * @throws IOException if the channel cannot be opened
     * @throws UnsupportedOperationException if Unix-Domain sockets are not supported
     */
    public static ServerSocketChannel openServerSocketChannel() throws IOException
    {
        ensureSupported();
        try
        {
            return (ServerSocketChannel)SERVER_SOCKET_CHANNEL_OPEN.invokeExact(UNIX_FAMILY);
        }
        catch (IOException | RuntimeException | Error x)
        {
            throw x;
        }
        catch (Throwable x)
        {
            throw new IOException(x);
        }
    }

    /**
     * @return a new Unix-Domain {@link DatagramChannel}
     * @throws IOException if the channel cannot be opened
     * @throws UnsupportedOperationException if Unix-Domain sockets are not supported
     */
    public static DatagramChannel openDatagramChannel() throws IOException
    {
        ensureSupported();
        try
        {
            return (DatagramChannel)DATAGRAM_CHANNEL_OPEN.invokeExact(UNIX_FAMILY);
        }
        catch (IOException | RuntimeException | Error x)
        {
            throw x;
        }
        catch (Throwable x)
        {
            throw new IOException(x);
        }
    }

    private static void ensureSupported()
    {
        if (!isSupported())
            throw new UnsupportedOperationException("Unix-Domain sockets are available starting from Java 16, your Java version is: " + JavaVersion.VERSION);
    }
}
