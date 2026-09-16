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

package org.eclipse.jetty.quic.quiche;

import java.nio.file.Path;
import java.util.Objects;

public final class PemPaths
{
    private final Path privateKeyPemPath;
    private final Path certificateChainPemPath;
    private final Path trustedCertificatesPemPath;

    public PemPaths(Path privateKeyPemPath, Path certificateChainPemPath, Path trustedCertificatesPemPath)
    {
        this.privateKeyPemPath = privateKeyPemPath;
        this.certificateChainPemPath = certificateChainPemPath;
        this.trustedCertificatesPemPath = trustedCertificatesPemPath;
    }

    public Path privateKeyPemPath()
    {
        return privateKeyPemPath;
    }

    public Path certificateChainPemPath()
    {
        return certificateChainPemPath;
    }

    public Path trustedCertificatesPemPath()
    {
        return trustedCertificatesPemPath;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        PemPaths that = (PemPaths)obj;
        return Objects.equals(privateKeyPemPath, that.privateKeyPemPath) && Objects.equals(certificateChainPemPath, that.certificateChainPemPath) &&
            Objects.equals(trustedCertificatesPemPath, that.trustedCertificatesPemPath);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(privateKeyPemPath, certificateChainPemPath, trustedCertificatesPemPath);
    }

    @Override
    public String toString()
    {
        return "PemPaths[privateKeyPemPath=" + privateKeyPemPath + ", certificateChainPemPath=" + certificateChainPemPath +
            ", trustedCertificatesPemPath=" + trustedCertificatesPemPath + "]";
    }
}
