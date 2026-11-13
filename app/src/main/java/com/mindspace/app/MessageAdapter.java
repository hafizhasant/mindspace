package com.mindspace.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import io.noties.markwon.Markwon;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
    interface Listener {
        void onRetry(int position);
    }

    private final Context ctx;
    private final List<Message> items;
    private final Markwon markwon;
    private final Listener listener;

    MessageAdapter(Context ctx, List<Message> items, Listener listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
        this.markwon = Markwon.create(ctx);
    }

    static final class VH extends RecyclerView.ViewHolder {
        final LinearLayout row, thinking, sources, actions;
        final MaterialCardView card;
        final ImageView photo;
        final TextView body;
        final View copy, share, retry;

        VH(View v) {
            super(v);
            row = v.findViewById(R.id.row);
            card = v.findViewById(R.id.card);
            photo = v.findViewById(R.id.photo);
            thinking = v.findViewById(R.id.thinking);
            body = v.findViewById(R.id.body);
            sources = v.findViewById(R.id.sources);
            actions = v.findViewById(R.id.actions);
            copy = v.findViewById(R.id.act_copy);
            share = v.findViewById(R.id.act_share);
            retry = v.findViewById(R.id.act_retry);
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
        h.row.setGravity(user ? Gravity.END : Gravity.START);
        h.card.setCardBackgroundColor(user ? Color.parseColor("#2E2E2C") : Color.TRANSPARENT);

        if (user && m.image != null) {
            h.photo.setImageBitmap(BitmapFactory.decodeFile(m.image));
            h.photo.setVisibility(View.VISIBLE);
        } else {
            h.photo.setVisibility(View.GONE);
        }

        if (m.loading) {
            h.thinking.setVisibility(View.VISIBLE);
            h.body.setVisibility(View.GONE);
        } else {
            h.thinking.setVisibility(View.GONE);
            h.body.setVisibility(View.VISIBLE);
            if (user) {
                h.body.setText(m.text);
            } else {
                markwon.setMarkdown(h.body, m.text);
            }
            h.body.setMovementMethod(LinkMovementMethod.getInstance());
        }

        h.sources.removeAllViews();
        if (!user && !m.loading) {
            Set<String> seen = new HashSet<>();
            int shown = 0;
            for (String[] s : m.sources) {
                if (shown >= 5 || !seen.add(s[1])) continue;
                h.sources.addView(sourceView(s));
                shown++;
            }
        }

        if (!user && !m.loading) {
            h.actions.setVisibility(View.VISIBLE);
            h.copy.setOnClickListener(x -> copy(m));
            h.share.setOnClickListener(x -> share(m));
            h.retry.setVisibility(position == items.size() - 1 ? View.VISIBLE : View.GONE);
            h.retry.setOnClickListener(x -> listener.onRetry(position));
        } else {
            h.actions.setVisibility(View.GONE);
        }

        h.card.setOnLongClickListener(v -> {
            copy(m);
            return true;
        });
    }

    private void copy(Message m) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("message", m.text));
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
    }

    private void share(Message m) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, m.text);
        ctx.startActivity(Intent.createChooser(i, "Share"));
    }

    private View sourceView(String[] s) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_source, null, false);
        ((TextView) v.findViewById(R.id.source_title)).setText(s[0].isEmpty() ? s[1] : s[0]);
        ((TextView) v.findViewById(R.id.source_url)).setText(s[1]);
        v.setOnClickListener(x -> ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(s[1]))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        return v;
    }
}
