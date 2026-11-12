package com.local.chat;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class Api {
    static final class Result {
        String reply = "";
        final List<String[]> sources = new ArrayList<>();
    }

    static Result chat(String llm, String searx, String model, List<Message> history) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "Today is " + LocalDate.now() + ". You have a web search tool. "
                        + "For recent events, news, results, weather or anything uncertain, call search first and include "
                        + "the year and full event name in the query. Answer only from search results, state the year for "
                        + "each fact, and list source URLs. If the results don't cover the question, say so."));
        for (Message m : history) {
            if (m.loading) continue;
            messages.put(new JSONObject()
                    .put("role", m.fromUser ? "user" : "assistant")
                    .put("content", m.text));
        }

        Result out = new Result();
        for (int round = 0; round < 4; round++) {
            JSONObject body = new JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("tools", tools())
                    .put("max_tokens", 1024);
            JSONObject msg = post(llm, body).getJSONArray("choices").getJSONObject(0).getJSONObject("message");
            JSONArray calls = msg.optJSONArray("tool_calls");
            if (calls == null || calls.length() == 0) {
                out.reply = msg.optString("content", "");
                return out;
            }
            messages.put(new JSONObject()
                    .put("role", "assistant")
                    .put("content", msg.optString("content", ""))
                    .put("tool_calls", calls));
            for (int i = 0; i < calls.length(); i++) {
                JSONObject call = calls.getJSONObject(i);
                Object raw = call.getJSONObject("function").opt("arguments");
                JSONObject args = raw instanceof JSONObject ? (JSONObject) raw : new JSONObject(String.valueOf(raw));
                String result = search(searx, args.optString("query", ""), out.sources);
                messages.put(new JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.optString("id", ""))
                        .put("content", result));
            }
        }
        out.reply = "The search loop hit its limit. Try a shorter question.";
        return out;
    }

    private static JSONArray tools() throws Exception {
        return new JSONArray().put(new JSONObject()
                .put("type", "function")
                .put("function", new JSONObject()
                        .put("name", "search")
                        .put("description", "Search the web")
                        .put("parameters", new JSONObject()
                                .put("type", "object")
                                .put("properties", new JSONObject()
                                        .put("query", new JSONObject().put("type", "string")))
                                .put("required", new JSONArray().put("query")))));
    }

    private static String search(String base, String query, List<String[]> sources) throws Exception {
        String sep = base.contains("?") ? "&" : "?";
        URL url = new URL(base + sep + "q=" + URLEncoder.encode(query, "UTF-8") + "&format=json");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        JSONArray results = new JSONObject(read(c)).optJSONArray("results");
        if (results == null) return "No results.";
        StringBuilder sb = new StringBuilder();
        int n = Math.min(5, results.length());
        for (int i = 0; i < n; i++) {
            JSONObject r = results.getJSONObject(i);
            String title = r.optString("title", "");
            String link = r.optString("url", "");
            sources.add(new String[]{title, link});
            sb.append(i + 1).append(". ").append(title).append('\n')
              .append(link).append('\n')
              .append(r.optString("content", "")).append("\n\n");
        }
        return sb.length() == 0 ? "No results." : sb.toString();
    }

    private static JSONObject post(String url, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(300000);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream out = c.getOutputStream()) {
            out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        return new JSONObject(read(c));
    }

    private static String read(HttpURLConnection c) throws IOException {
        InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}
