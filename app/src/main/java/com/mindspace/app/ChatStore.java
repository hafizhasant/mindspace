package com.mindspace.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class ChatStore {
    static final class Info {
        final String id;
        final String title;
        final long updated;

        Info(String id, String title, long updated) {
            this.id = id;
            this.title = title;
            this.updated = updated;
        }
    }

    private final File dir;

    ChatStore(Context ctx) {
        dir = new File(ctx.getFilesDir(), "chats");
        dir.mkdirs();
    }

    String newId() {
        return String.valueOf(System.currentTimeMillis());
    }

    private File file(String id) {
        return new File(dir, id + ".json");
    }

    private String read(File f) throws Exception {
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            int off = 0;
            while (off < b.length) {
                int n = in.read(b, off, b.length - off);
                if (n < 0) break;
                off += n;
            }
        }
        return new String(b, StandardCharsets.UTF_8);
    }

    private JSONObject readJson(String id) throws Exception {
        return new JSONObject(read(file(id)));
    }

    void save(String id, String title, boolean custom, List<Message> messages) {
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
            JSONObject o = new JSONObject()
                    .put("title", title)
                    .put("custom", custom)
                    .put("updated", System.currentTimeMillis())
                    .put("messages", arr);
            try (FileOutputStream out = new FileOutputStream(file(id))) {
                out.write(o.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    List<Message> load(String id) {
        List<Message> out = new ArrayList<>();
        try {
            JSONArray arr = readJson(id).getJSONArray("messages");
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
                out.add(m);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    String title(String id) {
        try {
            return readJson(id).optString("title", "New chat");
        } catch (Exception e) {
            return "New chat";
        }
    }

    boolean isCustom(String id) {
        try {
            return readJson(id).optBoolean("custom", false);
        } catch (Exception e) {
            return false;
        }
    }

    List<Info> list() {
        List<Info> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            String id = f.getName().replace(".json", "");
            try {
                JSONObject o = readJson(id);
                out.add(new Info(id, o.optString("title", "New chat"), o.optLong("updated", 0)));
            } catch (Exception ignored) {
            }
        }
        out.sort((a, b) -> Long.compare(b.updated, a.updated));
        return out;
    }

    void delete(String id) {
        file(id).delete();
    }
}
