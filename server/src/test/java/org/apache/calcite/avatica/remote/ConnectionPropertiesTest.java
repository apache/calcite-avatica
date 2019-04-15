package org.apache.calcite.avatica.remote;

import com.google.common.cache.Cache;
import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.AvaticaUtils;
import org.apache.calcite.avatica.ConnectionSpec;
import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.server.HttpServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class ConnectionPropertiesTest {
    private static final AvaticaServersForTest SERVERS = new AvaticaServersForTest();
    private static final Random RANDOM = new Random();

    private final HttpServer server;
    private final String url;
    private final int port;
    private final Driver.Serialization serialization;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() throws Exception {
        Properties prop = new Properties();
        prop.put(JdbcMeta.ConnectionCacheSettings.EXPIRY_DURATION.key(), "1");
        prop.put(JdbcMeta.ConnectionCacheSettings.EXPIRY_UNIT.key(), TimeUnit.SECONDS.name());
        SERVERS.startServers(prop);
        return SERVERS.getJUnitParameters();
    }

    public ConnectionPropertiesTest(Driver.Serialization serialization, HttpServer server) {
        this.server = server;
        this.port = this.server.getPort();
        this.serialization = serialization;
        this.url = SERVERS.getJdbcUrl(port, serialization);
    }

    @Test
    public void testConnectionPropertiesSync() throws Exception {
        ConnectionSpec.getDatabaseLock().lock();
        try {
            AvaticaConnection conn = (AvaticaConnection) DriverManager.getConnection(url);
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            // sync connection properties
            conn.createStatement();
            Connection remoteConn = getConnection(AvaticaServersForTest.FullyRemoteJdbcMetaFactory.getInstance(), conn.id);

            assertFalse(remoteConn.getAutoCommit());
            assertEquals(remoteConn.getTransactionIsolation(), Connection.TRANSACTION_REPEATABLE_READ);

            // after 1s, remote connection expired and reopen
            Thread.sleep(1000);

            conn.createStatement();
            Connection remoteConn1 = getConnection(AvaticaServersForTest.FullyRemoteJdbcMetaFactory.getInstance(), conn.id);

            assertFalse(remoteConn1.getAutoCommit());
            assertEquals(remoteConn1.getTransactionIsolation(), Connection.TRANSACTION_REPEATABLE_READ);

        } finally {
            ConnectionSpec.getDatabaseLock().unlock();
        }
    }

    private static Connection getConnection(JdbcMeta m, String id) throws Exception {
        Field f = JdbcMeta.class.getDeclaredField("connectionCache");
        f.setAccessible(true);
        //noinspection unchecked
        Cache<String, Connection> connectionCache = (Cache<String, Connection>) f.get(m);
        return connectionCache.getIfPresent(id);
    }
}
