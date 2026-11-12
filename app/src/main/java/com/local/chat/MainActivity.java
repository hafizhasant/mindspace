package com.local.chat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_LLM = "http://192.168.1.4:8080/v1/chat/completions";
    private static final String DEFAULT_SEARX = "http://192.168.1.4:8888/search";
    private static final String DEFAULT_MODEL = "gemma-4-12b-it-Q4_K_M";

    private SharedPreferences prefs;
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView list;
    private TextInputEditText input;
    private MaterialButton sendBtn;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle state) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(themeMode(prefs.getString("theme", "system")));
        if ("wallpaper".equals(prefs.getString("color", "wallpaper"))) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        list = findViewById(R.id.messages);
        input = findViewById(R.id.input);
        sendBtn = findViewById(R.id.send);

        adapter = new MessageAdapter(this, messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);

        sendBtn.setOnClickListener(v -> ask());
        loadChat();
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            messages.clear();
            saveChat();
            refresh();
            return true;
        }
        if (id == R.id.action_settings) {
            showSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        final String llm = prefs.getString("llm", DEFAULT_LLM);
        final String searx = prefs.getString("searx", DEFAULT_SEARX);
        final String model = prefs.getString("model", DEFAULT_MODEL);
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
            runOnUiThread(() -> {
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
        TextInputEditText llm = v.findViewById(R.id.llm);
        TextInputEditText searx = v.findViewById(R.id.searx);
        TextInputEditText model = v.findViewById(R.id.model);
        RadioGroup themeGroup = v.findViewById(R.id.theme_group);
        RadioGroup colorGroup = v.findViewById(R.id.color_group);

        llm.setText(prefs.getString("llm", DEFAULT_LLM));
        searx.setText(prefs.getString("searx", DEFAULT_SEARX));
        model.setText(prefs.getString("model", DEFAULT_MODEL));

        String theme = prefs.getString("theme", "system");
        themeGroup.check(theme.equals("light") ? R.id.theme_light
                : theme.equals("dark") ? R.id.theme_dark : R.id.theme_system);
        String color = prefs.getString("color", "wallpaper");
        colorGroup.check("default".equals(color) ? R.id.color_default : R.id.color_wallpaper);

        final String oldColor = color;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Settings")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    int themeId = themeGroup.getCheckedRadioButtonId();
                    String newTheme = themeId == R.id.theme_light ? "light"
                            : themeId == R.id.theme_dark ? "dark" : "system";
                    int colorId = colorGroup.getCheckedRadioButtonId();
                    String newColor = colorId == R.id.color_default ? "default" : "wallpaper";
                    prefs.edit()
                            .putString("llm", text(llm, DEFAULT_LLM))
                            .putString("searx", text(searx, DEFAULT_SEARX))
                            .putString("model", text(model, DEFAULT_MODEL))
                            .putString("theme", newTheme)
                            .putString("color", newColor)
                            .apply();
                    AppCompatDelegate.setDefaultNightMode(themeMode(newTheme));
                    if (!newColor.equals(oldColor)) recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String text(TextInputEditText e, String fallback) {
        String s = e.getText() == null ? "" : e.getText().toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    private static int themeMode(String t) {
        switch (t) {
            case "light": return AppCompatDelegate.MODE_NIGHT_NO;
            case "dark": return AppCompatDelegate.MODE_NIGHT_YES;
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
