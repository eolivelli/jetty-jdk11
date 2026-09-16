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

package org.eclipse.jetty.ee10.servlet.internal;

import java.util.Objects;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.WebConnection;
import org.eclipse.jetty.util.IO;

public final class JettyWebConnection implements WebConnection
{
    private final ServletInputStream inputStream;
    private final ServletOutputStream outputStream;

    public JettyWebConnection(ServletInputStream inputStream, ServletOutputStream outputStream)
    {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public ServletInputStream inputStream()
    {
        return inputStream;
    }

    public ServletOutputStream outputStream()
    {
        return outputStream;
    }

    @Override
    public void close()
    {
        IO.close(inputStream);
        IO.close(outputStream);
    }

    @Override
    public ServletInputStream getInputStream()
    {
        return inputStream;
    }

    @Override
    public ServletOutputStream getOutputStream()
    {
        return outputStream;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        JettyWebConnection that = (JettyWebConnection)obj;
        return Objects.equals(inputStream, that.inputStream) &&
            Objects.equals(outputStream, that.outputStream);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(inputStream, outputStream);
    }

    @Override
    public String toString()
    {
        return "JettyWebConnection[inputStream=" + inputStream + ", outputStream=" + outputStream + "]";
    }
}
