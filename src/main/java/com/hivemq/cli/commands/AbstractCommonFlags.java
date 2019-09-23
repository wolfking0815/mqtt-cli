/*
 * Copyright 2019 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.hivemq.cli.commands;

import com.hivemq.cli.converters.*;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pmw.tinylog.Logger;
import picocli.CommandLine;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractCommonFlags extends AbstractConnectRestrictionFlags implements Connect {

    private static final String DEFAULT_TLS_VERSION = "TLSv1.2";

    @Nullable
    private MqttClientSslConfig sslConfig;

    private final List<X509Certificate> certificates = new ArrayList<>();

    @CommandLine.Option(names = {"-u", "--user"}, description = "The username for authentication", order = 2)
    @Nullable
    private String user;

    @CommandLine.Option(names = {"-pw", "--password"}, arity = "0..1", interactive = true, converter = ByteBufferConverter.class, description = "The password for authentication", order = 2)
    @Nullable
    private ByteBuffer password;

    @CommandLine.Option(names = {"-k", "--keepAlive"}, converter = UnsignedShortConverter.class, description = "A keep alive of the client (in seconds) (default: 60)", order = 2)
    private @Nullable Integer keepAlive;

    @CommandLine.Option(names = {"-c", "--cleanStart"}, negatable = true, description = "Define a clean start for the connection (default: true)", order = 2)
    @Nullable
    private Boolean cleanStart;

    @CommandLine.Option(names = {"-s", "--secure"}, defaultValue = "false", description = "Use default ssl configuration if no other ssl options are specified (default: false)", order = 2)
    private boolean useSsl;

    @CommandLine.Option(names = {"--cafile"}, paramLabel = "FILE", converter = FileToCertificateConverter.class, description = "Path to a file containing trusted CA certificates to enable encrypted certificate based communication", order = 2)
    private void addCAFile(X509Certificate certificate) {
        certificates.add(certificate);
    }

    @CommandLine.Option(names = {"--capath"}, paramLabel = "DIR", converter = DirectoryToCertificateCollectionConverter.class, description = {"Path to a directory containing certificate files to import to enable encrypted certificate based communication"}, order = 2)
    private void addCACollection(Collection<X509Certificate> certs) {
        certificates.addAll(certs);
    }

    @CommandLine.Option(names = {"--ciphers"}, split = ":", description = "The client supported cipher suites list in IANA format separated with ':'", order = 2)
    @Nullable
    private Collection<String> cipherSuites;

    @CommandLine.Option(names = {"--tls-version"}, description = "The TLS protocol version to use (default: {'TLSv.1.2'})", order = 2)
    @Nullable
    private Collection<String> supportedTLSVersions;


    @CommandLine.ArgGroup(exclusive = false)
    @Nullable
    private ClientSideAuthentication clientSideAuthentication;

    private static class ClientSideAuthentication {

        @CommandLine.Option(names = {"--cert"}, required = true, converter = FileToCertificateConverter.class, description = "The client certificate to use for client side authentication", order = 2)
        @Nullable X509Certificate clientCertificate;

        @CommandLine.Option(names = {"--key"}, required = true, converter = FileToPrivateKeyConverter.class, description = "The path to the client private key for client side authentication", order = 2)
        @Nullable PrivateKey clientPrivateKey;
    }


    public void handleCommonOptions() {

        if (useBuiltSslConfig()) {
            try {
                buildSslConfig();
            }
            catch (Exception e) {
                if (isDebug()) {
                    Logger.debug("Failed to build ssl config: {}", e);
                }
                Logger.error("Failed to build ssl config: {}", e.getMessage());
                return;
            }
        }
    }


    private boolean useBuiltSslConfig() {
        return !certificates.isEmpty() ||
                cipherSuites != null ||
                supportedTLSVersions != null ||
                clientSideAuthentication != null ||
                useSsl;
    }

    private void buildSslConfig() throws Exception {

        // build trustManagerFactory for server side authentication and to enable tls
        TrustManagerFactory trustManagerFactory = null;
        if (!certificates.isEmpty()) {
            trustManagerFactory = buildTrustManagerFactory(certificates);
        }


        // build keyManagerFactory if clientSideAuthentication is used
        KeyManagerFactory keyManagerFactory = null;
        if (clientSideAuthentication != null) {
            keyManagerFactory = buildKeyManagerFactory(clientSideAuthentication.clientCertificate, clientSideAuthentication.clientPrivateKey);
        }

        // default to tlsv.2
        if (supportedTLSVersions == null) {
            supportedTLSVersions = new ArrayList<>();
            supportedTLSVersions.add(DEFAULT_TLS_VERSION);
        }

        sslConfig = MqttClientSslConfig.builder()
                .trustManagerFactory(trustManagerFactory)
                .keyManagerFactory(keyManagerFactory)
                .cipherSuites(cipherSuites)
                .protocols(supportedTLSVersions)
                .build();
    }


    private TrustManagerFactory buildTrustManagerFactory(final @NotNull Collection<X509Certificate> certCollection) throws Exception {

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);

        // add all certificates of the collection to the KeyStore
        int i = 1;
        for (final X509Certificate cert : certCollection) {
            final String alias = Integer.toString(i);
            ks.setCertificateEntry(alias, cert);
            i++;
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        trustManagerFactory.init(ks);

        return trustManagerFactory;
    }

    private KeyManagerFactory buildKeyManagerFactory(final @NotNull X509Certificate cert, final @NotNull PrivateKey key) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        ks.load(null, null);

        final Certificate[] certChain = new Certificate[1];
        certChain[0] = cert;
        ks.setKeyEntry("mykey", key, null, certChain);

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        keyManagerFactory.init(ks, null);

        return keyManagerFactory;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    @Override
    public String toString() {

        return "Connect{" +
                "key=" + getKey() +
                ", " + commonOptions() +
                '}';
    }


    public String commonOptions() {
        return super.toString() +
                ", user='" + user + '\'' +
                ", keepAlive=" + keepAlive +
                ", cleanStart=" + cleanStart +
                ", useSsl=" + useSsl +
                ", sslConfig=" + sslConfig +
                ", " + getWillOptions();
    }


    // GETTER AND SETTER

    public void setUseSsl(final boolean useSsl) {
        this.useSsl = useSsl;
    }

    @Nullable
    public String getUser() {
        return user;
    }

    public void setUser(final @Nullable String user) {
        this.user = user;
    }

    @Nullable
    public ByteBuffer getPassword() {
        return password;
    }

    public void setPassword(final @Nullable ByteBuffer password) {
        this.password = password;
    }

    @Nullable
    public Integer getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(final @Nullable Integer keepAlive) {
        this.keepAlive = keepAlive;
    }

    @Nullable
    public Boolean getCleanStart() {
        return cleanStart;
    }

    public void setCleanStart(final @Nullable Boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    @Nullable
    public MqttClientSslConfig getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(final @Nullable MqttClientSslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

}