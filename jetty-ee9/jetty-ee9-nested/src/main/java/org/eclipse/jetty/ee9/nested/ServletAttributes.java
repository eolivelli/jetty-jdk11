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

package org.eclipse.jetty.ee9.nested;

import java.util.Objects;
import java.util.Set;

import jakarta.servlet.AsyncContext;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Attributes;

/**
 * An implementation of Attributes that supports the standard SSL and async attributes.
 */
public class ServletAttributes extends Attributes.Synthetic
{
    private static final Set<String> ATTRIBUTES =
        Set.of(
            Request.SSL_CIPHER_SUITE,
            Request.SSL_KEY_SIZE,
            Request.SSL_SESSION_ID,
            Request.PEER_CERTIFICATES,
            AsyncContext.ASYNC_REQUEST_URI,
            AsyncContext.ASYNC_CONTEXT_PATH,
            AsyncContext.ASYNC_SERVLET_PATH,
            AsyncContext.ASYNC_PATH_INFO,
            AsyncContext.ASYNC_QUERY_STRING,
            AsyncContext.ASYNC_MAPPING
        );

    private static final class Async
    {
        private final String requestURI;
        private final String contextPath;
        private final String pathInContext;
        private final ServletPathMapping mapping;
        private final String queryString;

        private Async(
            String requestURI,
            String contextPath,
            String pathInContext,
            ServletPathMapping mapping,
            String queryString)
        {
            this.requestURI = requestURI;
            this.contextPath = contextPath;
            this.pathInContext = pathInContext;
            this.mapping = mapping;
            this.queryString = queryString;
        }

        public String requestURI()
        {
            return requestURI;
        }

        public String contextPath()
        {
            return contextPath;
        }

        public String pathInContext()
        {
            return pathInContext;
        }

        public ServletPathMapping mapping()
        {
            return mapping;
        }

        public String queryString()
        {
            return queryString;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Async that = (Async)obj;
            return Objects.equals(requestURI, that.requestURI) &&
                Objects.equals(contextPath, that.contextPath) &&
                Objects.equals(pathInContext, that.pathInContext) &&
                Objects.equals(mapping, that.mapping) &&
                Objects.equals(queryString, that.queryString);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(requestURI, contextPath, pathInContext, mapping, queryString);
        }

        @Override
        public String toString()
        {
            return "Async[requestURI=" + requestURI + ", contextPath=" + contextPath + ", pathInContext=" + pathInContext +
                ", mapping=" + mapping + ", queryString=" + queryString + "]";
        }
    }

    private Async _async;

    ServletAttributes(Attributes attributes)
    {
        super(attributes);
    }

    @Override
    protected Object getSyntheticAttribute(String name)
    {
        switch (name)
        {
            case Request.SSL_CIPHER_SUITE:
            {
                Object attribute = getWrapped().getAttribute(EndPoint.SslSessionData.ATTRIBUTE);
                return attribute instanceof EndPoint.SslSessionData ? ((EndPoint.SslSessionData)attribute).cipherSuite() : null;
            }
            case Request.SSL_KEY_SIZE:
            {
                Object attribute = getWrapped().getAttribute(EndPoint.SslSessionData.ATTRIBUTE);
                return attribute instanceof EndPoint.SslSessionData ? ((EndPoint.SslSessionData)attribute).keySize() : null;
            }
            case Request.SSL_SESSION_ID:
            {
                Object attribute = getWrapped().getAttribute(EndPoint.SslSessionData.ATTRIBUTE);
                return attribute instanceof EndPoint.SslSessionData ? ((EndPoint.SslSessionData)attribute).sslSessionId() : null;
            }
            case Request.PEER_CERTIFICATES:
            {
                Object attribute = getWrapped().getAttribute(EndPoint.SslSessionData.ATTRIBUTE);
                return attribute instanceof EndPoint.SslSessionData ? ((EndPoint.SslSessionData)attribute).peerCertificates() : null;
            }
            case AsyncContext.ASYNC_REQUEST_URI:
                return _async == null ? null : _async.requestURI;
            case AsyncContext.ASYNC_CONTEXT_PATH:
                return _async == null ? null : _async.contextPath;
            case AsyncContext.ASYNC_SERVLET_PATH:
                return _async == null ? null : _async.mapping == null ? null : _async.mapping.getServletPath();
            case AsyncContext.ASYNC_PATH_INFO:
                return _async == null ? null : _async.mapping == null ? _async.pathInContext : _async.mapping.getPathInfo();
            case AsyncContext.ASYNC_QUERY_STRING:
                return _async == null ? null : _async.queryString;
            case AsyncContext.ASYNC_MAPPING:
                return _async == null ? null : _async.mapping;
            default:
                return null;
        }
    }

    @Override
    protected Set<String> getSyntheticNameSet()
    {
        return ATTRIBUTES;
    }

    public void setAsyncAttributes(String requestURI, String contextPath, String pathInContext, ServletPathMapping servletPathMapping, String queryString)
    {
        _async = new Async(requestURI, contextPath, pathInContext, servletPathMapping, queryString);
    }
}
