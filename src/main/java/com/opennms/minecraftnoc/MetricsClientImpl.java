package com.opennms.minecraftnoc;

import com.google.gson.Gson;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MetricsClientImpl {
    private final Gson gson = new Gson();
    private final OkHttpClient client;
    private final HttpUrl metricBaseUrl;
    private final String apiKey;
    private final MinecraftNOC plugin;

    public MetricsClientImpl(MinecraftNOC main) {
        FileConfiguration config = main.getConfig();
        metricBaseUrl = HttpUrl.parse(config.get("metrics.baseurl").toString());
        apiKey = config.get("metrics.apikey").toString();
        plugin = main;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS);
        builder = configureToIgnoreCertificate(builder);
        client = builder.build();
    }

    public CompletableFuture<String> getMetric(String url) {
        final HttpUrl.Builder builder = metricBaseUrl.newBuilder()
                .addPathSegment(url == null ? "/" : url);

        final Request request = new Request.Builder()
                .url(builder.build())
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .build();

        Location loc = plugin.getCurrentBlock().getLocation();
        int X = loc.getBlockX();
        int Y = loc.getBlockY();
        int Z = loc.getBlockZ();
        String pos = X + "," + Y + "," + Z;

        String path = "signs." + pos;
        plugin.getConfig().set(path, url);
        plugin.saveConfig();

        final CompletableFuture<String> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if(!response.isSuccessful()) {
                        try {
                            future.completeExceptionally(new IOException("Request failed: " + extractMessageFromErrorResponse(response)));
                        } catch (IOException e) {
                            future.completeExceptionally(new IOException("Could not extract message from error response", e));
                        }
                    }

                    try (InputStream is = responseBody.byteStream()) {
                        Block block = plugin.getCurrentBlock();
                        future.complete(inputStreamToByteArray(is, block, plugin));
                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            Sign s = (Sign)block.getState();
                            try {
                                String buf = future.get();
                                s.setLine(0, buf.substring(0, Math.min(buf.length(), 15)));
                                s.update();
                            } catch(InterruptedException | ExecutionException e) {
                                Bukkit.getLogger().log(Level.SEVERE, e.getLocalizedMessage());
                            }
                        });
                    } catch (IOException e) {
                        Bukkit.getLogger().log(Level.SEVERE, e.getLocalizedMessage());
                        future.completeExceptionally(e);
                    }
                }
            }
        });

        return future;
    }

    private static String inputStreamToByteArray(InputStream is, Block block, MinecraftNOC plugin) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toString(Charset.defaultCharset());
    }

    private static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {
        try {

            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    private static String extractMessageFromErrorResponse(Response response) throws IOException {
        final String contentType = response.header("Content-Type");
        if (contentType.toLowerCase().contains("application/json")) {
            final JSONTokener tokener = new JSONTokener(response.body().string());
            final JSONObject json = new JSONObject(tokener);
            if (json.has("message")) {
                return json.getString("message");
            } else {
                return json.toString();
            }
        }
        return response.body().string();
    }
}
