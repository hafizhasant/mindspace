package com.mindspace.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class Models {
    static final class Entry {
        final String name;
        final String backend;
        final String endpoint;

        Entry(String name, String backend, String endpoint) {
            this.name = name;
            this.backend = backend;
            this.endpoint = endpoint;
        }

        String label() {
            return name.substring(name.lastIndexOf('/') + 1) + "  ·  " + backend;
        }
    }

    static List<Entry> scan(String host) {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray data = new JSONObject(get("http://" + host + ":8080/v1/models")).optJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    out.add(new Entry(data.getJSONObject(i).optString("id"), "llama.cpp",
                            "http://" + host + ":8080/v1/chat/completions"));
                }
            }
        } catch (Exception ignored) {
        }
        try {
            JSONArray models = new JSONObject(get("http://" + host + ":11434/api/tags")).optJSONArray("models");
            if (models != null) {
                for (int i = 0; i < models.length(); i++) {
                    out.add(new Entry(models.getJSONObject(i).optString("name"), "Ollama",
                            "http://" + host + ":11434/v1/chat/completions"));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String get(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(10000);
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }
}
