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

package org.eclipse.jetty.websocket.api;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An immutable websocket frame.
 */
public interface Frame
{
    enum Type
    {
        CONTINUATION((byte)0x00),
        TEXT((byte)0x01),
        BINARY((byte)0x02),
        CLOSE((byte)0x08),
        PING((byte)0x09),
        PONG((byte)0x0A);

        public static Type from(byte op)
        {
            for (Type type : values())
            {
                if (type.opcode == op)
                {
                    return type;
                }
            }
            throw new IllegalArgumentException("OpCode " + op + " is not a valid Frame.Type");
        }

        private final byte opcode;

        Type(byte code)
        {
            this.opcode = code;
        }

        public byte getOpCode()
        {
            return opcode;
        }

        public boolean isControl()
        {
            return (opcode >= CLOSE.getOpCode());
        }

        public boolean isData()
        {
            return (opcode == TEXT.getOpCode()) || (opcode == BINARY.getOpCode());
        }

        public boolean isContinuation()
        {
            return opcode == CONTINUATION.getOpCode();
        }

        @Override
        public String toString()
        {
            return this.name();
        }
    }

    byte[] getMask();

    byte getOpCode();

    ByteBuffer getPayload();

    /**
     * The original payload length ({@link ByteBuffer#remaining()})
     *
     * @return the original payload length ({@link ByteBuffer#remaining()})
     */
    int getPayloadLength();

    Type getType();

    boolean hasPayload();

    boolean isFin();

    boolean isMasked();

    boolean isRsv1();

    boolean isRsv2();

    boolean isRsv3();

    default CloseStatus getCloseStatus()
    {
        return null;
    }

    final class CloseStatus
    {
        private final int statusCode;
        private final String reason;

        public CloseStatus(int statusCode, String reason)
        {
            this.statusCode = statusCode;
            this.reason = reason;
        }

        public int statusCode()
        {
            return statusCode;
        }

        public String reason()
        {
            return reason;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            CloseStatus that = (CloseStatus)obj;
            return statusCode == that.statusCode && Objects.equals(reason, that.reason);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(statusCode, reason);
        }

        @Override
        public String toString()
        {
            return "CloseStatus[statusCode=" + statusCode + ", reason=" + reason + "]";
        }
    }

    /**
     * The effective opcode of the frame accounting for the CONTINUATION opcode.
     * If the frame is a CONTINUATION frame for a TEXT message, this will return TEXT.
     * If the frame is a CONTINUATION frame for a BINARY message, this will return BINARY.
     * Otherwise, this will return the same opcode as the frame.
     * @return the effective opcode of the frame.
     */
    byte getEffectiveOpCode();
}
