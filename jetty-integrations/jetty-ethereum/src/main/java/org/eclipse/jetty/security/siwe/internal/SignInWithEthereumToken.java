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

package org.eclipse.jetty.security.siwe.internal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.siwe.EthereumAuthenticator;
import org.eclipse.jetty.util.StringUtil;

/**
 * Class representing a parsed SIWE message defined by <a href="https://eips.ethereum.org/EIPS/eip-4361">EIP4361</a>.
 */
public final class SignInWithEthereumToken
{
    private static final String SCHEME_PATTERN = "[a-zA-Z][a-zA-Z0-9+\\-.]*";
    private static final String DOMAIN_PATTERN = "(?:[a-zA-Z0-9\\-._~%]+@)?[a-zA-Z0-9\\-._~%]+(?:\\:[0-9]+)?";
    private static final String ADDRESS_PATTERN = "0x[0-9a-fA-F]{40}";
    private static final String STATEMENT_PATTERN = "[^\\n]*";
    private static final String URI_PATTERN = "[^\\n]+";
    private static final String VERSION_PATTERN = "[0-9]+";
    private static final String CHAIN_ID_PATTERN = "[0-9]+";
    private static final String NONCE_PATTERN = "[a-zA-Z0-9]{8}";
    private static final String DATE_TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?";
    private static final String REQUEST_ID_PATTERN = "[^\\n]*";
    private static final String RESOURCE_PATTERN = "- " + URI_PATTERN;
    private static final String RESOURCES_PATTERN = "(?:\n" + RESOURCE_PATTERN + ")*";
    private static final Pattern SIGN_IN_WITH_ETHEREUM_PATTERN = Pattern.compile(
        "^(?:(?<scheme>" + SCHEME_PATTERN + ")://)?(?<domain>" + DOMAIN_PATTERN + ") wants you to sign in with your Ethereum account:\n" +
            "(?<address>" + ADDRESS_PATTERN + ")\n\n" +
            "(?<statement>" + STATEMENT_PATTERN + ")?\n\n" +
            "URI: (?<uri>" + URI_PATTERN + ")\n" +
            "Version: (?<version>" + VERSION_PATTERN + ")\n" +
            "Chain ID: (?<chainId>" + CHAIN_ID_PATTERN + ")\n" +
            "Nonce: (?<nonce>" + NONCE_PATTERN + ")\n" +
            "Issued At: (?<issuedAt>" + DATE_TIME_PATTERN + ")" +
            "(?:\nExpiration Time: (?<expirationTime>" + DATE_TIME_PATTERN + "))?" +
            "(?:\nNot Before: (?<notBefore>" + DATE_TIME_PATTERN + "))?" +
            "(?:\nRequest ID: (?<requestId>" + REQUEST_ID_PATTERN + "))?" +
            "(?:\nResources:(?<resources>" + RESOURCES_PATTERN + "))?$",
        Pattern.DOTALL
    );

    private final String scheme;
    private final String domain;
    private final String address;
    private final String statement;
    private final String uri;
    private final String version;
    private final String chainId;
    private final String nonce;
    private final String issuedAt;
    private final String expirationTime;
    private final String notBefore;
    private final String requestId;
    private final String resources;

    /**
     * @param scheme the URI scheme of the origin of the request.
     * @param domain the domain that is requesting the signing.
     * @param address the Ethereum address performing the signing.
     * @param statement a human-readable ASCII assertion that the user will sign.
     * @param uri an RFC 3986 URI referring to the resource that is the subject of the signing.
     * @param version the version of the SIWE Message.
     * @param chainId the Chain ID to which the session is bound.
     * @param nonce a random string used to prevent replay attacks.
     * @param issuedAt time when the message was generated.
     * @param expirationTime time when the signed authentication message is no longer valid.
     * @param notBefore time when the signed authentication message will become valid.
     * @param requestId a system-specific request identifier.
     * @param resources list of resources the user wishes to have resolved as part of authentication.
     */
    public SignInWithEthereumToken(String scheme,
                                   String domain,
                                   String address,
                                   String statement,
                                   String uri,
                                   String version,
                                   String chainId,
                                   String nonce,
                                   String issuedAt,
                                   String expirationTime,
                                   String notBefore,
                                   String requestId,
                                   String resources)
    {
        this.scheme = scheme;
        this.domain = domain;
        this.address = address;
        this.statement = statement;
        this.uri = uri;
        this.version = version;
        this.chainId = chainId;
        this.nonce = nonce;
        this.issuedAt = issuedAt;
        this.expirationTime = expirationTime;
        this.notBefore = notBefore;
        this.requestId = requestId;
        this.resources = resources;
    }

    public String scheme()
    {
        return scheme;
    }

    public String domain()
    {
        return domain;
    }

    public String address()
    {
        return address;
    }

    public String statement()
    {
        return statement;
    }

    public String uri()
    {
        return uri;
    }

    public String version()
    {
        return version;
    }

    public String chainId()
    {
        return chainId;
    }

    public String nonce()
    {
        return nonce;
    }

    public String issuedAt()
    {
        return issuedAt;
    }

    public String expirationTime()
    {
        return expirationTime;
    }

    public String notBefore()
    {
        return notBefore;
    }

    public String requestId()
    {
        return requestId;
    }

    public String resources()
    {
        return resources;
    }

    /**
     * Parses a SIWE Message into a {@link SignInWithEthereumToken},
     * based off the ABNF Message Format from <a href="https://eips.ethereum.org/EIPS/eip-4361">EIP-4361</a>.
     * @param message the SIWE message to parse.
     * @return the {@link SignInWithEthereumToken} or null if it was not a valid SIWE message.
     */
    public static SignInWithEthereumToken from(String message)
    {
        Matcher matcher = SIGN_IN_WITH_ETHEREUM_PATTERN.matcher(message);
        if (!matcher.matches())
            return null;

        return new SignInWithEthereumToken(matcher.group("scheme"), matcher.group("domain"),
            matcher.group("address"), matcher.group("statement"), matcher.group("uri"),
            matcher.group("version"), matcher.group("chainId"), matcher.group("nonce"),
            matcher.group("issuedAt"), matcher.group("expirationTime"), matcher.group("notBefore"),
            matcher.group("requestId"), matcher.group("resources"));
    }

    /**
     * @param signedMessage the {@link EthereumAuthenticator.SignedMessage}.
     * @param validateNonce a {@link Predicate} used to validate the nonce.
     * @param validateDomain a {@link Predicate} used to validate the domain.
     * @param validateChainId a {@link Predicate} used to validate the chainId.
     * @throws ServerAuthException if the {@link EthereumAuthenticator.SignedMessage} fails validation.
     */
    public void validate(EthereumAuthenticator.SignedMessage signedMessage,
                         Predicate<String> validateNonce,
                         Predicate<String> validateDomain,
                         Predicate<String> validateChainId) throws ServerAuthException
    {
        if (validateNonce != null && !validateNonce.test(nonce()))
            throw new ServerAuthException("invalid nonce " + nonce);

        if (!StringUtil.asciiEqualsIgnoreCase(signedMessage.recoverAddress(), address()))
            throw new ServerAuthException("signature verification failed");

        if (!"1".equals(version()))
            throw new ServerAuthException("unsupported version " + version);

        LocalDateTime now = LocalDateTime.now();
        if (StringUtil.isNotBlank(expirationTime()))
        {
            LocalDateTime expirationTime = LocalDateTime.parse(expirationTime(), DateTimeFormatter.ISO_DATE_TIME);
            if (now.isAfter(expirationTime))
                throw new ServerAuthException("expired SIWE message");
        }

        if (StringUtil.isNotBlank(notBefore()))
        {
            LocalDateTime notBefore = LocalDateTime.parse(notBefore(), DateTimeFormatter.ISO_DATE_TIME);
            if (now.isBefore(notBefore))
                throw new ServerAuthException("SIWE message not yet valid");
        }

        if (validateDomain != null && !validateDomain.test(domain()))
            throw new ServerAuthException("unregistered domain: " + domain());
        if (validateChainId != null && !validateChainId.test(chainId()))
            throw new ServerAuthException("unregistered chainId: " + chainId());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SignInWithEthereumToken that = (SignInWithEthereumToken)obj;
        return Objects.equals(scheme, that.scheme) &&
            Objects.equals(domain, that.domain) &&
            Objects.equals(address, that.address) &&
            Objects.equals(statement, that.statement) &&
            Objects.equals(uri, that.uri) &&
            Objects.equals(version, that.version) &&
            Objects.equals(chainId, that.chainId) &&
            Objects.equals(nonce, that.nonce) &&
            Objects.equals(issuedAt, that.issuedAt) &&
            Objects.equals(expirationTime, that.expirationTime) &&
            Objects.equals(notBefore, that.notBefore) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(resources, that.resources);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(scheme, domain, address, statement, uri, version, chainId, nonce, issuedAt, expirationTime, notBefore, requestId, resources);
    }

    @Override
    public String toString()
    {
        return "SignInWithEthereumToken[" +
            "scheme=" + scheme +
            ", domain=" + domain +
            ", address=" + address +
            ", statement=" + statement +
            ", uri=" + uri +
            ", version=" + version +
            ", chainId=" + chainId +
            ", nonce=" + nonce +
            ", issuedAt=" + issuedAt +
            ", expirationTime=" + expirationTime +
            ", notBefore=" + notBefore +
            ", requestId=" + requestId +
            ", resources=" + resources +
            "]";
    }
}
