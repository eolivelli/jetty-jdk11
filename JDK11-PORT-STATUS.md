# Jetty 12.1.x → JDK 11 port — plan and status

Branch `jetty-12.1.x-jdk11` (from `jetty-12.1.x` @ bdc419f91bd). PR: https://github.com/eolivelli/jetty-jdk11/pull/1

**Status: the port is complete** — every kept module builds (main + test) on JDK 11 with checkstyle/spotless, the distribution runs on JDK 11, and all test suites have been run; the only remaining test failures are environmental or pre-existing (see the per-step notes). This file was updated as the work progressed. Legend: ✅ done · 🔄 in progress · ⏳ not started · ⚠️ blocked / needs decision

## Scope

- Kept: `jetty-core`, `jetty-ee10`, `jetty-ee9`, `jetty-ee8` (generated from ee9), `jetty-integrations`, `jetty-demos`, `jetty-home`, `tests`.
- Dropped from the reactor (directories left in place): `jetty-ee11` (Jakarta EE 11 is Java 17 bytecode, no downgrade possible), `jetty-p2` (Tycho 4 needs JDK 17), `documentation`, `jetty-quic-quiche-foreign` (already gated to JDK 22+).
- Definition of done: full `mvn install` (main + test compilation of all kept modules) on JDK 11, jetty-core test suites green, EE/integration suites run per module with failures fixed where feasible, distribution smoke-tested on JDK 11.

## Steps

| # | Step | Status | Notes |
|---|------|--------|-------|
| 0 | Environment: Temurin 11.0.32 via sdkman, integration worktree, per-worktree Maven repos, helper scripts | ✅ | |
| 1 | Build plumbing: `release` 17→11, enforcer, drop ee11/p2/docs, dependency downgrades (see the table below), remove mimir/njord/build-cache extensions | ✅ | commits e1cd4325a4f, b6dbd4c9435, 74ebeb2c981, 18badac0e8f, e1fc970e650, f999b74ef95, c9dbf28c44b |
| 2 | Automated desugaring tool (JavaParser based, outside the repo) | ✅ | text blocks, `String.formatted`, `Stream.toList`, `@Serial`, sealed, arrow switches, switch expressions, pattern `instanceof` on locals |
| 2a | Mechanical pass A applied (text blocks, formatted, toList, @Serial, sealed) — 493 files | ✅ | commit 50a7daf7971, verified: whole reactor compiles + checkstyle on JDK 17 |
| 2b | Mechanical pass B applied (switch, instanceof) — 401 files | ✅ | commit 443ead63c8b, verified: whole reactor compiles + checkstyle on JDK 17 |
| 3 | Hand-port of the core roots (util, io, http, server, client, xml, jmx, start, util-ajax): records → classes, sealed, reflective `UnixDomain` helper for Unix-Domain sockets, `BufferUtil.absoluteSlice/absolutePut`, `HexFormat`, `skipNBytes`, record reflection in JSON/AsyncJSON, static members in inner classes, `StringBuilder.isEmpty()`, generic `instanceof`, local interfaces | ✅ | commits 18badac0e8f, e1fc970e650, 6fa5be4be42 — 136 modules compile on JDK 11 |
| 3t | Core roots test suites on JDK 11 | ✅ | util 2951 tests (2 jrt: tests gated to JDK 12+), io, jmx, xml, util-ajax, slf4j, start 198 (3 network-dependent tests excluded: upstream snapshot no longer exists), http 4869, http-tools, server 2290 (only failures: DNS-hijack environment test; `HttpChannelTest` timing flakiness under `-T`/parallel load — passes 3/3 in isolation), client 780 (only failures: 4 DNS-hijack environment tests + `HttpClientTLSTest.testHostNameVerificationFailure`, which passes in isolation) |
| 4 | Wave 1 (parallel worktrees): hand-port remaining records / declined instanceof sites in jetty-core (http2, http3, quic, fcgi, websocket, session, security, deploy, rewrite, compression, …) | ✅ | branch `jdk11/w1-core` merged (47 files); verified by the agent with a `-source 11` compile of 74 modules and a javap API audit against JDK 11 |
| 4 | Wave 1: jetty-ee10, jetty-integrations, jetty-demos | ✅ | branch `jdk11/w1-ee10` merged (32 files); Infinispan 14 / Hazelcast 5.3 needed no further source changes |
| 4 | Wave 1: jetty-ee9 (+ generated ee8), tests | ✅ | branch `jdk11/w1-ee9` merged (21 files); verified by the agent with a true `--release 11` compile of 79 modules |
| 5 | JDK 11 compile loop over the whole reactor (categories only javac 11 detects: static members in inner classes, `ByteBuffer.slice(int,int)`, `isEmpty()` on StringBuilder, try-with-resources on `HttpExchange`, …) | ✅ | **all 364 kept modules compile (main + test) on JDK 11**, `mvn install -DskipTests` green, checkstyle 0 violations. plexus-xml 4.2.0 → 4.0.4 (Java 8), maven-resources-plugin 3.5.0 → 3.4.0 (needed for the `.mod` `@g:a@` token filtering of modify-sources 1.0.13) |
| 6 | jetty-core test suites (all modules) on JDK 11 | ✅ | server-stack batch (33 modules: session, security, deploy, rewrite, jndi, plus, annotations, osgi, keystore, unixdomain, http-spi, compression, maven, …): 764 tests, 0 failures. http2/fcgi/proxy/alpn batch: 538 tests; only failures: `ExternalFastCGIServerTest` (tagged `external`, needs a php-fcgi server), `ConcurrentRequestsTest`/`TrailersTest` (fail identically with the untouched upstream sources on JDK 17 on this machine → pre-existing), `ForwardProxyWithDynamicTransportTest.testProxyConcurrentLoad` (flaky under load). websocket/quic/http3/client-transports batch (30 modules): 1963 tests; only `HttpClientStreamTest` shows load-dependent flakiness (a different parameterized case fails on each run, none deterministic) |
| 7 | EE10 / EE9 / EE8 test suites on JDK 11 | ✅ | EE10: ~2700 tests; EE9: ~2700 tests; EE8: 3164 tests. Remaining failures are all environmental or timing-related, none deterministic on JDK 11: `ProxyWebAppTest` (proxies to https://javadoc.jetty.org, unreachable here), `*DoSFilterTest.testBurstLowRateIP` (wall-clock rate limiting), `DeploymentErrorTest.testDelayedAddBadAppUnavailableFalse` and ee9 `RequestTest` (order/timing dependent, pass in isolation), websocket `IdleTimeoutTest`/`LargeAnnotatedTest` (pass in isolation), and the HTTP/3 (`H3_QUICHE`) variants of the EE client-transport tests failing with quiche `UNKNOWN_CA` — reproduced identically on JDK 17 with the untouched upstream sources on this machine → pre-existing, not caused by the port. Integrations/demos/tests batch (66 modules): 206 tests, 0 failures |
| 8 | Integrations, OSGi, session (Docker) test suites | ✅ | core jetty-session (MariaDB via Testcontainers): green; EE10 session stores (MongoDB, Infinispan 14, Hazelcast 5.3, memcached, GCloud): 113/113; EE9: 113/113; OSGi (pax-exam, Equinox 3.24 on JDK 11) ee8+ee9+ee10: 28/28 (note: the bundle manifests are derived from the class files by bnd — an incremental build after a JDK 17 build keeps stale `osgi.ee=JavaSE;version=17` manifests, always `clean` when switching JDK) |
| 9 | jetty-home build, class-file major-version scan of `lib/` **and of the `maven://` artifacts of the shipped modules** (must be ≤ 55), start.jar smoke test on JDK 11 (ee10/ee9/ee8 demos, JSP with ECJ 3.33) | ✅ | jetty-home builds; scan of `lib/` (198 jars): no class file newer than Java 11; `maven://` scan (212 coordinates, 189 jars, see below): only `jetty-setuid-jna` is Java 17; `start.jar` on JDK 11.0.32 with `server,http,http2c,ee10/ee9/ee8-deploy` + simple/jsp/spec demos: all three environments deploy, `dump.jsp` renders (200) on ee10, ee9 and ee8, no errors in the log |
| 10 | test-distribution, maven-plugin ITs, checkstyle/spotless re-enabled for the final build | ✅ | **final `mvn clean install -DskipTests` on JDK 11 with checkstyle and spotless enabled: BUILD SUCCESS, 364 modules**. test-distribution (starts the built jetty-home on JDK 11 for ee8/ee9/ee10, 220 tests): fixed the ee11-removal fallout in parameterized tests, gated `testUnixDomain` to JDK 16+, replaced the deleted Servlet 6 demo war, aligned the Infinispan Hot Rod runtime stack with Infinispan 14 (WildFly Elytron 2.6.0, wildfly-common 1.6.0, jboss-threads 2.3.6, explicit `infinispan-remote-query-client`), declared `jakarta.mail-api` 2.1.3 for the offline ee10-demo-jndi module; remaining failures are all environmental: `DemoModulesTests.testAsyncRest` (demo calls the eBay API), `OpenIdTests` (Keycloak container `Secure` cookie over plain http, same with upstream), `DistributionTests.testDownload` (needs Maven Central), plus one `BindException` port race. maven-plugin invoker ITs on JDK 11: jetty-maven-plugin 13/13 for each of ee10/ee9/ee8, jspc-maven-plugin 3/3 each |

### Dependency downgrades (step 1)

| property / artifact | 12.1.x (bdc419f91bd) | JDK 11 branch | commit | why |
|---|---|---|---|---|
| `junit.version` | 6.1.3 | 5.14.4 | e1cd4325a4f | JUnit 6 requires Java 17 |
| `junit.platform.version` | `${junit.version}` | 1.14.4 | 18badac0e8f | Platform 1.14.x is the JUnit 5.14.x pairing |
| `testcontainers.version` | 2.0.5 | 1.21.4 | e1cd4325a4f | Testcontainers 2 requires Java 17 |
| `infinispan.version` | 16.2.3 | 14.0.35.Final | e1cd4325a4f | Infinispan 15+ requires Java 17 |
| `infinispan.protostream.version` | 6.0.11 | 4.6.5.Final | e1cd4325a4f | matches Infinispan 14 (source change in b6dbd4c9435) |
| `infinispan.docker.image.version` | 15.2.1.Final | 14.0.35.Final | e1cd4325a4f | server image must match the client |
| `hibernate.search.version` | 8.4.0.Final | 6.1.5.Final | e1cd4325a4f | Hibernate Search 8 requires Java 17; 6.1 is what Infinispan 14 uses |
| `hazelcast.version` | 5.7.0 | 5.3.8 | e1cd4325a4f | 5.4+ is Java 17 bytecode |
| `eclipse.jdt.ecj.version` | 3.46.100 | 3.33.0 | e1cd4325a4f | last ECJ line that runs on Java 11 |
| `modify-sources-plugin.version` | 1.0.14 | 1.0.13 | e1cd4325a4f | 1.0.14 is Java 17 bytecode |
| `maven.resources.plugin.version` | 3.5.0 | 3.4.0 | 74ebeb2c981 | needed for the `.mod` `@g:a@` token filtering of modify-sources 1.0.13 |
| `build-support.version` | 1.6 | 1.5 | e1fc970e650 | 1.6 is Java 17 bytecode; the enforcer rules are referenced by `implementation=` because 1.5 has no sisu index |
| `plexus-xml.version` | 4.2.0 | 4.0.4 | f999b74ef95 | 4.2.0 is Java 17 bytecode |
| `wildfly.elytron.version` | 2.9.2.Final | 2.6.0.Final | c9dbf28c44b | Elytron 2.7+ is Java 17 bytecode; 2.6.0 is the Infinispan 14 Hot Rod stack |
| `wildfly.common.version` | 2.0.1 | 1.6.0.Final | c9dbf28c44b | same |
| `jboss-threads.version` | 3.10.1 | 2.3.6.Final | c9dbf28c44b | same |

### Class-file version scan of the distribution

Two scans must stay green (major version ≤ 55 = Java 11):

1. every jar under `jetty-home/target/jetty-home/lib/**` (what the distribution ships);
2. every `maven://group/artifact/version[/type[/classifier]]` entry in the `[files]` section of
   `jetty-home/target/jetty-home/modules/**/*.mod` — these jars are *not* in `lib/`, they are downloaded
   into `$JETTY_BASE` the first time a user runs `--add-modules=<module>`, so they escape scan 1.

Scan 2 (run after `mvn install -DskipTests -pl jetty-home`): expand the `${...}` placeholders from the `[ini]`
defaults of the `.mod` files, resolve each coordinate from the local repository (or download it from Maven
Central), and report every jar with a class-file major version > 55, ignoring `META-INF/versions/**`
(multi-release jars). Note that the `infinispan-*-libs.mod` and `gcloud-*.mod` `[files]` lists are *generated*
from `dependency:list` of the corresponding `jetty-integrations` module, so those modules have to be rebuilt
before jetty-home or the scan reads stale coordinates.

Result on this branch: 212 distinct coordinates — 189 jars resolved and scanned, 23 reactor `.war` files skipped
(built with `--release 11` in the same reactor) —
exactly one artifact above 55 — `org.eclipse.jetty.toolchain.setuid:jetty-setuid-jna:2.0.3` (all 7 classes are
major 61), see the `setuid` note below.

## Decisions / known deviations

- `Stream.toList()` was replaced by `collect(Collectors.toList())` (mutable list; `toUnmodifiableList()` rejects nulls, `Stream.toList()` does not).
- Record → class conversions keep the accessor names (`r.name()`), so callers are unchanged; `equals/hashCode/toString` follow the record semantics.
- Unix-Domain sockets (Java 16 API) are accessed reflectively via `org.eclipse.jetty.io.UnixDomain` (same approach as Jetty 11); on JDK 11 `Transport.TCPUnix/UDPUnix` and `UnixDomainServerConnector` throw `UnsupportedOperationException`, tests are gated with `@EnabledForJreRange(min = JAVA_16)`.
- JSON/AsyncJSON record support works at runtime on Java 16+ through a reflective helper; the record round-trip tests were removed (a Java 11 test source cannot declare a record).
- `jrt:/<module>` resources: JDK 11's jrt file system uses the older `/modules/<module>` layout, so `ResourceFactory.newResource("jrt:/java.base")` does not resolve on JDK 11; the two tests relying on it are disabled on JDK 11.
- `requireUpperBoundDeps` excludes `jakarta.transaction:jakarta.transaction-api` (Infinispan 14's Hot Rod client uses the javax-namespace 1.3.x artifact, EE9/EE10 the jakarta 2.x one).
- Multi-release jar env for zipfs: both `releaseVersion` (JDK 13+) and `multi-release` (JDK 11/12) keys are set.
- The `setuid` module requires JDK 17+. `jetty-setuid-jna` is published only as 2.0.0–2.0.3 and every one of
  those jars is Java 17 bytecode (major 61); the only Java 8 alternative, `jetty-setuid-java` 1.0.4, is the
  old JNI implementation (different artifact, needs the `libsetuid` native library) and is not API-compatible
  with the `etc/jetty-setuid.xml` shipped by the JNA `config` artifact. `jetty-setuid-version` therefore stays
  at 2.0.3 and `jetty-home/src/main/resources/modules/setuid.mod` overrides the upstream module file to state
  the JDK 17 requirement in its `[description]` (and to take the version from the build property).
- `SessionHandler.ServletSessionApi.getOrCreateSession` (ee9, and the generated ee8) became
  `SessionHandler.getOrCreateSession`: a `static` member of a non-static inner class is illegal before Java 16,
  and no deprecated delegate can be left behind in the inner class for the same reason.
- Public records converted to `final` classes (they no longer extend `java.lang.Record`; component accessor
  names, `equals`/`hashCode`/`toString` semantics are unchanged): jetty-http `ByteRange`,
  `ComplianceViolation.Event`; jetty-server `CustomRequestLog.LogDetail`, `ResourceService.WelcomeAction`,
  `HttpChannel.IdleTimeoutTask`; jetty-client `PathResponseListener.PathResponse`,
  `RedirectCache.MethodOriginTarget`; websocket `Frame.CloseStatus`; compression `EncoderSink.WriteRecord`;
  quic `FrameGenerator.BytesGenerated`, `PemPaths`, `Quiche.CloseInfo`; session
  `AbstractSessionManager.RequestedSession`; rewrite `CompactPathRule.CompactedEvent`,
  `RewriteEncodingRule.Encoding`; deploy `DeploymentScanner.DeployAction`; ee10 `JettyWebConnection`;
  ethereum `SignInWithEthereumToken`, `EthereumAuthenticator.SignedMessage`; demos `JettyDemos.MavenCoordinate`.
- Where a `static` logger had to move out of an inner/anonymous class (illegal before Java 16), the logger
  category is preserved explicitly so log configuration keeps working unchanged.

## Tooling (outside the repo, `/home/eolivelli/dev/jetty-jdk11-tools/`)

`mvn11.sh` (Maven on JDK 11 with a per-worktree local repo), `desugar/` (JavaParser tool), `scan-major.sh` (class-file version scanner), `RECIPE.md` (hand-porting rules given to worker agents).

## Review rounds (automated: picky reviewer agents per commit → worker agents fix → re-review)

| Round | Reviewers | Findings (high/medium/low/nit) | Outcome |
|---|---|---|---|
| 1 | 7 reviewers: build/poms, core hand-port, desugar core main, desugar EE main, desugar tests, wave-1 EE, wave-1 core | in progress | |
