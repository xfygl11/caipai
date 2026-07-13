package webapp.newcloud.lottery.movie.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.MainActivity;
import webapp.newcloud.lottery.movie.adapter.MovieGridAdapter;
import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.model.Favorite;
import webapp.newcloud.lottery.movie.model.History;
import webapp.newcloud.lottery.movie.model.Movie;

public class LibraryFragment extends Fragment implements MovieGridAdapter.OnMovieClickListener {

    private TextView tabFav;
    private TextView tabHist;
    private RecyclerView libraryContentGrid;
    private LinearLayout emptyState;

    private MovieGridAdapter adapter;
    private java.util.ArrayList<Movie> movieList = new java.util.ArrayList<>();
    private String currentTab = "fav";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        
        tabFav = view.findViewById(R.id.tab_fav);
        tabHist = view.findViewById(R.id.tab_hist);
        libraryContentGrid = view.findViewById(R.id.libraryContentGrid);
        emptyState = view.findViewById(R.id.emptyState);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        libraryContentGrid.setLayoutManager(layoutManager);
        adapter = new MovieGridAdapter(movieList, this);
        libraryContentGrid.setAdapter(adapter);

        tabFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("fav");
            }
        });
        tabHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("history");
            }
        });

        switchTab("fav");
        return view;
    }

    private void switchTab(String tab) {
        currentTab = tab;
        if (tab.equals("fav")) {
            tabFav.setTextColor(0xffffffff);
            tabFav.setBackgroundResource(R.color.primary);
            tabHist.setTextColor(0xff667788);
            tabHist.setBackgroundResource(R.color.background_card);
            loadFavorites();
        } else {
            tabHist.setTextColor(0xffffffff);
            tabHist.setBackgroundResource(R.color.primary);
            tabFav.setTextColor(0xff667788);
            tabFav.setBackgroundResource(R.color.background_card);
            loadHistory();
        }
    }

    private void loadFavorites() {
        if (getContext() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Favorite> favorites = DbHelper.getInstance(getContext()).favoriteDao().getAll();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (favorites.isEmpty()) {
                                emptyState.setVisibility(View.VISIBLE);
                                libraryContentGrid.setVisibility(View.GONE);
                            } else {
                                emptyState.setVisibility(View.GONE);
                                libraryContentGrid.setVisibility(View.VISIBLE);
                            java.util.ArrayList<Movie> movies = new ArrayList<>();
                            for (Favorite fav : favorites) {
                                Movie m = new Movie();
                                m.id = fav.movieId;
                                m.title = fav.title;
                                m.pic = fav.pic;
                                m.type = fav.type;
                                m.tag = fav.tag;
                                m.vodId = fav.movieId;
                                movies.add(m);
                            }
                            movieList.clear();
                            movieList.addAll(movies);
                            adapter.update(movies);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void loadHistory() {
        if (getContext() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<History> historyList = DbHelper.getInstance(getContext()).historyDao().getAll();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (historyList.isEmpty()) {
                                emptyState.setVisibility(View.VISIBLE);
                                libraryContentGrid.setVisibility(View.GONE);
                            } else {
                                emptyState.setVisibility(View.GONE);
                                libraryContentGrid.setVisibility(View.VISIBLE);
                            java.util.ArrayList<Movie> movies = new ArrayList<>();
                            for (History h : historyList) {
                                Movie m = new Movie();
                                m.id = h.movieId;
                                m.title = h.title;
                                m.pic = h.pic;
                                m.type = h.type;
                                m.tag = h.tag;
                                m.play = h.playUrl;
                                m.vodId = h.movieId;
                                movies.add(m);
                            }
                            movieList.clear();
                            movieList.addAll(movies);
                            adapter.update(movies);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onMovieClick(Movie movie) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.openMovieDetail(movie);
        }
    }
}
