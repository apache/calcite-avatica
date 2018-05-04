/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.avatica.server;

import org.apache.calcite.avatica.metrics.MetricsSystemConfiguration;
import org.apache.calcite.avatica.remote.AuthenticationType;
import org.apache.calcite.avatica.remote.Driver.Serialization;
import org.apache.calcite.avatica.remote.Service;
import org.apache.calcite.avatica.remote.Service.RpcMetadataResponse;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Avatica HTTP server.
 *
 * <p>If you need to change the server's configuration, override the
 * {@link #configureConnector(ServerConnector, int)} method in a derived class.
 */
public class HttpServer {
  private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);
  private static final int MAX_ALLOWED_HEADER_SIZE = 1024 * 64;

  private Server server;
  private int port = -1;
  private final AvaticaHandler handler;
  private final AvaticaServerConfiguration config;
  private final Subject subject;
  private final SslContextFactory sslFactory;
  private final List<ServerCustomizer<Server>> serverCustomizers;
  private final int maxAllowedHeaderSize;

  @Deprecated
  public HttpServer(Handler handler) {
    this(wrapJettyHandler(handler));
  }

  /**
   * Constructs an {@link HttpServer} which binds to an ephemeral port.
   * @param handler The Handler to run
   */
  public HttpServer(AvaticaHandler handler) {
    this(0, handler);
  }

  @Deprecated
  public HttpServer(int port, Handler handler) {
    this(port, wrapJettyHandler(handler));
  }

  /**
   * Constructs an {@link HttpServer} with no additional configuration.
   * @param port The listen port
   * @param handler The Handler to run
   */
  public HttpServer(int port, AvaticaHandler handler) {
    this(port, handler, null);
  }

  /**
   * Constructs an {@link HttpServer}.
   * @param port The listen port
   * @param handler The Handler to run
   * @param config Optional configuration for the server
   */
  public HttpServer(int port, AvaticaHandler handler, AvaticaServerConfiguration config) {
    this(port, handler, config, null);
  }

  /**
   * Constructs an {@link HttpServer}.
   * @param port The listen port
   * @param handler The Handler to run
   * @param config Optional configuration for the server
   * @param subject The javax.security Subject for the server, or null
   */
  public HttpServer(int port, AvaticaHandler handler, AvaticaServerConfiguration config,
      Subject subject) {
    this(port, handler, config, subject, null);
  }

  /**
   * Constructs an {@link HttpServer}.
   * @param port The listen port
   * @param handler The Handler to run
   * @param config Optional configuration for the server
   * @param subject The javax.security Subject for the server, or null
   * @param sslFactory A configured SslContextFactory, or null
   */
  public HttpServer(int port, AvaticaHandler handler,
      AvaticaServerConfiguration config, Subject subject,
      SslContextFactory sslFactory) {
    this(port, handler, config, subject, sslFactory,
        Collections.<ServerCustomizer<Server>>emptyList(),
        MAX_ALLOWED_HEADER_SIZE);
  }

  /**
   * Constructs an {@link HttpServer}.
   * @param port The listen port
   * @param handler The Handler to run
   * @param config Optional configuration for the server
   * @param subject The javax.security Subject for the server, or null
   * @param sslFactory A configured SslContextFactory, or null
   * @param maxAllowedHeaderSize A maximum size in bytes that are allowed in an HTTP header
   */
  public HttpServer(int port, AvaticaHandler handler, AvaticaServerConfiguration config,
      Subject subject, SslContextFactory sslFactory, int maxAllowedHeaderSize) {
    this(port, handler, config, subject, sslFactory,
        Collections.<ServerCustomizer<Server>>emptyList(),
        maxAllowedHeaderSize);
  }

  /**
   * Constructs an {@link HttpServer}.
   * @param port The listen port
   * @param handler The Handler to run
   * @param config Optional configuration for the server
   * @param subject The javax.security Subject for the server, or null
   * @param sslFactory A configured SslContextFactory, or null
   * @param maxAllowedHeaderSize A maximum size in bytes that are allowed in an HTTP header
   */
  private HttpServer(int port, AvaticaHandler handler, AvaticaServerConfiguration config,
      Subject subject, SslContextFactory sslFactory,
      List<ServerCustomizer<Server>> serverCustomizers, int maxAllowedHeaderSize) {
    this.port = port;
    this.handler = handler;
    this.config = config;
    this.subject = subject;
    this.sslFactory = sslFactory;
    this.serverCustomizers = serverCustomizers;
    this.maxAllowedHeaderSize = maxAllowedHeaderSize;
  }

  static AvaticaHandler wrapJettyHandler(Handler handler) {
    if (handler instanceof AvaticaHandler) {
      return (AvaticaHandler) handler;
    }
    // Backwards compatibility, noop's the AvaticaHandler interface
    return new DelegatingAvaticaHandler(handler);
  }

  public void start() {
    if (null != subject) {
      // Run the start in the privileged block (as the kerberos-identified user)
      Subject.doAs(subject, new PrivilegedAction<Void>() {
        @Override public Void run() {
          internalStart();
          return null;
        }
      });
    } else {
      internalStart();
    }
  }

  protected void internalStart() {
    if (server != null) {
      throw new RuntimeException("Server is already started");
    }

    final QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setDaemon(true);
    server = new Server(threadPool);
    server.manage(threadPool);

    final ServerConnector connector = configureConnector(getConnector(), port);
    ConstraintSecurityHandler securityHandler = null;

    if (null != this.config) {
      switch (config.getAuthenticationType()) {
      case SPNEGO:
        // Get the Handler for SPNEGO authentication
        securityHandler = configureSpnego(server, connector, this.config);
        break;
      case BASIC:
        securityHandler = configureBasicAuthentication(server, connector, config);
        break;
      case DIGEST:
        securityHandler = configureDigestAuthentication(server, connector, config);
        break;
      default:
        // Pass
        break;
      }
    }

    server.setConnectors(new Connector[] { connector });

    // Default to using the handler that was passed in
    final HandlerList handlerList = new HandlerList();
    Handler avaticaHandler = handler;

    // Wrap the provided handler for security if we made one
    if (null != securityHandler) {
      securityHandler.setHandler(handler);
      avaticaHandler = securityHandler;
    }

    handlerList.setHandlers(new Handler[] {avaticaHandler, new DefaultHandler()});

    server.setHandler(handlerList);
    // Apply server customizers
    for (ServerCustomizer<Server> customizer : this.serverCustomizers) {
      customizer.customize(server);
    }

    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    port = connector.getLocalPort();

    LOG.info("Service listening on port {}.", getPort());

    // Set the information about the address for this server
    try {
      this.handler.setServerRpcMetadata(createRpcServerMetadata(connector));
    } catch (UnknownHostException e) {
      // Failed to do the DNS lookup, bail out.
      throw new RuntimeException(e);
    }
  }

  private ServerConnector getConnector() {
    HttpConnectionFactory factory = new HttpConnectionFactory();
    factory.getHttpConfiguration().setRequestHeaderSize(maxAllowedHeaderSize);

    if (null == sslFactory) {
      return new ServerConnector(server, factory);
    }
    return new ServerConnector(server, AbstractConnectionFactory.getFactories(sslFactory, factory));
  }

  private RpcMetadataResponse createRpcServerMetadata(ServerConnector connector) throws
      UnknownHostException {
    String host = connector.getHost();
    if (null == host) {
      // "null" means binding to all interfaces, we need to pick one so the client gets a real
      // address and not "0.0.0.0" or similar.
      host = InetAddress.getLocalHost().getHostName();
    }

    final int port = connector.getLocalPort();

    return new RpcMetadataResponse(
        String.format(Locale.ROOT, "%s:%d", host, port));
  }

  /**
   * Configures the <code>connector</code> given the <code>config</code> for using SPNEGO.
   *
   * @param connector The connector to configure
   * @param config The configuration
   */
  protected ConstraintSecurityHandler configureSpnego(Server server, ServerConnector connector,
      AvaticaServerConfiguration config) {
    final String realm = Objects.requireNonNull(config.getKerberosRealm());
    final String principal = Objects.requireNonNull(config.getKerberosPrincipal());

    // A customization of SpnegoLoginService to explicitly set the server's principal, otherwise
    // we would have to require a custom file to set the server's principal.
    PropertyBasedSpnegoLoginService spnegoLoginService =
        new PropertyBasedSpnegoLoginService(realm, principal);

    // Roles are "realms" for Kerberos/SPNEGO
    final String[] allowedRealms = getAllowedRealms(realm, config);

    return configureCommonAuthentication(server, connector, config, Constraint.__SPNEGO_AUTH,
        allowedRealms, new AvaticaSpnegoAuthenticator(), realm, spnegoLoginService);
  }

  protected String[] getAllowedRealms(String serverRealm, AvaticaServerConfiguration config) {
    // Roles are "realms" for Kerberos/SPNEGO
    String[] allowedRealms = new String[] {serverRealm};
    // By default, only the server's realm is allowed, but other realms can also be allowed.
    if (null != config.getAllowedRoles()) {
      allowedRealms = new String[config.getAllowedRoles().length + 1];
      allowedRealms[0] = serverRealm;
      System.arraycopy(config.getAllowedRoles(), 0, allowedRealms, 1,
          config.getAllowedRoles().length);
    }
    return allowedRealms;
  }

  protected ConstraintSecurityHandler configureBasicAuthentication(Server server,
      ServerConnector connector, AvaticaServerConfiguration config) {
    final String[] allowedRoles = config.getAllowedRoles();
    final String realm = config.getHashLoginServiceRealm();
    final String loginServiceProperties = config.getHashLoginServiceProperties();

    HashLoginService loginService = new HashLoginService(realm, loginServiceProperties);
    server.addBean(loginService);

    return configureCommonAuthentication(server, connector, config, Constraint.__BASIC_AUTH,
        allowedRoles, new BasicAuthenticator(), null, loginService);
  }

  protected ConstraintSecurityHandler configureDigestAuthentication(Server server,
      ServerConnector connector, AvaticaServerConfiguration config) {
    final String[] allowedRoles = config.getAllowedRoles();
    final String realm = config.getHashLoginServiceRealm();
    final String loginServiceProperties = config.getHashLoginServiceProperties();

    HashLoginService loginService = new HashLoginService(realm, loginServiceProperties);
    server.addBean(loginService);

    return configureCommonAuthentication(server, connector, config, Constraint.__DIGEST_AUTH,
        allowedRoles, new DigestAuthenticator(), null, loginService);
  }

  protected ConstraintSecurityHandler configureCommonAuthentication(Server server,
      ServerConnector connector, AvaticaServerConfiguration config, String constraintName,
      String[] allowedRoles, Authenticator authenticator, String realm,
      LoginService loginService) {

    Constraint constraint = new Constraint();
    constraint.setName(constraintName);
    constraint.setRoles(allowedRoles);
    // This is telling Jetty to not allow unauthenticated requests through (very important!)
    constraint.setAuthenticate(true);

    ConstraintMapping cm = new ConstraintMapping();
    cm.setConstraint(constraint);
    cm.setPathSpec("/*");

    ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
    sh.setAuthenticator(authenticator);
    sh.setLoginService(loginService);
    sh.setConstraintMappings(new ConstraintMapping[]{cm});
    sh.setRealmName(realm);

    return sh;
  }

  /**
   * Configures the server connector.
   *
   * <p>The default configuration sets a timeout of 1 minute and disables
   * TCP linger time.
   *
   * <p>To change the configuration, override this method in a derived class.
   * The overriding method must call its super method.
   *
   * @param connector connector to be configured
   * @param port port number handed over in constructor
   */
  protected ServerConnector configureConnector(ServerConnector connector, int port) {
    connector.setIdleTimeout(60 * 1000);
    connector.setSoLingerTime(-1);
    connector.setPort(port);
    return connector;
  }

  protected AvaticaServerConfiguration getConfig() {
    return this.config;
  }

  public void stop() {
    if (server == null) {
      throw new RuntimeException("Server is already stopped");
    }

    LOG.info("Service terminating.");
    try {
      final Server server1 = server;
      port = -1;
      server = null;
      server1.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void join() throws InterruptedException {
    server.join();
  }

  public int getPort() {
    return port;
  }

  /**
   * Builder class for creating instances of {@link HttpServer}.
   */
  public static class Builder<T> {
    private int port;

    private Service service;
    private Serialization serialization;
    private AvaticaHandler handler = null;

    private MetricsSystemConfiguration<?> metricsConfig;

    private AuthenticationType authenticationType = AuthenticationType.NONE;

    private String kerberosPrincipal;
    private String kerberosRealm;
    private File keytab;

    private DoAsRemoteUserCallback remoteUserCallback;
    private RemoteUserExtractor remoteUserExtractor = new HttpRequestRemoteUserExtractor();

    private String loginServiceRealm;
    private String loginServiceProperties;
    private String[] loginServiceAllowedRoles;

    private boolean usingTLS = false;
    private File keystore;
    private String keystorePassword;
    private File truststore;
    private String truststorePassword;

    private List<ServerCustomizer<T>> serverCustomizers = Collections.emptyList();

    // The maximum size in bytes of an http header the server will read (64KB)
    private int maxAllowedHeaderSize = MAX_ALLOWED_HEADER_SIZE;

    public Builder() {}

    /**
     * Creates a typed Builder for Server customization.
     * @param <T> The type of HttpServer
     * @return A typed Builder
     */
    public static <T> Builder<T> newBuilder() {
      return new Builder<>();
    }

    public Builder<T> withPort(int port) {
      this.port = port;
      return this;
    }

    /**
     * Sets the {@link Service} and {@link Serialization} information necessary to construct
     * the appropriate {@link AvaticaHandler}.
     *
     * @param service The Avatica service
     * @param serialization The serialization method
     * @return <code>this</code>
     */
    public Builder<T> withHandler(Service service, Serialization serialization) {
      this.service = Objects.requireNonNull(service);
      this.serialization = Objects.requireNonNull(serialization);
      return this;
    }

    /**
     * Sets an {@link AvaticaHandler} directly on the builder. Most users will not want to use
     * this method and should instead use {@link #withHandler(Service, Serialization)}.
     *
     * @param handler The handler
     * @return <code>this</code>
     */
    public Builder<T> withHandler(AvaticaHandler handler) {
      this.handler = Objects.requireNonNull(handler);
      return this;
    }

    /**
     * Sets the given configuration to enable metrics collection in the server.
     *
     * @param metricsConfig Configuration object for metrics.
     * @return <code>this</code>
     */
    public Builder<T> withMetricsConfiguration(MetricsSystemConfiguration<?> metricsConfig) {
      this.metricsConfig = Objects.requireNonNull(metricsConfig);
      return this;
    }

    /**
     * Configures the server to use SPNEGO authentication. This method requires that the
     * <code>principal</code> contains the Kerberos realm. Invoking this method overrides any
     * previous call which configures authentication.
     *
     * @param principal A kerberos principal with the realm required.
     * @return <code>this</code>
     */
    public Builder<T> withSpnego(String principal) {
      return withSpnego(principal, (String[]) null);
    }

    /**
     * Configures the server to use SPNEGO authentication. This method requires that the
     * <code>principal</code> contains the Kerberos realm. Invoking this method overrides any
     * previous call which configures authentication. Invoking this method overrides any previous
     * call which configures authentication. By default, only principals from the server's realm are
     * permitted, but additional realms can be allowed using <code>additionalAllowedRealms</code>.
     *
     * @param principal A kerberos principal with the realm required.
     * @param additionalAllowedRealms Any additional realms, other than the server's realm, which
     *    should be allowed to authenticate against the server. Can be null.
     * @return <code>this</code>
     */
    public Builder<T> withSpnego(String principal, String[] additionalAllowedRealms) {
      int index = Objects.requireNonNull(principal).lastIndexOf('@');
      if (-1 == index) {
        throw new IllegalArgumentException("Could not find '@' symbol in '" + principal
            + "' to parse the Kerberos realm from the principal");
      }
      final String realm = principal.substring(index + 1);
      return withSpnego(principal, realm, additionalAllowedRealms);
    }

    /**
     * Configures the server to use SPNEGO authentication. It is required that callers are logged
     * in via Kerberos already or have provided the necessary configuration to automatically log
     * in via JAAS (using the <code>java.security.auth.login.config</code> system property) before
     * starting the {@link HttpServer}. Invoking this method overrides any previous call which
     * configures authentication.
     *
     * @param principal The kerberos principal
     * @param realm The kerberos realm
     * @return <code>this</code>
     */
    public Builder<T> withSpnego(String principal, String realm) {
      return this.withSpnego(principal, realm, null);
    }

    /**
     * Configures the server to use SPNEGO authentication. It is required that callers are logged
     * in via Kerberos already or have provided the necessary configuration to automatically log
     * in via JAAS (using the <code>java.security.auth.login.config</code> system property) before
     * starting the {@link HttpServer}. Invoking this method overrides any previous call which
     * configures authentication. By default, only principals from the server's realm are permitted,
     * but additional realms can be allowed using <code>additionalAllowedRealms</code>.
     *
     * @param principal The kerberos principal
     * @param realm The kerberos realm
     * @param additionalAllowedRealms Any additional realms, other than the server's realm, which
     *    should be allowed to authenticate against the server. Can be null.
     * @return <code>this</code>
     */
    public Builder<T> withSpnego(String principal, String realm, String[] additionalAllowedRealms) {
      this.authenticationType = AuthenticationType.SPNEGO;
      this.kerberosPrincipal = Objects.requireNonNull(principal);
      this.kerberosRealm = Objects.requireNonNull(realm);
      this.loginServiceAllowedRoles = additionalAllowedRealms;
      return this;
    }

    /**
     * Sets a keytab to be used to perform a Kerberos login automatically (without the use of JAAS).
     *
     * @param keytab A KeyTab file for the server's login.
     * @return <code>this</code>
     */
    public Builder<T> withAutomaticLogin(File keytab) {
      this.keytab = Objects.requireNonNull(keytab);
      return this;
    }

    /**
     * Sets a callback implementation to defer the logic on how to run an action as a given user and
     * if the action should be permitted for that user.
     *
     * @param remoteUserCallback User-provided implementation of the callback
     * @return <code>this</code>
     */
    public Builder<T> withImpersonation(DoAsRemoteUserCallback remoteUserCallback) {
      this.remoteUserCallback = Objects.requireNonNull(remoteUserCallback);
      return this;
    }

    /**
     * Sets a callback implementation to defer the logic on how to use the right remoteUserExtractor
     * to extract remote user.
     *
     * @param remoteUserExtractor User-provided remoteUserExtractor
     * @return <code>this</code>
     */

    public Builder withRemoteUserExtractor(RemoteUserExtractor remoteUserExtractor) {
      this.remoteUserExtractor = Objects.requireNonNull(remoteUserExtractor);
      return this;
    }


    /**
     * Configures the server to use HTTP Basic authentication. The <code>properties</code> must
     * be in a form consumable by Jetty. Invoking this method overrides any previous call which
     * configures authentication. This authentication is supplementary to the JDBC-provided user
     * authentication interfaces and should only be used when those interfaces are not used.
     *
     * @param properties Location of a properties file parseable by Jetty which contains users and
     *     passwords.
     * @param allowedRoles An array of allowed roles in the properties file
     * @return <code>this</code>
     */
    public Builder<T> withBasicAuthentication(String properties, String[] allowedRoles) {
      return withAuthentication(AuthenticationType.BASIC, properties, allowedRoles);
    }

    /**
     * Configures the server to use HTTP Digest authentication. The <code>properties</code> must
     * be in a form consumable by Jetty. Invoking this method overrides any previous call which
     * configures authentication. This authentication is supplementary to the JDBC-provided user
     * authentication interfaces and should only be used when those interfaces are not used.
     *
     * @param properties Location of a properties file parseable by Jetty which contains users and
     *     passwords.
     * @param allowedRoles An array of allowed roles in the properties file
     * @return <code>this</code>
     */
    public Builder<T> withDigestAuthentication(String properties, String[] allowedRoles) {
      return withAuthentication(AuthenticationType.DIGEST, properties, allowedRoles);
    }

    private Builder<T> withAuthentication(AuthenticationType authType, String properties,
        String[] allowedRoles) {
      this.loginServiceRealm = "Avatica";
      this.authenticationType = authType;
      this.loginServiceProperties = Objects.requireNonNull(properties);
      this.loginServiceAllowedRoles = Objects.requireNonNull(allowedRoles);
      return this;
    }

    /**
     * Configures the server to use TLS for wire encryption.
     *
     * @param keystore The server's keystore
     * @param keystorePassword The keystore's password
     * @param truststore The truststore containing the key used to generate the server's key
     * @param truststorePassword The truststore's password
     * @return <code>this</code>
     */
    public Builder<T> withTLS(File keystore, String keystorePassword, File truststore,
        String truststorePassword) {
      this.usingTLS = true;
      this.keystore = Objects.requireNonNull(keystore);
      this.keystorePassword = Objects.requireNonNull(keystorePassword);
      this.truststore = Objects.requireNonNull(truststore);
      this.truststorePassword = Objects.requireNonNull(truststorePassword);
      return this;
    }

    /**
     * Adds customizers to configure a Server before startup.
     *
     * @param serverCustomizers The customizers to use
     * @param clazz The type of server to customize
     * @return <code>this</code>
     */
    public Builder<T> withServerCustomizers(List<ServerCustomizer<T>> serverCustomizers,
        Class<T> clazz) {
      Objects.requireNonNull(clazz);
      if (!clazz.isAssignableFrom(Server.class)) {
        throw new IllegalArgumentException("Only Jetty Server customizers are supported");
      }
      this.serverCustomizers = Objects.requireNonNull(serverCustomizers);
      return this;
    }

    /**
     * Configures the maximum size, in bytes, of an HTTP header that the server will read.
     *
     * @param maxHeaderSize Maximums HTTP header size in bytes
     * @return <code>this</code>
     */
    public Builder<T> withMaxHeaderSize(int maxHeaderSize) {
      this.maxAllowedHeaderSize = maxHeaderSize;
      return this;
    }

    /**
     * Builds the HttpServer instance from <code>this</code>.
     * @return An HttpServer.
     */
    @SuppressWarnings("unchecked")
    public HttpServer build() {
      final AvaticaServerConfiguration serverConfig;
      final Subject subject;
      switch (authenticationType) {
      case NONE:
        serverConfig = null;
        subject = null;
        break;
      case BASIC:
      case DIGEST:
        // Build the configuration for BASIC or DIGEST authentication.
        serverConfig = buildUserAuthenticationConfiguration(this);
        subject = null;
        break;
      case SPNEGO:
        if (usingTLS) {
          throw new IllegalArgumentException("TLS has not been tested wtih SPNEGO");
        }
        if (null != keytab) {
          LOG.debug("Performing Kerberos login with {} as {}", keytab, kerberosPrincipal);
          subject = loginViaKerberos(this);
        } else {
          LOG.debug("Not performing Kerberos login");
          subject = null;
        }
        serverConfig = buildSpnegoConfiguration(this);
        break;
      default:
        throw new IllegalArgumentException("Unhandled AuthenticationType");
      }

      AvaticaHandler handler = buildHandler(this, serverConfig);
      SslContextFactory sslFactory = null;
      if (usingTLS) {
        sslFactory = new SslContextFactory();
        sslFactory.setKeyStorePath(this.keystore.getAbsolutePath());
        sslFactory.setKeyStorePassword(keystorePassword);
        sslFactory.setTrustStorePath(truststore.getAbsolutePath());
        sslFactory.setTrustStorePassword(truststorePassword);
      }

      List<ServerCustomizer<Server>> jettyCustomizers = new ArrayList<>();
      for (ServerCustomizer<?> customizer : this.serverCustomizers) {
        // Type checked in withServerCustomizers
        jettyCustomizers.add((ServerCustomizer<Server>) customizer);
      }

      return new HttpServer(port, handler, serverConfig, subject, sslFactory, jettyCustomizers,
          maxAllowedHeaderSize);
    }

    /**
     * Creates the appropriate {@link AvaticaHandler}.
     *
     * @param b The {@link Builder}.
     * @param config The Avatica server configuration
     * @return An {@link AvaticaHandler}.
     */
    private AvaticaHandler buildHandler(Builder b, AvaticaServerConfiguration config) {
      // The user provided a handler explicitly.
      if (null != b.handler) {
        return b.handler;
      }

      // Normal case, we create the handler for the user.
      HandlerFactory factory = new HandlerFactory();
      return factory.getHandler(b.service, b.serialization, b.metricsConfig, config);
    }

    /**
     * Builds an {@link AvaticaServerConfiguration} implementation for SPNEGO-based authentication.
     * @param b The {@link Builder}.
     * @return A configuration instance.
     */
    private AvaticaServerConfiguration buildSpnegoConfiguration(Builder b) {
      final String principal = b.kerberosPrincipal;
      final String realm = b.kerberosRealm;
      final String[] additionalAllowedRealms = b.loginServiceAllowedRoles;
      final DoAsRemoteUserCallback callback = b.remoteUserCallback;
      final RemoteUserExtractor remoteUserExtractor = b.remoteUserExtractor;
      return new AvaticaServerConfiguration() {

        @Override public AuthenticationType getAuthenticationType() {
          return AuthenticationType.SPNEGO;
        }

        @Override public String getKerberosRealm() {
          return realm;
        }

        @Override public String getKerberosPrincipal() {
          return principal;
        }

        @Override public boolean supportsImpersonation() {
          return null != callback;
        }

        @Override public <T> T doAsRemoteUser(String remoteUserName, String remoteAddress,
            Callable<T> action) throws Exception {
          return callback.doAsRemoteUser(remoteUserName, remoteAddress, action);
        }

        @Override public RemoteUserExtractor getRemoteUserExtractor() {
          return remoteUserExtractor;
        }

        @Override public String[] getAllowedRoles() {
          return additionalAllowedRealms;
        }

        @Override public String getHashLoginServiceRealm() {
          return null;
        }

        @Override public String getHashLoginServiceProperties() {
          return null;
        }
      };
    }

    private AvaticaServerConfiguration buildUserAuthenticationConfiguration(Builder b) {
      final AuthenticationType authType = b.authenticationType;
      final String[] allowedRoles = b.loginServiceAllowedRoles;
      final String realm = b.loginServiceRealm;
      final String properties = b.loginServiceProperties;
      final RemoteUserExtractor remoteUserExtractor = b.remoteUserExtractor;

      return new AvaticaServerConfiguration() {
        @Override public AuthenticationType getAuthenticationType() {
          return authType;
        }

        @Override public String[] getAllowedRoles() {
          return allowedRoles;
        }

        @Override public String getHashLoginServiceRealm() {
          return realm;
        }

        @Override public String getHashLoginServiceProperties() {
          return properties;
        }

        // Unused

        @Override public String getKerberosRealm() {
          return null;
        }

        @Override public String getKerberosPrincipal() {
          return null;
        }

        @Override public boolean supportsImpersonation() {
          return false;
        }

        @Override public <T> T doAsRemoteUser(String remoteUserName, String remoteAddress,
            Callable<T> action) throws Exception {
          return null;
        }

        @Override public RemoteUserExtractor getRemoteUserExtractor() {
          return remoteUserExtractor;
        }
      };
    }

    private Subject loginViaKerberos(Builder b) {
      Set<Principal> principals = new HashSet<Principal>();
      principals.add(new KerberosPrincipal(b.kerberosPrincipal));

      Subject subject = new Subject(false, principals, new HashSet<Object>(),
          new HashSet<Object>());

      ServerKeytabJaasConf conf = new ServerKeytabJaasConf(b.kerberosPrincipal,
          b.keytab.toString());
      String confName = "NotUsed";
      try {
        LoginContext loginContext = new LoginContext(confName, subject, null, conf);
        loginContext.login();
        return loginContext.getSubject();
      } catch (LoginException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

// End HttpServer.java
