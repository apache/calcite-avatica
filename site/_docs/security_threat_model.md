---
layout: docs
title: Security threat model
sidebar_title: Threat model
permalink: /docs/security_threat_model.html
---
<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

Apache Avatica is a JDBC/ODBC wire-protocol layer: a server that fronts a
local JDBC `DataSource` (typically Apache Calcite, but any JDBC driver
is supported), and a client-side JDBC driver that speaks the Avatica
wire protocol over HTTP or HTTPS. This threat model covers what an
attacker who reaches an Avatica server over the wire, or who tricks an
Avatica client into connecting to a server, can and cannot do.
This model covers the Java server and the Java (JDBC) client in this
repository. `apache/calcite-avatica-go`
is a separate implementation that speaks the same wire protocol; it
is not covered here and would need its own model.

Avatica treats the behaviors below as security vulnerabilities, so that
reporters and committers triage them the same way. A report that
contradicts this model is a feature request or a documentation gap, not a
vulnerability.

Companion documents:

* [Security features]({{ site.baseurl }}/docs/security.html) — how to
  configure authentication, TLS, and impersonation. This document (threat
  model) is normative; the features document is operational guidance.
* Apache Calcite's [security threat
  model](https://calcite.apache.org/docs/security_threat_model.html) —
  Calcite's model covers only what happens inside the embedded engine
  once a JDBC connection has been established. When Avatica is fronting
  Calcite, the two documents divide responsibility: Avatica owns the
  wire; Calcite owns the query engine.


* TOC
{:toc}

## Attacker and trust boundary

Avatica has two deployment shapes and therefore two attacker profiles:

**Server-side attacker: an HTTP client** that can reach an Avatica
server over the network.

The attacker can:

* open TCP connections to the server's listening port;
* send arbitrary HTTP requests, including well-formed and malformed
  Avatica protocol frames (JSON or Protobuf);
* if the server has no authentication configured, be treated as
  authenticated;
* if the server has authentication configured, attempt to authenticate
  and observe responses; a valid credential upgrades the attacker to
  the *authenticated caller* profile.

The attacker cannot:

* change JVM system properties, `avatica-server` command-line
  arguments, `HttpServer.Builder` configuration, or the classpath;
* obtain valid authentication credentials without an out-of-band step
  (that step is outside this model — see [Downstream
  responsibilities](#downstream-responsibilities)).

**Authenticated caller:** the profile above plus a valid credential.
An authenticated caller can:

* execute any SQL the underlying JDBC driver accepts, subject to the
  underlying driver's own authorization model (Avatica has none of its
  own — see [Downstream
  responsibilities](#downstream-responsibilities));
* set any Avatica connection property that is not restricted by the
  server's configuration.

**Client-side attacker: a hostile Avatica server**, or a network
attacker between the client and a legitimate server.

The attacker can:

* return arbitrary bytes on the HTTP response body, including malformed
  or crafted Avatica protocol frames;
* if hostname verification or truststore validation is disabled or
  weakened by the client's configuration, present any TLS certificate.

The attacker cannot:

* choose the client's JVM system properties, connection-string
  properties, or classpath — those are operator- and application-level
  decisions.

## Assets

* **The Avatica server host.** No code execution outside the query
  path Avatica is configured to expose; no incidental file or network
  access.
* **The credentials Avatica handles.** BASIC and DIGEST passwords in
  transit, Bearer tokens, Kerberos tickets, and the passwords for
  configured keystores and truststores.
* **The backend JDBC driver's data.** Avatica is a wire layer, not an
  access-control layer; the driver's own authorization model governs
  what data is reachable, but the *identity* Avatica hands to the
  driver via impersonation must correctly reflect the authenticated
  caller.
* **The Avatica client's JVM.** No code execution triggered by
  content on the wire; no incidental file or network access.

## Inputs

Everything the attacker controls resolves to one of P1–P5 or to an explicit
carve-out below. A report that reaches a sink not covered here is a model gap
(see [Triage dispositions](#triage-dispositions)).

| Input | How it is supplied | What it feeds | Governing rule |
| --- | --- | --- | --- |
| HTTP request bytes | wire | Jetty request parser → Avatica handler → wire deserialization (JSON via Jackson, or Protobuf) → `Service` dispatch | P1, P2 |
| Authentication credentials | HTTP `Authorization` header, query string, SPNEGO negotiation | Avatica server configuration → Jetty security handlers | P5 |
| `doAs` / impersonation identity | either the authenticated principal, or a configurable extractor from HTTP request (e.g. `HttpQueryStringParameterRemoteUserExtractor`) | `DoAsRemoteUserCallback` → backend JDBC driver | P5 (see [Impersonation](#impersonation)) |
| SQL text and JDBC parameter values | HTTP body | Avatica `Service` → backend JDBC driver | Governed by the backend driver's own model; Avatica passes it through |
| `TypedValue` payloads (parameter values, result rows) | HTTP body | `TypedValue.fromProto` / `fromJson` → `Object` in server or client JVM | P1 |
| Class-naming client-side wire fields — e.g. `CursorFactory.className` | HTTP response body from server | `Class.forName(name)` on the client | Surprising vs unsurprising class loading (P1); see [Client-side surprising class loading](#client-side-surprising-class-loading) |
| Client-side connection properties — `httpclient_factory`, `httpclient_impl`, `bearer_token_provider_class`, `lb_strategy`, `factory` | JDBC connection string, `Properties`, or a URL fragment the embedding application composes | client-side `AvaticaUtils.instantiatePlugin` and equivalents | Surprising vs unsurprising class loading (P1) |
| TLS material — `truststore`, `keystore`, passwords, `hostname_verification` | JDBC connection string or `Properties` | client-side TLS setup | P5 |
| Server-side configuration — port, `sslFactory`, `withImpersonation`, `withCustomAuthentication`, `HandlerFactory` | `HttpServer.Builder` in embedder code | Jetty server assembly | Operator trust (see [Downstream responsibilities](#downstream-responsibilities)) |

## Security properties

* **P1: no code execution.** Neither receiving a wire frame on either
  end nor decoding it may execute code outside Avatica's protocol
  semantics. This covers `Runtime.exec`, `ProcessBuilder`, and the
  weaker primitive of loading an attacker-named class so that its
  static initializer, constructor, or an accessed static field runs.
* **P2: no incidental file access.** Neither receiving a wire frame nor
  decoding it may read or create a file, except for the specific
  configuration files an operator has explicitly named (keystore,
  truststore, keytab, token file). An attacker-controlled path reaching
  a file-read sink is a vulnerability.
* **P3: no server-side request forgery.** Neither receiving a wire
  frame nor decoding it may open an outbound network connection to an
  attacker-chosen host. The Avatica server dials only the JDBC URL its
  operator configured; the Avatica client dials only the URL its own
  operator configured.
* **P4: no impersonation escape.** The identity that Avatica hands to
  the backend JDBC driver, via `DoAsRemoteUserCallback` or an
  equivalent, must correspond to the authenticated caller — never to a
  caller-chosen identity in a context where authentication was
  supposed to establish it.
* **P5: no wire secret disclosure.** Credentials on the wire — BASIC
  passwords, Bearer tokens, Kerberos tickets, keystore and truststore
  passwords, session cookies — must not be exposed to a passive
  observer on the network under the configured TLS settings, and must
  not be logged or echoed back to unauthenticated callers.

## Always a vulnerability

1. **Code execution** — on the server host, or in the client JVM —
   that results from receiving a wire frame or from decoding it. The
   bar is the primitive, not a full chain: a reachable sink that loads
   an attacker-named class qualifies, because class loading runs the
   static initializer before any type check.
2. **Arbitrary file read or write** on either the server host or the
   client JVM, driven by an attacker-controlled input on the wire or
   in a connection property that a caller can set. Reading the
   operator-configured keystore, truststore, keytab, or token file is
   not incidental access.
3. **Server-side request forgery** — the Avatica server or client
   opens a network connection to an attacker-chosen host as a result
   of a wire frame or connection property.
4. **Impersonation escape.** A caller reaches the backend JDBC driver
   under an identity that is not the identity authentication
   established. Includes: unauthenticated `doAs` when authentication
   is meant to be required; a `doAs` value the extractor accepts
   without validation against the authenticated principal; a session
   whose identity survives past its authentication.
5. **Wire secret disclosure.** Credentials sent over the wire are
   observable by a passive attacker under the deployment's declared TLS
   settings; or are logged or echoed to a caller who did not
   authenticate; or are stored on the server in a form other than
   what the deployment declared. Falling back to plaintext HTTP for
   an authentication scheme that requires TLS is included.
6. **Authentication bypass.** A caller reaches the `Service` dispatch
   under a configured authentication mode without supplying a valid
   credential.
7. **Denial of service by a single wire frame** — a single request
   parseable within stated size limits should not be able to exhaust
   server resources, except where [Denial of service](#denial-of-service)
   records the bound as not yet landed. Combinatorial or unbounded resource
   consumption from a single frame is a vulnerability (see also
   [Denial of service](#denial-of-service) for known limitations).

## Not a vulnerability

* **An unauthenticated Avatica server exposed to an untrusted
  network.** `AuthenticationType.NONE` is a valid configuration for
  isolated or internally-networked deployments. Reachability of an
  unauthenticated server from the public internet is an operator
  decision (see [Downstream
  responsibilities](#downstream-responsibilities)).
* **An unencrypted Avatica server** — running Avatica without
  `sslFactory` — exposing HTTP credentials to a network eavesdropper.
  This is an operator decision. Avatica does not force TLS.
* **Backend driver behavior.** Once Avatica has connected to the JDBC
  URL its operator configured, the backend driver's SQL semantics,
  its authorization model, its own configuration, its bugs and its
  CVEs are that driver's concern — Avatica passes SQL through.
* **The behavior of the `StandaloneServer` demo.** The
  `standalone-server` module ships a runnable jar for local
  development and testing. It has no authentication, no TLS, and no
  impersonation by default. Its purpose is to demonstrate the
  protocol; running it in production without additional configuration
  is not supported and not covered by this model.
* **Cross-tenant or cross-schema visibility handled by the backend
  driver.** Avatica has no schema visibility model of its own; the
  backend driver (typically Calcite) is responsible for scoping what
  each authenticated caller may see. See Calcite's threat model for
  its treatment of this concern.
* **Anything requiring a change to server-side configuration** — port,
  `sslFactory`, `HttpServer.Builder` options, command-line arguments,
  system properties, or the classpath. Those are operator inputs, not
  attacker inputs, by definition.
* **`hostname_verification=NONE` or an accepted truststore that
  trusts a hostile CA.** Both are documented client-side connection
  properties. A client that configures itself into a weakened trust
  posture accepts that posture. Not a client-side vulnerability.

## Downstream responsibilities

Several controls belong to the operator or the embedding application, not to
Avatica itself. A finding that lands in one of these is not an Avatica
vulnerability.

* **Network perimeter.** Deciding who can reach Avatica's listening
  port is the operator's job. Avatica has no built-in IP allowlist,
  no rate limiting, and no DDoS protection.
* **Choice of authentication mode.** `AuthenticationType.NONE` is
  legal for isolated networks; production deployments on shared or
  public networks should configure BASIC, DIGEST, SPNEGO, Bearer, or a
  `CUSTOM` scheme. Avatica does not force a choice.
* **Choice to enable TLS.** `HttpServer.Builder.withSSL(...)` opts
  into HTTPS. Deploying without TLS on an untrusted network exposes
  credentials by construction; Avatica does not require TLS.
* **Choice of impersonation extractor.** Configuring
  `HttpQueryStringParameterRemoteUserExtractor` or a custom
  `RemoteUserExtractor` that trusts a caller-supplied `doAs` value
  without authenticating it is an operator misconfiguration — see
  [Impersonation](#impersonation) for the boundary.
* **Backend driver selection.** Which JDBC URL Avatica fronts is the
  operator's choice. The backend driver's own security posture is out
  of this model.
* **What credentials, keystores, keytabs, and token files exist on
  disk.** The paths Avatica reads are named in the operator's
  configuration; ensuring those files are readable only by the
  Avatica process is a filesystem-permission concern for the
  operator.
* **Classpath.** The operator owns the classpath. `HttpClient`
  factories, `BearerTokenProvider` classes, and load-balancing
  strategies are loaded by name; which classes are present is the
  operator's trust decision.
* **The Avatica client's connection string.** A JDBC connection
  string may name a `httpclient_factory` or a
  `bearer_token_provider_class`. If the embedding application lets an
  untrusted caller compose the connection string, that caller has the
  same capability as the operator with respect to class loading.

## Surprising vs unsurprising class loading

Avatica loads a class named in a connection property, in a wire frame,
or in a configuration file only to use it through a specific SPI:
`AvaticaHttpClient`, `BearerTokenProvider`, `LBStrategy`, `Meta.Factory`,
`ColumnMetaData.Rep` element type. The security boundary follows that
contract, not a blanket trust of the classpath or of the wire.

* **Unsurprising.** The class implements the SPI interface for the
  position it was named in, and Avatica invokes it through that
  interface.
* **Surprising.** Naming a class runs the class's own code — a static
  initializer, constructor, method, or static-field read — even
  though it does not implement the SPI for that position.
  Surprising class loading is always a vulnerability.

**Mechanism.** Load with `Class.forName(name, false, loader)`, check
`pluginClass.isAssignableFrom(clazz)`, and only then initialize and
instantiate. A class that fails the check never runs its static
initializer.

### Client-side surprising class loading

The Avatica client reconstructs `Meta.CursorFactory` from a wire
`Common.CursorFactory` proto whose `className` field is
attacker-controllable if the server is hostile or the wire is
tampered with. The current implementation of `CursorFactory.fromProto`
in `Meta` calls `Class.forName(name)` — which initializes the class
before any type check.

This sink is tracked as a known open primitive under the P1 triage
rule: a reachable class-loading sink that fires before an interface
gate is a vulnerability on its own, whether or not a specific
end-to-end exploit chain against a specific classpath has been
demonstrated.

The fix pattern is the same as elsewhere: load with `initialize=false`,
gate on an allowlist of expected element types (`ColumnMetaData.Rep`
values and their corresponding Java classes), and only then initialize.

## Impersonation

Avatica supports impersonation: an authenticated caller may execute
SQL on the backend JDBC driver *as* another identity, using
`DoAsRemoteUserCallback`. The security boundary here is subtle
enough to spell out.

The identity Avatica hands to the callback is produced by a
`RemoteUserExtractor`. Three implementations ship with Avatica:

* `HttpRequestRemoteUserExtractor` — returns
  `HttpServletRequest.getRemoteUser()`, i.e. the identity Jetty's
  security handler established through the configured authentication
  mode. Safe by construction: only reflects a value the caller
  authenticated to.
* `HttpQueryStringParameterRemoteUserExtractor` — returns the value
  of a caller-supplied query-string parameter. **This is safe only
  when the caller is authenticated to some other identity** (e.g. a
  Kerberos proxy identity that is separately authorized to impersonate
  other users). Configuring this extractor on an
  `AuthenticationType.NONE` server, or on any server where the
  caller's own authenticated identity is not checked against
  authorization to impersonate the extracted user, is an operator
  misconfiguration that P4 rules out.
* Any operator-supplied `RemoteUserExtractor`. Operator's
  responsibility.

The boundary: **who is allowed to impersonate whom** is an operator
policy that Avatica does not enforce. A bug where Avatica accepts an
impersonation request under a configured mode without applying the
operator's declared policy is a P4 vulnerability. An operator who
configures an extractor that accepts unauthenticated `doAs` values
has misconfigured Avatica; that is not a P4 vulnerability but it *is*
an item Avatica's documentation should call out sharply.

## Denial of service

A single request within stated size limits should not be able to
exhaust the server. This is in scope as a hardening goal. The
controls are not all in place yet, so treat the gaps below as known
limitations rather than per-report vulnerabilities until the
controls land.

* **Request size.** Jetty's `maxAllowedHeaderSize` configuration on
  `HttpServer.Builder` bounds header size; the wire body has no
  explicit Avatica-side cap and inherits Jetty's default. Very large
  SQL bodies or `TypedValue` payloads can exhaust heap.
* **Result-set streaming.** A backend query that returns a very
  large result exhausts server heap unless the caller uses the
  incremental fetch protocol correctly. Operator-tunable via fetch
  size.
* **Connection accumulation.** Long-lived Avatica connections keep
  backend JDBC connections open. A caller that opens many
  connections and never closes them exhausts backend resources.
  Mitigation is an operator-side connection cap.

Once request-size and connection caps exist, a single reasonably-sized
request that exceeds them is a configuration choice, not a
vulnerability.

## Historical CVEs

Avatica has published these CVEs relevant to this model, fixed in current releases;
they are noted here as ground truth for the rule they establish.

* **CVE-2022-36364** — an untrusted URL supplied to the Avatica
  JDBC driver could load an arbitrary `httpclient_impl` class via
  `Class.forName`, without an interface gate. The fix added the
  `asSubclass(AvaticaHttpClient.class)` check that is now present in
  `AvaticaHttpClientFactoryImpl`. Established the surprising-class-
  loading rule for client-side connection properties.

## Triage dispositions

Every security report against Avatica resolves to exactly one of:

* **Valid** — violates P1–P5, or matches an item in [Always a
  vulnerability](#always-a-vulnerability). Gets a fix. A demonstrated
  class-loading primitive qualifies on its own.
* **Not a vulnerability (by design)** — matches an item in [Not a
  vulnerability](#not-a-vulnerability): an unauthenticated or
  un-encrypted deployment the operator chose, backend driver
  behavior past the connection, cross-schema visibility that lives
  in the backend driver, `StandaloneServer` demo behavior, or a
  client that opted itself into a weakened trust posture. Close with
  a pointer to this model.
* **Out of model** — requires a capability the attacker does not
  have (changing a JVM system property, server-side configuration,
  the classpath), or lands in a layer this model assigns to the
  operator (network perimeter, authentication choice, TLS choice,
  filesystem permissions). Close; redirect to the operator or
  embedder.
* **Backend concern** — the report is really about the backend JDBC
  driver Avatica is fronting. Close; redirect to that project's
  security process. If the backend is Apache Calcite, redirect to
  [Calcite's threat
  model](https://calcite.apache.org/docs/security_threat_model.html).
* **Known limitation** — a [Denial of service](#denial-of-service)
  gap whose control has not landed yet. Tracked as hardening, not a
  per-report vulnerability, until the bound exists.
* **Duplicate** — the same sink or root cause is already tracked in
  an open Jira. Link and close.
* **Model gap** — plausible, but this model does not clearly place
  it in or out. Escalate to the PMC to decide, then update this
  document with the ruling so the next report of its kind is no
  longer a gap.
