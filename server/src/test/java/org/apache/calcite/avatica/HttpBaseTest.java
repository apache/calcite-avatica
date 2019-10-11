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
package org.apache.calcite.avatica;

import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.server.HttpServer;
import org.apache.calcite.avatica.util.DateTimeUtils;

import org.apache.kerby.kerberos.kerb.KrbException;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

/**
 * Common Base for HTTP End2End Tests
 */
public abstract class HttpBaseTest {
  protected static final Logger LOG = LoggerFactory.getLogger(HttpBaseTest.class);

  protected static final String KEYSTORE_PASSWORD = "avaticasecret";
  protected static final String KEYSTORE_EMPTY_PASSWORD = "";
  protected static final ConnectionSpec CONNECTION_SPEC = ConnectionSpec.HSQLDB;
  protected static final List<HttpServer> SERVERS_TO_STOP = new ArrayList<>();

  protected static final String TARGET_DIR_NAME = System.getProperty("target.dir", "target");
  protected static final File TARGET_DIR =
          new File(System.getProperty("user.dir"), TARGET_DIR_NAME);
  protected static final File KEYSTORE = new File(TARGET_DIR, "avatica-test.jks");
  protected static final File EMPTY_PW_KEYSTORE = new File(TARGET_DIR, "avatica-test-emptypw.jks");

  protected static LocalService localService;

  protected final String jdbcUrl;

  public static void setupClass() throws SQLException {
    // Create a self-signed cert
    if (KEYSTORE.isFile()) {
      assertTrue("Failed to delete keystore: " + KEYSTORE, KEYSTORE.delete());
    }
    new CertTool().createSelfSignedCert(KEYSTORE, "avatica", KEYSTORE_PASSWORD);

    if (EMPTY_PW_KEYSTORE.isFile()) {
      assertTrue("Failed to delete keystore: " + EMPTY_PW_KEYSTORE, EMPTY_PW_KEYSTORE.delete());
    }
    new CertTool().createSelfSignedCert(EMPTY_PW_KEYSTORE, "avatica", KEYSTORE_EMPTY_PASSWORD);

    // Create a LocalService around HSQLDB
    JdbcMeta jdbcMeta;
    jdbcMeta = new JdbcMeta(CONNECTION_SPEC.url,
        CONNECTION_SPEC.username, CONNECTION_SPEC.password);
    localService = new LocalService(jdbcMeta);
  }

  @AfterClass public static void stopServers() throws KrbException {
    for (HttpServer server : SERVERS_TO_STOP) {
      server.stop();
    }
    SERVERS_TO_STOP.clear();
  }

  @Before public void checkUrl() {
    //We signal that we skip the test because of the IBM Java issue by specifying a Null URL
    assumeNotNull(jdbcUrl);
  }

  public HttpBaseTest(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  /**
   * Utility class for creating certificates for testing.
   */
  private static class CertTool {
    private static final String SIGNING_ALGORITHM = "SHA256WITHRSA";
    private static final String ENC_ALGORITHM = "RSA";

    static {
      Security.addProvider(new BouncyCastleProvider());
    }

    private void createSelfSignedCert(File targetKeystore, String keyName,
        String keystorePassword) {
      if (targetKeystore.exists()) {
        throw new RuntimeException("Keystore already exists: " + targetKeystore);
      }

      try {
        KeyPair kp = generateKeyPair();

        X509Certificate cert = generateCert(keyName, kp, true, kp.getPublic(),
            kp.getPrivate());

        char[] password = keystorePassword.toCharArray();
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, null);
        keystore.setCertificateEntry(keyName + "Cert", cert);
        keystore.setKeyEntry(keyName + "Key", kp.getPrivate(), password, new Certificate[] {cert});
        try (FileOutputStream fos = new FileOutputStream(targetKeystore)) {
          keystore.store(fos, password);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
      KeyPairGenerator gen = KeyPairGenerator.getInstance(ENC_ALGORITHM);
      gen.initialize(2048);
      return gen.generateKeyPair();
    }

    private X509Certificate generateCert(String keyName, KeyPair kp, boolean isCertAuthority,
                                         PublicKey signerPublicKey, PrivateKey signerPrivateKey)
        throws IOException, OperatorCreationException, CertificateException,
        NoSuchAlgorithmException {
      Calendar startDate = DateTimeUtils.calendar();
      Calendar endDate = DateTimeUtils.calendar();
      endDate.add(Calendar.YEAR, 100);

      BigInteger serialNumber = BigInteger.valueOf(startDate.getTimeInMillis());
      X500Name issuer = new X500Name(
          IETFUtils.rDNsFromString("cn=localhost", RFC4519Style.INSTANCE));
      JcaX509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(issuer,
          serialNumber, startDate.getTime(), endDate.getTime(), issuer, kp.getPublic());
      JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
      certGen.addExtension(Extension.subjectKeyIdentifier, false,
          extensionUtils.createSubjectKeyIdentifier(kp.getPublic()));
      certGen.addExtension(Extension.basicConstraints, false,
          new BasicConstraints(isCertAuthority));
      certGen.addExtension(Extension.authorityKeyIdentifier, false,
          extensionUtils.createAuthorityKeyIdentifier(signerPublicKey));
      if (isCertAuthority) {
        certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
      }
      X509CertificateHolder certificateHolder = certGen.build(
          new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(signerPrivateKey));
      return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }
  }
}

// End HttpBaseTest.java
