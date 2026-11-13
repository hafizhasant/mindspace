package com.local.chat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
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
    private ChatStore store;
    private String currentId;
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView list;
    private EditText input;
    private ImageButton sendBtn;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        store = new ChatStore(this);

        currentId = prefs.getString("current", null);
        if (currentId == null) {
            currentId = store.newId();
            prefs.edit().putString("current", currentId).apply();
        }
        messages.addAll(store.load(currentId));

        list = findViewById(R.id.messages);
        input = findViewById(R.id.input);
        sendBtn = findViewById(R.id.send);

        adapter = new MessageAdapter(this, messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);

        findViewById(R.id.btn_history).setOnClickListener(v -> showHistory());
        findViewById(R.id.btn_new).setOnClickListener(v -> newChat());
        findViewById(R.id.btn_settings).setOnClickListener(v -> showSettings());
        findViewById(R.id.btn_tools).setOnClickListener(v -> showTools());
        sendBtn.setOnClickListener(v -> ask());
        refresh();
    }

    private String model() {
        return prefs.getString("model", DEFAULT_MODEL);
    }

    private String endpoint() {
        return prefs.getString("endpoint", DEFAULT_ENDPOINT);
    }

    private void saveCurrent() {
        if (messages.isEmpty()) return;
        boolean custom = store.isCustom(currentId);
        String title = custom ? store.title(currentId) : autoTitle();
        store.save(currentId, title, custom, messages);
    }

    private String autoTitle() {
        for (Message m : messages) {
            if (m.fromUser) {
                String t = m.text.trim().replace('\n', ' ');
                return t.length() > 40 ? t.substring(0, 40) + "..." : t;
            }
        }
        return "New chat";
    }

    private void openChat(String id) {
        saveCurrent();
        currentId = id;
        prefs.edit().putString("current", id).apply();
        messages.clear();
        messages.addAll(store.load(id));
        refresh();
    }

    private void newChat() {
        saveCurrent();
        currentId = store.newId();
        prefs.edit().putString("current", currentId).apply();
        messages.clear();
        refresh();
    }

    private void clearChat() {
        messages.clear();
        store.delete(currentId);
        refresh();
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
        final boolean search = prefs.getBoolean("search", true);
        final String searx = search ? prefs.getString("searx", DEFAULT_SEARX) : "";
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
                saveCurrent();
                refresh();
            });
        });
    }

    private void showTools() {
        View v = getLayoutInflater().inflate(R.layout.sheet_tools, null);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(v);

        String m = model();
        ((TextView) v.findViewById(R.id.tile_model_label)).setText(m.substring(m.lastIndexOf('/') + 1));
        v.findViewById(R.id.close).setOnClickListener(x -> sheet.dismiss());
        v.findViewById(R.id.tile_model).setOnClickListener(x -> {
            sheet.dismiss();
            showModelPicker();
        });
        v.findViewById(R.id.tile_server).setOnClickListener(x -> {
            sheet.dismiss();
            showSettings();
        });
        v.findViewById(R.id.tile_clear).setOnClickListener(x -> {
            sheet.dismiss();
            clearChat();
        });

        Switch search = v.findViewById(R.id.switch_search);
        search.setChecked(prefs.getBoolean("search", true));
        search.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean("search", on).apply());
        sheet.show();
    }

    private void showModelPicker() {
        Toast.makeText(this, "Scanning server...", Toast.LENGTH_SHORT).show();
        final String host = prefs.getString("host", DEFAULT_HOST);
        worker.execute(() -> {
            List<Models.Entry> found = Models.scan(host);
            ui.post(() -> {
                if (found.isEmpty()) {
                    Toast.makeText(this, "No models found at " + host, Toast.LENGTH_LONG).show();
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
                        })
                        .setNegativeButton("Close", null)
                        .show();
            });
        });
    }

    private void showHistory() {
        saveCurrent();
        List<ChatStore.Info> chats = store.list();
        if (chats.isEmpty()) {
            Toast.makeText(this, "No saved chats yet", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (ChatStore.Info c : chats) {
            labels.add(c.id.equals(currentId) ? c.title + "  (current)" : c.title);
        }
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Chats")
                .setMessage("Tap to open. Hold to rename or delete.")
                .setView(lv)
                .setNegativeButton("Close", null)
                .create();
        lv.setOnItemClickListener((p, v, pos, id) -> {
            dialog.dismiss();
            openChat(chats.get(pos).id);
        });
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            dialog.dismiss();
            chatOptions(chats.get(pos));
            return true;
        });
        dialog.show();
    }

    private void chatOptions(ChatStore.Info c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(c.title)
                .setItems(new String[]{"Open", "Rename", "Delete"}, (d, which) -> {
                    if (which == 0) openChat(c.id);
                    else if (which == 1) renameChat(c);
                    else deleteChat(c);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameChat(ChatStore.Info c) {
        EditText field = new EditText(this);
        field.setText(c.title);
        field.setSingleLine(true);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename chat")
                .setView(field)
                .setPositiveButton("Save", (d, w) -> {
                    String t = field.getText().toString().trim();
                    if (t.isEmpty()) return;
                    List<Message> msgs = c.id.equals(currentId) ? messages : store.load(c.id);
                    store.save(c.id, t, true, msgs);
                    showHistory();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChat(ChatStore.Info c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this chat?")
                .setMessage("\"" + c.title + "\" will be removed. This can't be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    store.delete(c.id);
                    if (c.id.equals(currentId)) {
                        messages.clear();
                        currentId = store.newId();
                        prefs.edit().putString("current", currentId).apply();
                        refresh();
                    }
                    showHistory();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        if (!messages.isEmpty()) list.scrollToPosition(messages.size() - 1);
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
                    String newSearx = text(searxField, DEFAULT_SEARX).replace(oldHost, newHost);
                    prefs.edit()
                            .putString("host", newHost)
                            .putString("searx", newSearx)
                            .putString("endpoint", newEndpoint)
                            .apply();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String text(TextInputEditText e, String fallback) {
        String s = e.getText() == null ? "" : e.getText().toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
