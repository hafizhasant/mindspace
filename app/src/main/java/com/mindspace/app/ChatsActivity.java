package com.mindspace.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatsActivity extends AppCompatActivity {
    private ChatStore store;
    private String currentId;
    private EditText search;
    private TextView empty;
    private final List<ChatStore.Info> all = new ArrayList<>();
    private final List<ChatStore.Info> shown = new ArrayList<>();

    private final BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int p) {
            return shown.get(p);
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public View getView(int p, View convert, ViewGroup parent) {
            View v = convert != null ? convert
                    : LayoutInflater.from(ChatsActivity.this).inflate(R.layout.item_chat, parent, false);
            ChatStore.Info c = shown.get(p);
            ((TextView) v.findViewById(R.id.chat_title)).setText(c.title);
            String when = ago(c.updated);
            ((TextView) v.findViewById(R.id.chat_time))
                    .setText(c.id.equals(currentId) ? when + "  ·  current" : when);
            return v;
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_chats);
        store = new ChatStore(this);
        currentId = getIntent().getStringExtra("current");
        search = findViewById(R.id.search);
        empty = findViewById(R.id.empty);

        ListView listView = findViewById(R.id.chat_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> finishWith(shown.get(pos).id));
        listView.setOnItemLongClickListener((p, v, pos, id) -> {
            options(shown.get(pos));
            return true;
        });

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_new_chat).setOnClickListener(v -> finishWith(store.newId()));

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        reload();
    }

    private void reload() {
        all.clear();
        all.addAll(store.list());
        filter();
    }

    private void filter() {
        String q = search.getText() == null ? ""
                : search.getText().toString().trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (ChatStore.Info c : all) {
            if (q.isEmpty() || c.title.toLowerCase(Locale.getDefault()).contains(q)) shown.add(c);
        }
        adapter.notifyDataSetChanged();
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void finishWith(String id) {
        setResult(RESULT_OK, new Intent().putExtra("id", id));
        finish();
    }

    private void options(ChatStore.Info c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(c.title)
                .setItems(new String[]{"Open", "Rename", "Delete"}, (d, which) -> {
                    if (which == 0) finishWith(c.id);
                    else if (which == 1) rename(c);
                    else confirmDelete(c);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rename(ChatStore.Info c) {
        EditText field = new EditText(this);
        field.setText(c.title);
        field.setSingleLine(true);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename chat")
                .setView(field)
                .setPositiveButton("Save", (d, w) -> {
                    String t = field.getText().toString().trim();
                    if (t.isEmpty()) return;
                    store.save(c.id, t, true, store.load(c.id));
                    reload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(ChatStore.Info c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this chat?")
                .setMessage("\"" + c.title + "\" will be removed.")
                .setPositiveButton("Delete", (d, w) -> {
                    store.delete(c.id);
                    if (c.id.equals(currentId)) finishWith(store.newId());
                    else reload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String ago(long t) {
        if (t <= 0) return "";
        long s = (System.currentTimeMillis() - t) / 1000;
        if (s < 60) return "Just now";
        if (s < 3600) return (s / 60) + " min ago";
        if (s < 86400) return (s / 3600) + " h ago";
        if (s < 172800) return "Yesterday";
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(t));
    }
}
