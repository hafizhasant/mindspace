package com.local.chat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_HOST = "192.168.1.4";
    private static final String DEFAULT_SEARX = "http://192.168.1.4:8888/search";
    private static final String DEFAULT_ENDPOINT = "http://192.168.1.4:8080/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gemma-4-12b-it-Q4_K_M";

    private SharedPreferences prefs;
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView list;
    private EditText input;
    private MaterialButton sendBtn;
    private MaterialButton modelChip;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        list = findViewById(R.id.messages);
        input = findViewById(R.id.input);
        sendBtn = findViewById(R.id.send);
        modelChip = findViewById(R.id.model_chip);

        adapter = new MessageAdapter(this, messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);

        ((ImageButton) findViewById(R.id.btn_new)).setOnClickListener(v -> {
            messages.clear();
            saveChat();
            refresh();
        });
        ((ImageButton) findViewById(R.id.btn_settings)).setOnClickListener(v -> showSettings());
        modelChip.setOnClickListener(v -> showModelPicker());
        sendBtn.setOnClickListener(v -> ask());

        loadChat();
        updateChip();
        refresh();
    }

    private String model() {
        return prefs.getString("model", DEFAULT_MODEL);
    }

    private String endpoint() {
        return prefs.getString("endpoint", DEFAULT_ENDPOINT);
    }

    private void updateChip() {
        String m = model();
        modelChip.setText(m.substring(m.lastIndexOf('/') + 1));
    }

    private void ask() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        sendBtn.setEnabled(false);

        messages.add(new Message(true, text));
        Message pending = new Message(false, "");
        pending.loading = true;
        messages.add(pending);
        refresh();

        final String llm = endpoint();
        final String searx = prefs.getString("searx", DEFAULT_SEARX);
        final String model = model();
        final List<Message> history = new ArrayList<>(messages);

        worker.execute(() -> {
            String reply;
            List<String[]> sources = new ArrayList<>();
            try {
                Api.Result r = Api.chat(llm, searx, model, history);
                reply = (r.reply == null || r.reply.isEmpty()) ? "(no reply)" : r.reply;
                sources.addAll(r.sources);
            } catch (Exception e) {
                reply = "Error: " + e.getMessage();
            }
            final String finalReply = reply;
            final List<String[]> finalSources = sources;
            ui.post(() -> {
                pending.loading = false;
                pending.text = finalReply;
                pending.sources.clear();
                pending.sources.addAll(finalSources);
                sendBtn.setEnabled(true);
                saveChat();
                refresh();
            });
        });
    }

    private void showModelPicker() {
        Toast.makeText(this, "Scanning server...", Toast.LENGTH_SHORT).show();
        final String host = prefs.getString("host", DEFAULT_HOST);
        worker.execute(() -> {
            List<Models.Entry> found = Models.scan(host);
            ui.post(() -> {
                if (found.isEmpty()) {
                    Toast.makeText(this, "No models found at " + host + ". Is the server running?",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                String[] labels = new String[found.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = found.get(i).label();
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Models on " + host)
                        .setItems(labels, (d, which) -> {
                            Models.Entry e = found.get(which);
                            prefs.edit()
                                    .putString("model", e.name)
                                    .putString("endpoint", e.endpoint)
                                    .apply();
                            updateChip();
                        })
                        .setNegativeButton("Close", null)
                        .show();
            });
        });
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        if (!messages.isEmpty()) list.scrollToPosition(messages.size() - 1);
    }

    private void saveChat() {
        try {
            JSONArray arr = new JSONArray();
            for (Message m : messages) {
                if (m.loading) continue;
                JSONArray src = new JSONArray();
                for (String[] s : m.sources) src.put(new JSONArray().put(s[0]).put(s[1]));
                arr.put(new JSONObject()
                        .put("user", m.fromUser)
                        .put("text", m.text)
                        .put("sources", src));
            }
            prefs.edit().putString("chat", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void loadChat() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("chat", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Message m = new Message(o.getBoolean("user"), o.getString("text"));
                JSONArray src = o.optJSONArray("sources");
                if (src != null) {
                    for (int j = 0; j < src.length(); j++) {
                        JSONArray s = src.getJSONArray(j);
                        m.sources.add(new String[]{s.getString(0), s.getString(1)});
                    }
                }
                messages.add(m);
            }
        } catch (Exception ignored) {
        }
    }

    private void showSettings() {
        View v = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        TextInputEditText hostField = v.findViewById(R.id.host);
        TextInputEditText searxField = v.findViewById(R.id.searx);
        hostField.setText(prefs.getString("host", DEFAULT_HOST));
        searxField.setText(prefs.getString("searx", DEFAULT_SEARX));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Settings")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String oldHost = prefs.getString("host", DEFAULT_HOST);
                    String newHost = text(hostField, DEFAULT_HOST);
                    String newEndpoint = endpoint().replace(oldHost, newHost);
                    prefs.edit()
                            .putString("host", newHost)
                            .putString("searx", text(searxField, DEFAULT_SEARX))
                            .putString("endpoint", newEndpoint)
                            .apply();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String text(EditText e, String fallback) {
        String s = e.getText() == null ? "" : e.getText().toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
