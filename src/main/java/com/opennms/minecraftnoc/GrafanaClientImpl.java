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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.json.*;

public class GrafanaClientImpl {
    private final OkHttpClient client;
    private final HttpUrl grafanaBaseUrl;
    private final String apiKey;
    private final String theme;
    private final MinecraftNOC plugin;
    private String defaultDashboardPath;
    private String defaultDashboardName;

    public GrafanaClientImpl(MinecraftNOC main) {
        FileConfiguration config = main.getConfig();
        grafanaBaseUrl = HttpUrl.parse(Objects.requireNonNull(config.get("grafana.baseurl")).toString());
        apiKey = Objects.requireNonNull(config.get("grafana.apikey")).toString();
        theme = config.getString("grafana.theme", "dark"); // everything's better in the dark ^_^

        Map<String, Object> boards = config.getConfigurationSection("dashboards").getValues(false);
        boards.forEach((k,v) -> {
            ConfigurationSection c = (ConfigurationSection)v;
            if (Objects.requireNonNull(c.get("type")).toString().equalsIgnoreCase("grafana")
                    && c.getBoolean("default", false)) {
                defaultDashboardPath = c.getString("path");
                defaultDashboardName = k;
                main.getLogger().log(Level.INFO, "Using default dashboard '" + k + "'");
            }
        });

        plugin = main;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        builder = configureToIgnoreCertificate(builder);
        client = builder.build();
    }

    public void setDashboard(String dashboardName, String dashboardPath) {
        defaultDashboardPath = dashboardPath;
        defaultDashboardName = dashboardName;
    }

    public String getCurrentDashboardName() { return defaultDashboardName; }
    public String getCurrentDashboardPath() { return defaultDashboardPath; }

    public CompletableFuture<BufferedImage> renderPngForPanel(Location loc, MapView map, String dashboardName, String panelId) {
        return renderPngForPanel(loc, dashboardName, panelId, 1, 1);
    }

    public CompletableFuture<BufferedImage> renderPngForPanel(Location loc, String dashboardName, String panelId, int width, int height) {
        final HttpUrl.Builder builder = grafanaBaseUrl.newBuilder();

        if(dashboardName == null) {
            builder.addPathSegments(defaultDashboardPath); // use the currently selected one
        } else {
            String path = plugin.getConfig().getString("dashboards." + dashboardName + ".path");
            builder.addPathSegments(path);
        }

        // Query parameters
        builder.addQueryParameter("panelId", panelId)
                .addQueryParameter("width", String.valueOf(128*width))
                .addQueryParameter("height", String.valueOf(128*height))
                // Set a render timeout equal to the client's read timeout
                .addQueryParameter("timeout", Integer.toString(10))
                .addQueryParameter("theme", theme);

        final Request request = new Request.Builder()
                .url(builder.build())
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .build();

        final CompletableFuture<BufferedImage> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }

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
                            BufferedImage img = future.get();
                            splitImageToMaps(img, loc);
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

    private void splitImageToMaps(BufferedImage img, Location topleft) {
        ConfigurationSection images = plugin.getConfig().getConfigurationSection("images");
        ConfigurationSection c = images.getConfigurationSection(topleft.getBlockX()+","+topleft.getBlockY()+","+topleft.getBlockZ());
        List<Integer> mapIds = c.getIntegerList("maps");
        final int width = c.getInt("width", 1);
        final int height = c.getInt("height", 1);
        plugin.getLogger().log(Level.WARNING, "image width:"+width+" height:"+height);

        NOCMapRenderer renderer = plugin.getMapRenderer();

        int mapindex = 0;
        for (int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                MapView map = plugin.getMap(mapIds.get(mapindex));
                mapindex++;

                BufferedImage tile = MapPalette.resizeImage(img.getSubimage(x * 128, y * 128, 128, 128));
                renderer.setMapImage(map.getId(), tile);
                renderer.applyToMap(map);
            }
        }
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
