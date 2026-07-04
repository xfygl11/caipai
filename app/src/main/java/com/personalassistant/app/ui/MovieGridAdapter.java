package com.personalassistant.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.personalassistant.app.R;
import com.personalassistant.app.data.model.MovieItem;

import java.util.ArrayList;
import java.util.List;

public class MovieGridAdapter extends RecyclerView.Adapter<MovieGridAdapter.ViewHolder> {
    private final List<MovieItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(MovieItem item); }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void setItems(List<MovieItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItems(List<MovieItem> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        CardView card = new CardView(ctx);
        card.setRadius(dp(12, ctx));
        card.setCardElevation(dp(4, ctx));
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(0xFF141E2E);

        View content = LayoutInflater.from(ctx).inflate(R.layout.item_movie_card, parent, false);
        card.addView(content);
        card.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(card);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieItem item = items.get(position);
        holder.title.setText(item.title);

        StringBuilder info = new StringBuilder();
        if (item.year != null && !item.year.isEmpty()) info.append(item.year);
        if (item.tag != null && !item.tag.isEmpty()) {
            if (info.length() > 0) info.append(" ");
            info.append(item.tag);
        }
        if (item.area != null && !item.area.isEmpty()) {
            info.append(" ").append(item.area);
        }
        holder.info.setText(info.toString());

        if (item.pic != null && !item.pic.isEmpty()) {
            Glide.with(holder.poster.getContext())
                    .load(item.pic)
                    .placeholder(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .error(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .into(holder.poster);
        } else {
            holder.poster.setImageDrawable(new android.graphics.drawable.ColorDrawable(0xFF1D3557));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView title, info;
        ViewHolder(View v) {
            super(v);
            poster = v.findViewById(R.id.movie_poster);
            title = v.findViewById(R.id.movie_title);
            info = v.findViewById(R.id.movie_info);
        }
    }

    static int dp(int dp, Context ctx) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
