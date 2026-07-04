package com.personalassistant.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.personalassistant.app.R;
import com.personalassistant.app.data.model.LiveChannel;

import java.util.ArrayList;
import java.util.List;

public class LiveChannelAdapter extends RecyclerView.Adapter<LiveChannelAdapter.ViewHolder> {
    private final List<LiveChannel> channels = new ArrayList<>();
    private OnChannelClickListener listener;

    public interface OnChannelClickListener { void onChannelClick(LiveChannel channel); }

    public void setOnChannelClickListener(OnChannelClickListener l) { this.listener = l; }

    public void setChannels(List<LiveChannel> newChannels) {
        channels.clear();
        if (newChannels != null) channels.addAll(newChannels);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LiveChannel ch = channels.get(position);
        holder.name.setText(ch.name);
        holder.group.setText(ch.group != null ? ch.group : "");

        if (ch.logo != null && !ch.logo.isEmpty()) {
            Glide.with(holder.logo.getContext())
                    .load(ch.logo)
                    .placeholder(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .error(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .into(holder.logo);
        } else {
            holder.logo.setImageDrawable(new android.graphics.drawable.ColorDrawable(0xFF1D3557));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChannelClick(ch);
        });
    }

    @Override
    public int getItemCount() { return channels.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name, group;
        ViewHolder(View v) {
            super(v);
            logo = v.findViewById(R.id.channel_logo);
            name = v.findViewById(R.id.channel_name);
            group = v.findViewById(R.id.channel_group);
        }
    }
}
