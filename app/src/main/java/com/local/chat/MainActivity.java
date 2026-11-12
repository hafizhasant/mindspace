package com.local.chat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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

public class MainActivity extends Activity {
    private LinearLayout chat;
    private ScrollView scroll;
    private EditText input, llmUrl, searxUrl, modelName;
    private Button send;
    private final JSONArray history = new JSONArray();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(22, 22, 22));
        root.setPadding(24, 24, 24, 24);

        llmUrl = field("LLM URL", prefs.getString("llm", "http://192.168.1.4:8080/v1/chat/completions"));
        searxUrl = field("SearXNG URL", prefs.getString("searx", "http://192.168.1.4:8888/search"));
        modelName = field("Model", prefs.getString("model", "gemma-4-12b-it-Q4_K_M"));
        root.addView(llmUrl);
        root.addView(searxUrl);
        root.addView(modelName);

        scroll = new ScrollView(this);
        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(chat);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        input = new EditText(this);
        input.setHint("Ask anything");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        bar.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        send = new Button(this);
        send.setText("Send");
        send.setOnClickListener(v -> ask());
        bar.addView(send);
        root.addView(bar);

        setContentView(root);
        bubble("Ready. Ask a question.", false, null);
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextSize(12);
        e.setTextColor(Color.LTGRAY);
        return e;
    }

    private void ask() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        final String llm = llmUrl.getText().toString().trim();
        final String searx = searxUrl.getText().toString().trim();
        final String model = modelName.getText().toString().trim();
        prefs.edit().putString("llm", llm).putString("searx", searx).putString("model", model).apply();

        input.setText("");
        send.setEnabled(false);
        bubble(text, true, null);
        try {
            history.put(new JSONObject().put("role", "user").put("content", text));
        } catch (Exception ignored) { }

        new Thread(() -> {
            List<String[]> sources = new ArrayList<>();
            String reply;
            try {
                reply = runChat(llm, searx, model, sources);
            } catch (Exception e) {
                reply = "Error: " + e;
            }
            final String r = reply;
            runOnUiThread(() -> {
                bubble(r, false, sources);
                send.setEnabled(true);
            });
        }).start();
    }

    private String runChat(String llm, String searx, String model, List<String[]> sources) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "Today is " + LocalDate.now() + ". You have a web search tool. "
                        + "For recent events, news, results, weather or anything uncertain, call search first and include the year "
                        + "and full event name in the query. Answer only from search results, state the year for each fact, "
                        + "and list source URLs. If the results don't cover the question, say so."));
        for (int i = 0; i < history.length(); i++) messages.put(history.get(i));

        for (int round = 0; round < 4; round++) {
            JSONObject body = new JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("tools", tools())
                    .put("max_tokens", 1024);
            JSONObject msg = post(llm, body).getJSONArray("choices").getJSONObject(0).getJSONObject("message");
            JSONArray calls = msg.optJSONArray("tool_calls");
            if (calls == null || calls.length() == 0) {
                String content = msg.optString("content", "");
                history.put(new JSONObject().put("role", "assistant").put("content", content));
                return content;
            }
            messages.put(msg);
            for (int i = 0; i < calls.length(); i++) {
                JSONObject call = calls.getJSONObject(i);
                Object raw = call.getJSONObject("function").opt("arguments");
                JSONObject args = raw instanceof JSONObject ? (JSONObject) raw : new JSONObject(String.valueOf(raw));
                String result = search(searx, args.optString("query", ""), sources);
                messages.put(new JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.optString("id", ""))
                        .put("content", result));
            }
        }
        return "The search loop hit its limit. Try a shorter question.";
    }

    private JSONArray tools() throws Exception {
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

    private String search(String base, String query, List<String[]> sources) throws Exception {
        String url = base + (base.contains("?") ? "&" : "?")
                + "q=" + URLEncoder.encode(query, "UTF-8") + "&format=json";
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        JSONArray results = new JSONObject(read(c)).optJSONArray("results");
        if (results == null) return "No results.";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, results.length()); i++) {
            JSONObject r = results.getJSONObject(i);
            String title = r.optString("title");
            String link = r.optString("url");
            sources.add(new String[]{title, link});
            sb.append(i + 1).append(". ").append(title).append("\n")
              .append(link).append("\n").append(r.optString("content")).append("\n\n");
        }
        return sb.length() == 0 ? "No results." : sb.toString();
    }

    private JSONObject post(String url, JSONObject body) throws Exception {
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

    private String read(HttpURLConnection c) throws IOException {
        InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private void bubble(String text, boolean mine, List<String[]> sources) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setPadding(24, 18, 24, 18);
        t.setBackgroundColor(mine ? Color.rgb(45, 45, 45) : Color.rgb(31, 31, 31));
        t.setAutoLinkMask(Linkify.WEB_URLS);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        StringBuilder full = new StringBuilder(text);
        if (sources != null && !sources.isEmpty()) {
            full.append("\n\nSources:");
            for (String[] s : sources) {
                full.append("\n").append(s[0].isEmpty() ? s[1] : s[0]).append("\n").append(s[1]);
            }
        }
        t.setText(full.toString());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        lp.setMargins(0, 12, 0, 12);
        chat.addView(t, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }
}
