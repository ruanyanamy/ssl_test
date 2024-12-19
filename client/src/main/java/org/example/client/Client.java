package org.example.client;


import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Client {

    public void postRequestApache() throws Exception {
        final PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setConnectionConfigResolver(route -> {
                    // Use different settings for all secure (TLS) connections
                    if (route.isSecure()) {
                        return ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMinutes(2))
                                .setSocketTimeout(Timeout.ofMinutes(2))
                                .setValidateAfterInactivity(TimeValue.ofMinutes(1))
                                .setTimeToLive(TimeValue.ofHours(1))
                                .build();
                    }
                    return ConnectionConfig.custom()
                            .setConnectTimeout(Timeout.ofMinutes(1))
                            .setSocketTimeout(Timeout.ofMinutes(1))
                            .setValidateAfterInactivity(TimeValue.ofSeconds(15))
                            .setTimeToLive(TimeValue.ofMinutes(15))
                            .build();
                })
                .setTlsConfigResolver(host -> {
                    // Use different settings for specific hosts
                    if (host.getSchemeName().equalsIgnoreCase("localhost")) {
                        return TlsConfig.custom()
                                .setSupportedProtocols(TLS.V_1_3)
                                .setHandshakeTimeout(Timeout.ofSeconds(10))
                                .build();
                    }
                    return TlsConfig.DEFAULT;
                })
                .build();

        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(new File("D:\\SSL_Test\\truststore.p12"), "export2".toCharArray())
                .build();


        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build())
        {
            // post request setting
            final HttpPost httppost = new HttpPost("https://localhost:8080/api/v1/mtls/connect");

            httpClient.execute(httppost , response -> {
                System.out.println("----------------------------------------");
                System.out.println(httppost + "->" + new StatusLine(response));

                final HttpEntity resEntity = response.getEntity();
                System.out.println("resEntity : "+resEntity);

                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(response.getEntity());  // 關閉entity資源(單個)
                return null;
            });
        }
    }

    public void postRequestOKHttp() throws IOException {
        final OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(
                "", MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .header("Content-Type", "multipart/form-data")
                .url("https://localhost:8080/api/v1/mtls/connect")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            System.out.println(response.protocol());
            System.out.println(response.body().string());
        }
    }
}

