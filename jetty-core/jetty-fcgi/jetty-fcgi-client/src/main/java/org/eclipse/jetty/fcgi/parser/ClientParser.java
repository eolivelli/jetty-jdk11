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

package org.eclipse.jetty.fcgi.parser;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;

import org.eclipse.jetty.fcgi.FCGI;
import org.eclipse.jetty.http.HttpField;

public class ClientParser extends Parser
{
    private final EnumMap<FCGI.FrameType, ContentParser> contentParsers = new EnumMap<>(FCGI.FrameType.class);

    public ClientParser(Listener listener)
    {
        super(listener);
        ResponseContentParser stdOutParser = new ResponseContentParser(headerParser, listener);
        contentParsers.put(FCGI.FrameType.STDOUT, stdOutParser);
        StreamContentParser stdErrParser = new StreamContentParser(headerParser, FCGI.StreamType.STD_ERR, listener);
        contentParsers.put(FCGI.FrameType.STDERR, stdErrParser);
        contentParsers.put(FCGI.FrameType.END_REQUEST, new EndRequestContentParser(headerParser, new EndRequestListener(listener, stdOutParser, stdErrParser)));
    }

    @Override
    protected ContentParser findContentParser(FCGI.FrameType frameType)
    {
        ContentParser contentParser = contentParsers.get(frameType);
        if (contentParser == null)
            throw new IllegalArgumentException("unsupported frame type " + frameType);
        return contentParser;
    }

    public interface Listener extends Parser.Listener
    {
        public default void onBegin(int request, int code, String reason)
        {
        }
    }

    private static final class EndRequestListener implements Listener
    {
        private final Listener listener;
        private final StreamContentParser[] streamParsers;

        private EndRequestListener(Listener listener, StreamContentParser... streamParsers)
        {
            this.listener = listener;
            this.streamParsers = streamParsers;
        }

        public Listener listener()
        {
            return listener;
        }

        public StreamContentParser[] streamParsers()
        {
            return streamParsers;
        }

        @Override
        public void onBegin(int request, int code, String reason)
        {
            listener.onBegin(request, code, reason);
        }

        @Override
        public void onHeader(int request, HttpField field)
        {
            listener.onHeader(request, field);
        }

        @Override
        public boolean onHeaders(int request)
        {
            return listener.onHeaders(request);
        }

        @Override
        public boolean onContent(int request, FCGI.StreamType stream, ByteBuffer buffer)
        {
            return listener.onContent(request, stream, buffer);
        }

        @Override
        public boolean onEnd(int request)
        {
            boolean result = listener.onEnd(request);
            for (StreamContentParser streamParser : streamParsers)
            {
                streamParser.end(request);
            }
            return result;
        }

        @Override
        public void onFailure(int request, Throwable failure)
        {
            listener.onFailure(request, failure);
            for (StreamContentParser streamParser : streamParsers)
            {
                streamParser.end(request);
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            EndRequestListener that = (EndRequestListener)obj;
            return Objects.equals(listener, that.listener) && Arrays.equals(streamParsers, that.streamParsers);
        }

        @Override
        public int hashCode()
        {
            return 31 * Objects.hash(listener) + Arrays.hashCode(streamParsers);
        }

        @Override
        public String toString()
        {
            return "EndRequestListener[listener=" + listener + ", streamParsers=" + Arrays.toString(streamParsers) + "]";
        }
    }
}
