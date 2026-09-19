# Security Considerations for Ublu

Ublu is an administrator-oriented scripting language designed for managing IBM i
(AS/400) midrange systems, mainframes, and databases. It is an **operator tool**, not
a public-facing web service. This document describes the security posture of the
project and the hardening steps required before exposing it on any network.

---

## Intended Deployment Model

Ublu is intended to be run:

- **Locally** as a REPL by a trusted operator logged in to the host system.
- **Over a private, firewalled network** via its optional socket server, where all
  clients are trusted administrators.

It is **not** designed to be exposed directly on a public network or multi-tenant
environment without additional hardening layers.

---

## Network Server Authentication

The server has no built-in login handshake, but the **documented and supported
pattern for adding authentication** is to use the `-block` switch, which causes
the server to run a user-supplied Ublu `FUNC` for every incoming connection before
the client gets interpreter access. This function can implement any authentication
logic the operator requires — password challenge, token validation, IP allowlist
checking, etc.

```sh
server -port 4242 -block ${ func authenticate ... }$
```

See the [server command reference](http://www.softwoehr.com/oss/ubludoc/ubluref.html#server)
for the full `-block` usage.

When the server is started **without** a `-block` function, every connecting client
receives full interpreter access immediately. In that configuration:

**Risk:** Any host that can reach the server port can execute arbitrary Ublu commands.

**Mitigations (operator responsibility):**

- Use `-block` with an authentication `FUNC` whenever the server is reachable from
  anything other than a fully trusted host.
- Restrict access at the firewall level. Bind the server to a specific interface
  (`server -bind 127.0.0.1 ...`) rather than all interfaces.
- Wrap Ublu in an SSH tunnel or authenticated reverse proxy if remote access is
  required from untrusted networks where a `-block` function alone is insufficient.
- Enable TLS with the `-ssl` flag to encrypt traffic in transit.
- The server enforces a **30-second read timeout** per client and a **maximum of 20
  simultaneous connections** by default. These limit denial-of-service exposure but
  do not replace authentication.

---

## OS Command Execution (`system` command)

The `system` command calls `Runtime.exec()` with an operator-supplied string.
There is **no sandboxing, no command allowlist, and no input validation** — this is
by design. The command executes under the OS user identity of the JVM process.

**Risk:** Full OS command execution with JVM user privileges.

**Mitigations (operator responsibility):**

- **Never run Ublu as root or a privileged OS account.** Create a dedicated
  least-privilege OS user for Ublu processes.
- Run inside a container or OS sandbox (e.g. SELinux, AppArmor, `seccomp`) to
  restrict what the JVM user can do on the host.
- If exposing the server, treat it as equivalent to giving the client a shell.

---

## Java Reflection (`calljava` command)

The `calljava` command calls `Class.forName()` with an operator-supplied class name
and invokes arbitrary public methods on any class on the JVM classpath. This
includes `java.lang.Runtime`, `java.lang.System`, `java.lang.ProcessBuilder`, and
all other JDK and library classes.

**Risk:** A client can invoke `Runtime.getRuntime().exec()`, `System.exit()`,
`System.setProperty()`, or any other reflective target. This is equivalent to
arbitrary code execution.

**Mitigations (operator responsibility):**

- Treat `calljava` access as equivalent to local JVM execution rights.
- Apply the same OS-level isolation as for the `system` command.
- The Java Security Manager (deprecated in Java 17, removed in Java 21) can restrict
  reflection in newer JVMs; this does not apply under the Java 8 target.

---

## File Access

Ublu commands (`ifs`, `streamf`, `file`, output sinks) accept operator-supplied
file paths with **no canonical-path validation and no sandbox directory**.

**Risk:** Read and write access to any file readable/writable by the JVM process.

**Mitigations (operator responsibility):**

- Run Ublu under an OS user that has only the minimum required file permissions.
- Use OS-level directory permissions and ACLs to constrain accessible paths.

---

## Credential Handling

Passwords for IBM i connections are accepted as `String` parameters. Java `String`s
are immutable and interned; they remain in the heap until garbage-collected and
**cannot be reliably zeroed after use**.

**Risk:** Heap dump analysis may reveal passwords.

**Recommendations:**

- Do not log command-line arguments; they may contain inline passwords.
- Prefer interactive prompts (handled by the built-in `SignonHandler`) over
  embedding passwords in scripts.
- Avoid storing scripts containing plaintext passwords in version control or
  world-readable files.

The trust-store password for SSL connections is read from the system property
`javax.net.ssl.trustStorePassword`. Protect this property value appropriately
(use environment injection at startup rather than command-line `-D` flags where
possible, to avoid exposure in process listings).

---

## Deserialization (`savesys` / `restoresys`)

The `savesys` and `restoresys` commands serialize and deserialize the interpreter's
function map and constant map. As of the current release, deserialization uses a
class allow-list that restricts restored objects to the small set of types that
Ublu legitimately stores (`FunctorMap`, `ConstMap`, `Functor`, `ArgArray`,
`Const`, `String`, `LinkedHashMap`, `HashMap`, `ArrayList`).

**Recommendation:** Treat `.ublusys` save files as trusted artifacts. Do not
restore save files sourced from untrusted parties.

---

## Recommended Hardening Checklist

| Item | Action |
| ------ | -------- |
| Authentication | Use `server -block` with an auth `FUNC` for any non-trusted exposure |
| OS user | Run under a dedicated least-privilege account, never root |
| Network binding | Bind server to loopback or VPN interface only |
| Firewall | Block the Ublu server port at the network boundary |
| TLS | Use `-ssl` for any non-loopback server exposure |
| Container | Run inside a container or OS sandbox for production use |
| Logging | Configure `java.util.logging` to write to a file, not stdout |
| Scripts | Do not embed plaintext passwords in `.ublu` script files |
| Save files | Treat `.ublusys` files as trusted; store with restricted permissions |
