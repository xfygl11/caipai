package com.personalassistant.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.personalassistant.app.db.ParserSeeder;
import com.personalassistant.app.ui.FavoritesFragment;
import com.personalassistant.app.ui.HistoryFragment;
import com.personalassistant.app.ui.LiveFragment;
import com.personalassistant.app.ui.LotteryFragment;
import com.personalassistant.app.ui.MovieHomeFragment;
import com.personalassistant.app.ui.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Seed default parsers if needed
        ParserSeeder.seedIfEmpty(this);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_movie) {
                switchFragment(new MovieHomeFragment());
                return true;
            } else if (id == R.id.nav_live) {
                switchFragment(new LiveFragment());
                return true;
            } else if (id == R.id.nav_history) {
                switchFragment(new HistoryFragment());
                return true;
            } else if (id == R.id.nav_lottery) {
                switchFragment(new LotteryFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                switchFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_movie);
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
