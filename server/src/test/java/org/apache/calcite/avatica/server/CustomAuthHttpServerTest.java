package org.apache.calcite.avatica.server;

import org.apache.calcite.avatica.ConnectionSpec;
import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.remote.AuthenticationType;
import org.apache.calcite.avatica.remote.Driver;
import org.apache.calcite.avatica.remote.LocalService;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CustomAuthHttpServerTest extends HttpAuthBase {
    private static final Logger LOG =
            LoggerFactory.getLogger(CustomAuthHttpServerTest.class);
    private static final ConnectionSpec CONNECTION_SPEC = ConnectionSpec.HSQLDB;
    private static HttpServer server;
    private static String url;

    private static int methodCallCounter1 = 0;
    private static int methodCallCounter2 = 0;
    private static int methodCallCounter3 = 0;

    @Before
    public void before() throws SQLException {
        methodCallCounter1 = 0;
        methodCallCounter2 = 0;
        methodCallCounter3 = 0;
    }

    @Test
    public void testCustomImpersonationConfig() throws Exception {
        final JdbcMeta jdbcMeta = new JdbcMeta(CONNECTION_SPEC.url,
                CONNECTION_SPEC.username, CONNECTION_SPEC.password);
        LocalService service = new LocalService(jdbcMeta);

        server = new HttpServer.Builder()
                .withCustomAuthentication(new CustomImpersonationConfig())
                .withHandler(service, Driver.Serialization.PROTOBUF)
                .withPort(0)
                .build();
        server.start();
        // Create and grant permissions to our users
        createHsqldbUsers();

        url = "jdbc:avatica:remote:url=http://localhost:" + server.getPort()
                + ";authentication=BASIC;serialization=PROTOBUF";

        readWriteData(url, "CUSTOM_CONFIG_1_TABLE", new Properties());
        Assert.assertEquals("supportsImpersonation should be called same number of times as doAsRemoteUser method", methodCallCounter1, methodCallCounter2);
        Assert.assertEquals("supportsImpersonation should be called same number of times as getRemoteUserExtractor method", methodCallCounter1, methodCallCounter3);
    }

    // CustomImpersonationConfig doesn't authenticates the user but supports user impersonation
    static class CustomImpersonationConfig implements AvaticaServerConfiguration {

        @Override
        public AuthenticationType getAuthenticationType() {
            return AuthenticationType.CUSTOM;
        }

        @Override
        public String getKerberosRealm() {
            return null;
        }

        @Override
        public String getKerberosPrincipal() {
            return null;
        }

        @Override
        public String[] getAllowedRoles() {
            return new String[0];
        }

        @Override
        public String getHashLoginServiceRealm() {
            return null;
        }

        @Override
        public String getHashLoginServiceProperties() {
            return null;
        }

        @Override
        public boolean supportsImpersonation() {
            methodCallCounter1++;
            return true;
        }

        @Override
        public <T> T doAsRemoteUser(String remoteUserName, String remoteAddress, Callable<T> action) throws Exception {
            methodCallCounter2++;
            return action.call();
        }

        @Override
        public RemoteUserExtractor getRemoteUserExtractor() {
            return new RemoteUserExtractor() {
                @Override
                public String extract(HttpServletRequest request) throws RemoteUserExtractionException {
                    methodCallCounter3++;
                    return "randomUser";
                }
            };
        }
    }

    @Test
    public void testCustomBasicImpersonationConfigWithAllowedUser() throws Exception {
        createServerWithCustomBasicImpersonationConfig();

        final Properties props = new Properties();
        props.put("avatica_user", "USER2");
        props.put("avatica_password", "password2");
        props.put("user", "USER2");
        props.put("password", "password2");

        readWriteData(url, "CUSTOM_CONFIG_2_ALLOWED_TABLE", props);
        Assert.assertEquals("supportsImpersonation should be called same number of times as doAsRemoteUser method", methodCallCounter1, methodCallCounter2);
        Assert.assertEquals("supportsImpersonation should be called same number of times as getRemoteUserExtractor method", methodCallCounter1, methodCallCounter3);
    }

    @Test
    public void testCustomBasicImpersonationConfigWithDisallowedUser() throws Exception {
        createServerWithCustomBasicImpersonationConfig();

        final Properties props = new Properties();
        props.put("avatica_user", "USER1");
        props.put("avatica_password", "password1");
        props.put("user", "USER1");
        props.put("password", "password1");

        try {
            readWriteData(url, "CUSTOM_CONFIG_2_DISALLOWED_TABLE", props);
            fail("Expected an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Failed to execute HTTP Request, got HTTP/403"));
        }
    }

    @SuppressWarnings("unchecked") // needed for the mocked customizers, not the builder
    protected void createServerWithCustomBasicImpersonationConfig() throws SQLException {
        final JdbcMeta jdbcMeta = new JdbcMeta(CONNECTION_SPEC.url,
                CONNECTION_SPEC.username, CONNECTION_SPEC.password);
        LocalService service = new LocalService(jdbcMeta);

        AvaticaServerConfiguration configuration = new CustomBasicImpersonationConfig();
        BasicAuthCustomizer basicAuthCustomizer = new BasicAuthCustomizer(configuration);
        server = new HttpServer.Builder()
                .withCustomAuthentication(configuration)
                .withHandler(service, Driver.Serialization.PROTOBUF)
                .withPort(0)
                .withServerCustomizers(Arrays.asList(basicAuthCustomizer), Server.class)
                .build();
        server.start();

        // Create and grant permissions to our users
        createHsqldbUsers();
        url = "jdbc:avatica:remote:url=http://localhost:" + basicAuthCustomizer.getLocalPort()
                + ";authentication=BASIC;serialization=PROTOBUF";
    }

    // Customizer to add BasicAuthentication connector to the server
    static class BasicAuthCustomizer implements ServerCustomizer<Server> {

        AvaticaServerConfiguration configuration;
        ServerConnector connector;

        public BasicAuthCustomizer(AvaticaServerConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void customize(Server server) {
            HttpServer avaticaServer = getServer();
            connector = avaticaServer.configureConnector(avaticaServer.getConnector(), 0);
            server.setConnectors(new Connector[] { connector });
            ConstraintSecurityHandler securityHandler = avaticaServer.configureBasicAuthentication(server, connector, configuration);
            avaticaServer.configureHandlers(securityHandler);
        }

        public int getLocalPort() {
            return connector.getLocalPort();
        }

        public HttpServer getServer() {
            return server;
        }
    }

    // CustomBasicImpersonationConfig supports BasicAuthentication with user impersonation
    static class CustomBasicImpersonationConfig implements AvaticaServerConfiguration {

        @Override
        public AuthenticationType getAuthenticationType() {
            return AuthenticationType.CUSTOM;
        }

        @Override
        public String getKerberosRealm() {
            return null;
        }

        @Override
        public String getKerberosPrincipal() {
            return null;
        }

        @Override
        public String[] getAllowedRoles() {
            return new String[] { "users" };
        }

        @Override
        public String getHashLoginServiceRealm() {
            return "Avatica";
        }

        @Override
        public String getHashLoginServiceProperties() {
            return HttpAuthBase.getHashLoginServicePropertiesString();
        }

        @Override
        public boolean supportsImpersonation() {
            methodCallCounter1++;
            return true;
        }

        @Override
        public <T> T doAsRemoteUser(String remoteUserName, String remoteAddress, Callable<T> action) throws Exception {
            methodCallCounter2++;
            if (remoteUserName.equals("USER1")) {
                throw new RemoteUserDisallowedException("USER1 is a disallowed user!");
            }
            return action.call();
        }

        @Override
        public RemoteUserExtractor getRemoteUserExtractor() {
            return new RemoteUserExtractor() {
                @Override
                public String extract(HttpServletRequest request) throws RemoteUserExtractionException {
                    methodCallCounter3++;
                    if (request instanceof Request) {
                        Authentication authentication = ((Request) request).getAuthentication();
                        if(authentication instanceof UserAuthentication) {
                            return ((UserAuthentication) authentication).getUserIdentity().getUserPrincipal().getName();
                        }
                    }
                    throw new RemoteUserExtractionException("Request doesn't contain user credentials.");
                }
            };
        }
    }

    @After
    public void stopServer() throws Exception {
        if (null != server) {
            server.stop();
        }
    }

}
