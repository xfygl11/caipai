package webapp.newcloud.lottery.movie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.model.LiveChannel;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {
    private final List<LiveChannel> channels;
    private final OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(LiveChannel channel);
    }

    public ChannelAdapter(List<LiveChannel> channels, OnChannelClickListener listener) {
        this.channels = channels;
        this.listener = listener;
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
        LiveChannel channel = channels.get(position);
        holder.bind(channel);
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    public void update(List<LiveChannel> newChannels) {
        channels.clear();
        channels.addAll(newChannels);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLiveName;
        TextView tvLiveGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLiveName = itemView.findViewById(R.id.tvLiveName);
            tvLiveGroup = itemView.findViewById(R.id.tvLiveGroup);
        }

        public void bind(final LiveChannel channel) {
            tvLiveName.setText(channel.name);
            tvLiveGroup.setText(channel.group);
            
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onChannelClick(channel);
                    }
                }
            });
        }
    }
}
