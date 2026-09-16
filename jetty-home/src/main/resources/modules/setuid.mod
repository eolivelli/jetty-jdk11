# See: https://eclipse.dev/jetty/documentation/
# This file overrides the setuid.mod shipped in the jetty-setuid-jna "config" artifact:
# it adds the JDK 17 requirement note and takes the artifact version from the build.

[description]
Enables the UNIX setUID configuration.
The server may be started as root to open privileged ports/files before
changing to a restricted user (e.g. Jetty).
NOTE: this module requires Java 17 or later: the jetty-setuid-jna artifact it
downloads is compiled for Java 17 and no Java 11 build of it exists.

[depend]
server
jna

[lib]
lib/setuid/jetty-setuid-jna-${jetty-setuid.version}.jar

[ini]
jetty-setuid.version?=@jetty-setuid-version@

[files]
maven://org.eclipse.jetty.toolchain.setuid/jetty-setuid-jna/${jetty-setuid.version}|lib/setuid/jetty-setuid-jna-${jetty-setuid.version}.jar

[xml]
etc/jetty-setuid.xml

[ini-template]
## SetUID Configuration
# jetty.setuid.startServerAsPrivileged=false
# jetty.setuid.userName=jetty
# jetty.setuid.groupName=jetty
# jetty.setuid.umask=002
# jetty.setuid.clearSupplementalGroups=false
