/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package com.opennms.minecraftnoc;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.json.*;

public class GrafanaClientImpl {
    private final OkHttpClient client;
    private final HttpUrl grafanaBaseUrl;
    private final String dashboardUid;
    private final String apiKey;
    private final MinecraftNOC plugin;
    private final String pngWidth;
    private final String pngHeight;

    public GrafanaClientImpl(MinecraftNOC main) {
        FileConfiguration config = main.getConfig();
        grafanaBaseUrl = HttpUrl.parse(Objects.requireNonNull(config.get("grafana.baseurl")).toString());
        apiKey = Objects.requireNonNull(config.get("grafana.apikey")).toString();
        pngWidth = Objects.requireNonNull(config.get("grafana.pngwidth")).toString();
        pngHeight = Objects.requireNonNull(config.get("grafana.pngheight")).toString();
        dashboardUid = Objects.requireNonNull(config.get("grafana.dashboard")).toString();
        plugin = main;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS);
        builder = configureToIgnoreCertificate(builder);
        client = builder.build();
    }

    public CompletableFuture<BufferedImage> renderPngForPanel(Location loc, MapView map, String panelId) {
        final HttpUrl.Builder builder = grafanaBaseUrl.newBuilder()
                .addPathSegments(dashboardUid);

        // Query parameters
        builder.addQueryParameter("panelId", panelId)
                .addQueryParameter("width", pngWidth)
                .addQueryParameter("height", pngHeight)
                // Set a render timeout equal to the client's read timeout
                .addQueryParameter("timeout", Integer.toString(10))
                .addQueryParameter("theme", "dark"); // everything's better in the dark ^_^

        final Request request = new Request.Builder()
                .url(builder.build())
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .build();

        int X = loc.getBlockX();
        int Y = loc.getBlockY();
        int Z = loc.getBlockZ();
        String pos = X + "," + Y + "," + Z;

        String path = "images." + pos;
        FileConfiguration config = plugin.getConfig();
        config.set(path + ".panel", panelId);
        config.set(path + ".map", map.getId());
        plugin.saveConfig();

        plugin.getMapRenderer().applyToMap(map);

        final CompletableFuture<BufferedImage> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        try {
                            future.completeExceptionally(new IOException("Request failed: " + extractMessageFromErrorResponse(response)));
                        } catch (IOException e) {
                            future.completeExceptionally(new IOException("Could not extract message from error response", e));
                        }
                    }

                    try (InputStream is = responseBody.byteStream()) {
                        future.complete(ImageIO.read(is));
                        try {
                            BufferedImage img = MapPalette.resizeImage(future.get());
                            plugin.getMapRenderer().setMapImage(map.getId(), img);
                        } catch(InterruptedException | ExecutionException e) {
                            Bukkit.getLogger().log(Level.SEVERE, e.getLocalizedMessage());
                        }
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    }
                }
            }
        });
        return future;
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
