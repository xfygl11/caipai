package webapp.newcloud.lottery.movie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.model.Movie;

public class MovieGridAdapter extends RecyclerView.Adapter<MovieGridAdapter.ViewHolder> {
    private final List<Movie> movies;
    private final OnMovieClickListener listener;

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    public MovieGridAdapter(List<Movie> movies, OnMovieClickListener listener) {
        this.movies = movies;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return 0; // single view type
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public void update(List<Movie> newMovies) {
        movies.clear();
        movies.addAll(newMovies);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle;
        TextView tvInfo;
        TextView tvTag;
        TextView tvQuality;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            tvTag = itemView.findViewById(R.id.tvTag);
            tvQuality = itemView.findViewById(R.id.tvQuality);
        }

        public void bind(final Movie movie) {
            tvTitle.setText(movie.title);
            String year = movie.year != null ? movie.year : "";
            String area = movie.area != null ? movie.area : "";
            String type = movie.type != null ? movie.type : "";
            tvInfo.setText(((year + " " + area + " " + type).trim().isEmpty() ? "--" : (year + " " + area + " " + type).trim()));

            if (movie.tag != null && !movie.tag.isEmpty()) {
                tvTag.setText(movie.tag);
                tvTag.setVisibility(View.VISIBLE);
            } else {
                tvTag.setVisibility(View.GONE);
            }

            if (movie.quality != null && !movie.quality.isEmpty()) {
                tvQuality.setText(movie.quality);
                tvQuality.setVisibility(View.VISIBLE);
            } else {
                tvQuality.setVisibility(View.GONE);
            }

            if (movie.pic != null && !movie.pic.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(movie.pic)
                        .placeholder(R.drawable.bg_card)
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(0);
                ivPoster.setBackgroundColor(0xff1a2a3a);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onMovieClick(movie);
                    }
                }
            });
        }
    }
}
