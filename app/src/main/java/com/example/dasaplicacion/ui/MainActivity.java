package com.example.dasaplicacion.ui;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.dasaplicacion.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import androidx.work.WorkManager;

import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_CALENDAR_CODE = 1002;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode";
    // Clave para el idioma de la app
    private static final String KEY_APP_LANGUAGE = "app_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forzamos la zona horaria de Madrid en toda la app
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));

        // Inicializamos SharedPreferences y aplicamos el modo oscuro/claro antes de setContentView
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!preferences.contains(KEY_NOTIFICATIONS_ENABLED)) {
            preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, true).apply();
        }
        if (!preferences.contains(KEY_DARK_MODE)) {
            preferences.edit().putBoolean(KEY_DARK_MODE, false).apply();
        }
        if (!preferences.contains(KEY_APP_LANGUAGE)) {
            preferences.edit().putString(KEY_APP_LANGUAGE, "es").apply();
        }
        // Aplicar el idioma guardado
        setLocale(preferences.getString(KEY_APP_LANGUAGE, "es"));

        boolean darkModeEnabled = preferences.getBoolean(KEY_DARK_MODE, false);
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            View customView = LayoutInflater.from(this).inflate(R.layout.custom_toolbar, toolbar, false);
            getSupportActionBar().setCustomView(customView);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
        }

        // Configurar DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Configurar el header del NavigationView
        View headerView = navigationView.getHeaderView(0);
        // Switch para notificaciones
        Switch switchNotifications = headerView.findViewById(R.id.switch_notifications);
        boolean notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        switchNotifications.setChecked(notificationsEnabled);
        switchNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
                if (!isChecked) {
                    WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("task_notification");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    PERMISSION_REQUEST_CODE);
                        }
                    }
                }
            }
        });

        // Configurar el switch para el modo oscuro
        Switch switchTheme = headerView.findViewById(R.id.switch_theme);
        switchTheme.setChecked(darkModeEnabled);
        switchTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                // Reiniciamos la actividad
                setLocale(preferences.getString(KEY_APP_LANGUAGE, "es"));
            }
        });

        // Configurar el spinner de idioma usando arrays.xml
        Spinner spinnerLanguage = headerView.findViewById(R.id.spinner_language);
        String savedLanguage = preferences.getString(KEY_APP_LANGUAGE, "es");
        int selectedIndex = 0;
        if ("es".equals(savedLanguage)) {
            selectedIndex = 0;
        } else if ("eu".equals(savedLanguage)) {
            selectedIndex = 1;
        } else if ("en".equals(savedLanguage)) {
            selectedIndex = 2;
        }
        // Quitar temporalmente el listener para evitar triggers no deseados
        spinnerLanguage.setOnItemSelectedListener(null);
        spinnerLanguage.setSelection(selectedIndex);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String languageCode = "es";
                if (position == 0) {
                    languageCode = "es";
                } else if (position == 1) {
                    languageCode = "eu";
                } else if (position == 2) {
                    languageCode = "en";
                }
                String currentLanguage = getResources().getConfiguration().locale.getLanguage();
                if (languageCode.equals(currentLanguage)) {
                    return;
                }
                preferences.edit().putString(KEY_APP_LANGUAGE, languageCode).apply();
                setLocale(languageCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Solicitar permisos de calendario si no han sido concedidos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                        PERMISSION_REQUEST_CALENDAR_CODE);
            }
        }

        // Configurar BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        loadFragment(new HomeFragment());
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (id == R.id.nav_tasks) {
                selectedFragment = new TasksFragment();
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // Método para cambiar el idioma sin provocar un bucle infinito
    private void setLocale(String languageCode) {
        // Si ya está configurado, no hacemos nada
        String currentLanguage = getResources().getConfiguration().locale.getLanguage();
        if (languageCode.equals(currentLanguage)) {
            return;
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        // Finalizamos y reiniciamos la actividad con una transición suave
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(getIntent());
    }
}
