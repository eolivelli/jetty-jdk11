# Jetty 12.1.x → JDK 11 port — plan and status

Branch `jetty-12.1.x-jdk11` (from `jetty-12.1.x` @ bdc419f91bd). PR: https://github.com/eolivelli/jetty-jdk11/pull/1

This file is updated as the work progresses. Legend: ✅ done · 🔄 in progress · ⏳ not started · ⚠️ blocked / needs decision

## Scope

- Kept: `jetty-core`, `jetty-ee10`, `jetty-ee9`, `jetty-ee8` (generated from ee9), `jetty-integrations`, `jetty-demos`, `jetty-home`, `tests`.
- Dropped from the reactor (directories left in place): `jetty-ee11` (Jakarta EE 11 is Java 17 bytecode, no downgrade possible), `jetty-p2` (Tycho 4 needs JDK 17), `documentation`, `jetty-quic-quiche-foreign` (already gated to JDK 22+).
- Definition of done: full `mvn install` (main + test compilation of all kept modules) on JDK 11, jetty-core test suites green, EE/integration suites run per module with failures fixed where feasible, distribution smoke-tested on JDK 11.

## Steps

| # | Step | Status | Notes |
|---|------|--------|-------|
| 0 | Environment: Temurin 11.0.32 via sdkman, integration worktree, per-worktree Maven repos, helper scripts | ✅ | |
| 1 | Build plumbing: `release` 17→11, enforcer, drop ee11/p2/docs, dependency downgrades (JUnit 6→5.14.4, Testcontainers 2→1.21.4, Infinispan 16→14.0.35, Hibernate Search 8→6.1.5, Hazelcast 5.7→5.3.8, ECJ 3.46→3.33, modify-sources 1.0.14→1.0.13, jetty-build-support 1.6→1.5), remove mimir/njord/build-cache extensions | ✅ | commits e1cd4325a4f, b6dbd4c9435 |
| 2 | Automated desugaring tool (JavaParser based, outside the repo) | ✅ | text blocks, `String.formatted`, `Stream.toList`, `@Serial`, sealed, arrow switches, switch expressions, pattern `instanceof` on locals |
| 2a | Mechanical pass A applied (text blocks, formatted, toList, @Serial, sealed) — 493 files | ✅ | commit 50a7daf7971, verified: whole reactor compiles + checkstyle on JDK 17 |
| 2b | Mechanical pass B applied (switch, instanceof) — 401 files | ✅ | commit 443ead63c8b, verified: whole reactor compiles + checkstyle on JDK 17 |
| 3 | Hand-port of the core roots (util, io, http, server, client, xml, jmx, start, util-ajax): records → classes, sealed, reflective `UnixDomain` helper for Unix-Domain sockets, `BufferUtil.absoluteSlice/absolutePut`, `HexFormat`, `skipNBytes`, record reflection in JSON/AsyncJSON, static members in inner classes, `StringBuilder.isEmpty()`, generic `instanceof`, local interfaces | ✅ | commits 18badac0e8f, e1fc970e650, 6fa5be4be42 — 136 modules compile on JDK 11 |
| 3t | Core roots test suites on JDK 11 | ✅ | util 2951 tests (2 jrt: tests gated to JDK 12+), io, jmx, xml, util-ajax, slf4j, start 198 (3 network-dependent tests excluded: upstream snapshot no longer exists), http 4869, http-tools, server 2290 (only failures: DNS-hijack environment test; `HttpChannelTest` timing flakiness under `-T`/parallel load — passes 3/3 in isolation), client 780 (only failures: 4 DNS-hijack environment tests + `HttpClientTLSTest.testHostNameVerificationFailure`, which passes in isolation) |
| 4 | Wave 1 (parallel worktrees): hand-port remaining records / declined instanceof sites in jetty-core (http2, http3, quic, fcgi, websocket, session, security, deploy, rewrite, compression, …) | ✅ | branch `jdk11/w1-core` merged (47 files); verified by the agent with a `-source 11` compile of 74 modules and a javap API audit against JDK 11 |
| 4 | Wave 1: jetty-ee10, jetty-integrations, jetty-demos | ✅ | branch `jdk11/w1-ee10` merged (32 files); Infinispan 14 / Hazelcast 5.3 needed no further source changes |
| 4 | Wave 1: jetty-ee9 (+ generated ee8), tests | ✅ | branch `jdk11/w1-ee9` merged (21 files); verified by the agent with a true `--release 11` compile of 79 modules |
| 5 | JDK 11 compile loop over the whole reactor (categories only javac 11 detects: static members in inner classes, `ByteBuffer.slice(int,int)`, `isEmpty()` on StringBuilder, try-with-resources on `HttpExchange`, …) | ✅ | **all 364 kept modules compile (main + test) on JDK 11**, `mvn install -DskipTests` green, checkstyle 0 violations. plexus-xml 4.2.0 → 4.0.4 (Java 8), maven-resources-plugin 3.5.0 → 3.4.0 (needed for the `.mod` `@g:a@` token filtering of modify-sources 1.0.13) |
| 6 | jetty-core test suites (all modules) on JDK 11 | ✅ | server-stack batch (33 modules: session, security, deploy, rewrite, jndi, plus, annotations, osgi, keystore, unixdomain, http-spi, compression, maven, …): 764 tests, 0 failures. http2/fcgi/proxy/alpn batch: 538 tests; only failures: `ExternalFastCGIServerTest` (tagged `external`, needs a php-fcgi server), `ConcurrentRequestsTest`/`TrailersTest` (fail identically with the untouched upstream sources on JDK 17 on this machine → pre-existing), `ForwardProxyWithDynamicTransportTest.testProxyConcurrentLoad` (flaky under load). websocket/quic/http3/client-transports batch (30 modules): 1963 tests; 3 errors in `jetty-test-client-transports` under analysis (isolation rerun) |
| 7 | EE10 / EE9 / EE8 test suites on JDK 11 | 🔄 | EE10 (excl. Docker session stores/OSGi): 2428 tests, 5 timing/network-looking failures being rerun in isolation; EE9: 1027 tests so far, 1 failure (`RequestTest.testConnectionClose`) being rerun; EE8 pending |
| 8 | Integrations, OSGi, session (Docker) test suites | ⏳ | |
| 9 | jetty-home build, class-file major-version scan of `lib/` (must be ≤ 55), start.jar smoke test on JDK 11 (ee10/ee9/ee8 demos, JSP with ECJ 3.33) | ✅ | jetty-home builds; scan of `lib/` (198 jars): no class file newer than Java 11; `start.jar` on JDK 11.0.32 with `server,http,http2c,ee10/ee9/ee8-deploy` + simple/jsp/spec demos: all three environments deploy, `dump.jsp` renders (200) on ee10, ee9 and ee8, no errors in the log |
| 10 | test-distribution, maven-plugin ITs, checkstyle/spotless re-enabled for the final build | ⏳ | |

## Decisions / known deviations

- `Stream.toList()` was replaced by `collect(Collectors.toList())` (mutable list; `toUnmodifiableList()` rejects nulls, `Stream.toList()` does not).
- Record → class conversions keep the accessor names (`r.name()`), so callers are unchanged; `equals/hashCode/toString` follow the record semantics.
- Unix-Domain sockets (Java 16 API) are accessed reflectively via `org.eclipse.jetty.io.UnixDomain` (same approach as Jetty 11); on JDK 11 `Transport.TCPUnix/UDPUnix` and `UnixDomainServerConnector` throw `UnsupportedOperationException`, tests are gated with `@EnabledForJreRange(min = JAVA_16)`.
- JSON/AsyncJSON record support works at runtime on Java 16+ through a reflective helper; the record round-trip tests were removed (a Java 11 test source cannot declare a record).
- `jrt:/<module>` resources: JDK 11's jrt file system uses the older `/modules/<module>` layout, so `ResourceFactory.newResource("jrt:/java.base")` does not resolve on JDK 11; the two tests relying on it are disabled on JDK 11.
- `requireUpperBoundDeps` excludes `jakarta.transaction:jakarta.transaction-api` (Infinispan 14's Hot Rod client uses the javax-namespace 1.3.x artifact, EE9/EE10 the jakarta 2.x one).
- Multi-release jar env for zipfs: both `releaseVersion` (JDK 13+) and `multi-release` (JDK 11/12) keys are set.

## Tooling (outside the repo, `/home/eolivelli/dev/jetty-jdk11-tools/`)

`mvn11.sh` (Maven on JDK 11 with a per-worktree local repo), `desugar/` (JavaParser tool), `scan-major.sh` (class-file version scanner), `RECIPE.md` (hand-porting rules given to worker agents).
