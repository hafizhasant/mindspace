package com.mindspace.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import io.noties.markwon.Markwon;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
    interface Listener {
        void onRetry(int position);
        void onEdit(int position);
    }

    private final Context ctx;
    private final List<Message> items;
    private final Markwon markwon;
    private final Listener listener;
    private final Typeface sans;

    MessageAdapter(Context ctx, List<Message> items, Listener listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
        this.markwon = Markwon.create(ctx);
        this.sans = ResourcesCompat.getFont(ctx, R.font.firasans);
    }

    static final class VH extends RecyclerView.ViewHolder {
        final LinearLayout row, actions, thinking, blocks, sources;
        final MaterialCardView card;
        final ImageView photo;
        final ImageButton copy, share, retry, edit;
        final TextView sourceToggle;

        VH(View v) {
            super(v);
            row = v.findViewById(R.id.row);
            actions = v.findViewById(R.id.actions);
            card = v.findViewById(R.id.card);
            photo = v.findViewById(R.id.photo);
            thinking = v.findViewById(R.id.thinking);
            blocks = v.findViewById(R.id.blocks);
            sources = v.findViewById(R.id.sources);
            sourceToggle = v.findViewById(R.id.source_toggle);
            copy = v.findViewById(R.id.act_copy);
            share = v.findViewById(R.id.act_share);
            retry = v.findViewById(R.id.act_retry);
            edit = v.findViewById(R.id.act_edit);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onBindViewHolder(VH h, int position) {
        Message m = items.get(position);
        boolean user = m.fromUser;
        boolean blank = !m.loading && m.text.trim().isEmpty() && m.image == null;
        h.row.setVisibility(blank ? View.GONE : View.VISIBLE);
        if (blank) return;

        h.row.setGravity(user ? Gravity.END : Gravity.START);
        h.card.setCardBackgroundColor(user ? Color.parseColor("#2F2F2F") : Color.TRANSPARENT);

        if (user && m.image != null) {
            h.photo.setImageBitmap(BitmapFactory.decodeFile(m.image));
            h.photo.setVisibility(View.VISIBLE);
        } else {
            h.photo.setVisibility(View.GONE);
        }

        h.blocks.removeAllViews();
        if (m.loading) {
            h.thinking.setVisibility(View.VISIBLE);
            h.blocks.setVisibility(View.GONE);
        } else {
            h.thinking.setVisibility(View.GONE);
            h.blocks.setVisibility(View.VISIBLE);
            if (user) h.blocks.addView(textBlock(m.text));
            else fillBlocks(h.blocks, m.text);
        }

        bindSources(h, m, user);

        h.actions.setVisibility(m.loading ? View.GONE : View.VISIBLE);
        h.copy.setVisibility(user ? View.GONE : View.VISIBLE);
        h.share.setVisibility(user ? View.GONE : View.VISIBLE);
        h.copy.setOnClickListener(x -> copyText(m.text));
        h.share.setOnClickListener(x -> share(m));
        h.retry.setVisibility(!user && position == items.size() - 1 ? View.VISIBLE : View.GONE);
        h.retry.setOnClickListener(x -> listener.onRetry(position));
        h.edit.setVisibility(user ? View.VISIBLE : View.GONE);
        h.edit.setOnClickListener(x -> listener.onEdit(position));
        h.card.setOnLongClickListener(v -> {
            if (user) listener.onEdit(position);
            else copyText(m.text);
            return true;
        });
    }

    private void bindSources(VH h, Message m, boolean user) {
        h.sources.removeAllViews();
        h.sources.setVisibility(View.GONE);

        Set<String> seen = new HashSet<>();
        List<String[]> unique = new ArrayList<>();
        for (String[] s : m.sources) {
            if (seen.add(s[1])) unique.add(s);
        }
        if (user || m.loading || unique.isEmpty()) {
            h.sourceToggle.setVisibility(View.GONE);
            return;
        }

        int shown = Math.min(unique.size(), 5);
        h.sourceToggle.setVisibility(View.VISIBLE);
        h.sourceToggle.setText("Sources · " + shown);
        h.sourceToggle.setOnClickListener(x -> {
            if (h.sources.getVisibility() == View.VISIBLE) {
                h.sources.setVisibility(View.GONE);
                return;
            }
            h.sources.removeAllViews();
            for (int i = 0; i < shown; i++) h.sources.addView(sourceRow(unique.get(i)));
            h.sources.setVisibility(View.VISIBLE);
        });
    }

    private View sourceRow(String[] s) {
        TextView tv = new TextView(ctx);
        String title = s[0].isEmpty() ? host(s[1]) : s[0];
        tv.setText(title + "\n" + host(s[1]));
        tv.setTextColor(Color.parseColor("#B8B8B5"));
        tv.setTextSize(13);
        if (sans != null) tv.setTypeface(sans);
        tv.setMaxLines(2);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        tv.setBackgroundResource(R.drawable.bg_tile);
        tv.setClickable(true);
        tv.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(280), LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, 0);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(x -> ctx.startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(s[1])).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        return tv;
    }

    private static String host(String url) {
        try {
            String h = new URL(url).getHost();
            return h.startsWith("www.") ? h.substring(4) : h;
        } catch (Exception e) {
            return url;
        }
    }

    private void fillBlocks(LinearLayout container, String text) {
        String[] parts = text.split("```", -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i % 2 == 0) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    TextView tv = textBlock(null);
                    markwon.setMarkdown(tv, t);
                    container.addView(tv);
                }
            } else {
                String lang = "";
                String code = part;
                int nl = part.indexOf('\n');
                if (nl >= 0) {
                    String first = part.substring(0, nl).trim();
                    if (!first.contains(" ") && first.length() <= 20) {
                        lang = first;
                        code = part.substring(nl + 1);
                    }
                }
                container.addView(codeBlock(lang, code.replaceAll("\\s+$", "")));
            }
        }
        if (container.getChildCount() == 0) container.addView(textBlock(text));
    }

    private TextView textBlock(String plain) {
        TextView tv = new TextView(ctx);
        tv.setTextColor(Color.parseColor("#ECECEC"));
        tv.setTextSize(16);
        tv.setLineSpacing(dp(2), 1.2f);
        if (sans != null) tv.setTypeface(sans);
        tv.setTextIsSelectable(true);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setMaxWidth(dp(320));
        tv.setPadding(0, 0, 0, dp(6));
        if (plain != null) tv.setText(plain);
        return tv;
    }

    private View codeBlock(String lang, String code) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_code);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT);
        boxLp.setMargins(0, dp(6), 0, dp(8));
        box.setLayoutParams(boxLp);

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(2), dp(4), dp(2));

        TextView label = new TextView(ctx);
        label.setText(lang.isEmpty() ? "Code" : lang);
        label.setTextColor(Color.parseColor("#B8B8B5"));
        label.setTextSize(13);
        if (sans != null) label.setTypeface(sans);
        header.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(iconButton(R.drawable.ic_act_copy, "Copy code", x -> copyText(code)));
        header.addView(iconButton(R.drawable.ic_expand, "Expand code", x -> expand(code)));
        box.addView(header);

        HorizontalScrollView scroll = new HorizontalScrollView(ctx);
        TextView body = new TextView(ctx);
        body.setText(code);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextColor(Color.parseColor("#D6D6D2"));
        body.setTextSize(13);
        body.setTextIsSelectable(true);
        body.setPadding(dp(12), dp(4), dp(12), dp(12));
        scroll.addView(body);
        box.addView(scroll);
        return box;
    }

    private ImageButton iconButton(int res, String desc, View.OnClickListener l) {
        ImageButton b = new ImageButton(ctx);
        b.setImageResource(res);
        b.setColorFilter(Color.parseColor("#B8B8B5"));
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(7), dp(7), dp(7), dp(7));
        b.setContentDescription(desc);
        b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        return b;
    }

    private void expand(String code) {
        TextView tv = new TextView(ctx);
        tv.setText(code);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(Color.parseColor("#ECECEC"));
        tv.setTextSize(13);
        tv.setTextIsSelectable(true);
        int p = dp(16);
        tv.setPadding(p, p, p, p);
        HorizontalScrollView hs = new HorizontalScrollView(ctx);
        hs.addView(tv);
        ScrollView sv = new ScrollView(ctx);
        sv.addView(hs);
        new MaterialAlertDialogBuilder(ctx)
                .setView(sv)
                .setPositiveButton("Copy", (d, w) -> copyText(code))
                .setNegativeButton("Close", null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }

    private void copyText(String s) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("text", s));
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
    }

    private void share(Message m) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, m.text);
        ctx.startActivity(Intent.createChooser(i, "Share"));
    }
}
