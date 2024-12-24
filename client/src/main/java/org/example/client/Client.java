package org.example.client;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Client {

    public void postRequestApache() throws Exception {
        // ssl setting
        // load keystore and truststore because of mTLS
        final SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(new File("D:\\ssl_test2_file\\clientkeystore.p12"), "export3".toCharArray(), "export3".toCharArray())
                .loadTrustMaterial(new File("D:\\ssl_test2_file\\truststore.p12"), "export2".toCharArray())
                .build();

        // Configure TLS strategy
        final TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

        // Create connection manager with TLS settings
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .setDefaultTlsConfig(TlsConfig.custom()
                        .setHandshakeTimeout(Timeout.ofSeconds(30))
                        .setSupportedProtocols(TLS.V_1_3)
                        .build())
                .build();

        // Create HTTP client
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .build()) {

            // Configure POST request
            final HttpPost httppost = new HttpPost("https://localhost:8080/api/v1/mtls/connect");
            httppost.setEntity(new StringEntity("{\"key\": \"value\"}", ContentType.parse("UTF-8")));
            httppost.setHeader("Content-Type", "application/json");

            System.out.println("Executing request " + httppost.getMethod() + " " + httppost.getUri());

            // Execute request
            final HttpClientContext clientContext = HttpClientContext.create();
            httpclient.execute(httppost, clientContext, response -> {
                System.out.println("----------------------------------------");
                System.out.println(httppost + "->" + new StatusLine(response));

                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Response -> " + responseBody);

                EntityUtils.consume(response.getEntity());

                // Retrieve and log SSL session details
                final SSLSession sslSession = clientContext.getSSLSession();
                if (sslSession != null) {
                    System.out.println("SSL protocol: " + sslSession.getProtocol());
                    System.out.println("SSL cipher suite: " + sslSession.getCipherSuite());
                }
                return null;
            });
        }
    }

    public void postRequestOKHttp() throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        //trust : rootCA
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        FileInputStream certInputStream = new FileInputStream("D:\\ssl_test2_file\\rootCA.pem");
        X509Certificate caCertificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);


        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null); // 初始化空的 KeyStore
        keyStore.setCertificateEntry("rootca", caCertificate);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        //client certificate
        keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream("D:\\ssl_test2_file\\clientkeystore.p12")) {
            keyStore.load(fis, "export3".toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "export3".toCharArray());

        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();


        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .hostnameVerifier((hostname, session) -> "localhost".equals(hostname))
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                .build();

        RequestBody requestBody = RequestBody.create(
                "", MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .header("Content-Type", "multipart/form-data")
                .url("https://localhost:8080/api/v1/mtls/connect")
                .post(requestBody)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            System.out.println(response.protocol());
            System.out.println(response.body().string());
        }
    }
}

