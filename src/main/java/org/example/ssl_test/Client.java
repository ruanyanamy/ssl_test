package org.example.ssl_test;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;

public class Client {

    public void postRequestClient() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.custom().build())
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

}
