package webapp.newcloud.lottery.movie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.model.SiteConfig;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {
    private final List<SiteConfig> sites;
    private final OnSiteClickListener listener;

    public interface OnSiteClickListener {
        void onSiteClick(SiteConfig site);
    }

    public SiteAdapter(List<SiteConfig> sites, OnSiteClickListener listener) {
        this.sites = sites;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_site, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SiteConfig site = sites.get(position);
        holder.bind(site);
    }

    @Override
    public int getItemCount() {
        return sites.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSiteName;
        TextView tvSiteTag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSiteName = itemView.findViewById(R.id.tvSiteName);
            tvSiteTag = itemView.findViewById(R.id.tvSiteTag);
        }

        public void bind(final SiteConfig site) {
            tvSiteName.setText(site.name);
            String typeLabel = site.type == 0 ? "XML" : site.type == 1 ? "JSON" : site.type == 3 ? "JS插件" : "未知";
            String searchTag = (site.searchable > 0 || site.quickSearch > 0) ? " · 可搜索" : "";
            tvSiteTag.setText(typeLabel + searchTag);
            
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onSiteClick(site);
                    }
                }
            });
        }
    }
}
