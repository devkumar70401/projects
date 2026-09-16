package com.devendra.healthtracker;

import com.devendra.healthtracker.ui.components.AestheticSceneryView;
import com.devendra.healthtracker.ui.components.AtmosphericCardBackgroundView;
import com.devendra.healthtracker.ui.components.SwipeGestureLayout;

import com.devendra.healthtracker.biometrics.BiometricsManager;
import com.devendra.healthtracker.calculator.CalculatorCardController;
import com.devendra.healthtracker.cleaner.DeepCleanManager;
import com.devendra.healthtracker.downloader.AsyncThumbnailLoader;
import com.devendra.healthtracker.downloader.DownloadJob;
import com.devendra.healthtracker.downloader.ExtractedMedia;
import com.devendra.healthtracker.downloader.InstagramExtractorEngine;
import com.devendra.healthtracker.downloader.MediaDownloadManager;
import com.devendra.healthtracker.downloader.MediaResolverRegistry;
import com.devendra.healthtracker.downloader.MediaSource;
import com.devendra.healthtracker.downloader.StreamFormat;
import com.devendra.healthtracker.downloader.WhatsAppStatusHarvester;
import com.devendra.healthtracker.pomodoro.PomodoroManager;
import com.devendra.healthtracker.pomodoro.PomodoroWidgetProvider;
import com.devendra.healthtracker.routine.AlarmAudioService;
import com.devendra.healthtracker.routine.ChecklistItem;
import com.devendra.healthtracker.routine.ChecklistManager;
import com.devendra.healthtracker.routine.RoutineBlock;
import com.devendra.healthtracker.routine.RoutineCompletionManager;
import com.devendra.healthtracker.routine.ScheduleManager;
import com.devendra.healthtracker.sound.SoundManager;
import com.devendra.healthtracker.telemetry.DeviceTelemetryManager;
import com.devendra.healthtracker.ui.components.ModernToggleSwitch;
import com.devendra.healthtracker.ui.components.SnapHorizontalScrollView;
import com.devendra.healthtracker.wellbeing.DailyWellbeingRecord;
import com.devendra.healthtracker.wellbeing.WellbeingHistoryManager;
import com.devendra.healthtracker.wellbeing.WellbeingManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import com.devendra.healthtracker.calendar.CalendarManager;
import android.text.TextUtils;
import java.util.Set;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.view.HapticFeedbackConstants;

import com.devendra.healthtracker.theme.ThemeManager;
import com.devendra.healthtracker.theme.ThemeToggleView;
import com.devendra.healthtracker.features.FeatureItem;
import com.devendra.healthtracker.features.FeatureRegistry;
import com.devendra.healthtracker.features.FeatureOrderManager;
import com.devendra.healthtracker.features.ReorderableFeatureGrid;
import com.devendra.healthtracker.ui.design.DesignTokens;
import com.devendra.healthtracker.ui.design.TouchFeedback;
import com.devendra.healthtracker.ui.components.PremiumButton;
import com.devendra.healthtracker.ui.components.PremiumIconButton;
import com.devendra.healthtracker.ui.components.PremiumCard;
import com.devendra.healthtracker.ui.components.PremiumSegmentedControl;
import com.devendra.healthtracker.ui.components.PremiumChip;
import com.devendra.healthtracker.ui.components.PremiumStepper;
import com.devendra.healthtracker.ui.components.PremiumCheckbox;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private SnapHorizontalScrollView pager;
    private View[] dotIndicators;
    private int screenWidth;

    // Theme & Navigation
    private boolean isDarkTheme = false;
    private ReorderableFeatureGrid featureGrid;
    private Button editLayoutBtn;
    private TextView launcherSecSub;
    private FrameLayout mainContainer;
    private ScrollView launcherScrollView;
    private FrameLayout featureCardContainer;
    private String currentOpenFeatureId = null;
    private final Map<String, View> featureViews = new HashMap<>();

    // Hero Command Center views on Launcher
    private TextView heroCadenceTitle;
    private TextView heroCadenceSub;
    private TextView heroCadenceBadge;
    private TextView heroQuickStatus;

    // Left Card (Checklist / To-Do) views
    private LinearLayout checklistItemsContainer;
    private EditText checklistInput;
    private String currentChecklistFilter = "ALL";
    private Button filterAllBtn;
    private Button filterActiveBtn;
    private Button filterDoneBtn;
    private TextView checklistStatsText;
    private ProgressBar checklistProgressBar;

    // Home Screen Views & State
    private View launcherContainer;
    private SwipeGestureLayout homeGestureContainer;
    private AestheticSceneryView homeSceneryView;
    private TextView homeNeverGiveUpTitle;
    private TextView homeNeverGiveUpSub;
    private TextView homeHeroCadenceView;
    private TextView homeClockView;
    private ModernToggleSwitch homeEngineSwitch;
    private TextView homeEngineStatusText;
    private SwipeGestureLayout featureSwipeLayout;

    // Calendar & Special Notes Views & State
    private Calendar calDisplayMonth = Calendar.getInstance();
    private String calSelectedDateKey = CalendarManager.getTodayKey();
    private TextView calMonthHeaderView;
    private LinearLayout calGridContainer;
    private TextView calDateHeaderView;
    private EditText calNoteInput;
    private TextView calCurrentNoteSnippet;
    private LinearLayout calNotesListContainer;
    private TextView homeCalendarNoteView;

    // Left Sidebar Drawer State & Views
    private boolean isSidebarOpen = false;
    private View sidebarScrim;
    private View sidebarPanel;
    private int sidebarWidth;
    private ModernToggleSwitch sidebarEngineSwitch;
    private TextView sidebarStatusText;
    private TextView launcherStatusPill;

    // Circadian Schedule Config views
    private TextView wakeTimeView;
    private TextView sleepTimeView;
    private TextView scheduleSummaryView;

    // Center Card (Cadence) views
    private TextView currentRomanView;
    private TextView currentTitleView;
    private TextView currentCountdownView;
    private TextView currentDirectiveView;
    private TextView currentWhyView;
    private LinearLayout phaseListContainer;
    private TextView routineProgressStatsView;
    private ProgressBar routineProgressBar;

    // Digital Wellbeing views
    private LinearLayout wellbeingListContainer;
    private TextView screenTimeTotalView;
    private TextView focusScoreView;
    private LinearLayout categoryBarContainer;
    private LinearLayout permissionBanner;
    private TextView wbFocusTimeView;
    private TextView wbSessionsView;
    private TextView wbTasksDoneView;
    private TextView wbAppActiveView;
    private LinearLayout sevenDayTrendsContainer;

    // Right Card (Biometrics) views
    private TextView hydrationTextView;
    private ProgressBar hydrationProgressBar;

    // Pomodoro views
    private TextView pomoTimerView;
    private TextView pomoPhaseBadgeView;
    private TextView pomoSessionView;
    private ProgressBar pomoProgressBar;
    private Button pomoToggleBtn;
    private Button pomoSoundToggleBtn;
    private Button pomoNotifToggleBtn;
    private TextView pomoStatsView;
    private TextView pomoParallelDietView;
    private PomodoroManager.OnPomodoroUpdateListener pomoListener;

    // System & Hardware Monitor views
    private TextView sysRamTitleView;
    private TextView sysRamSubtitleView;
    private View sysRamProgressBar;
    private TextView sysRamSubDetailsView;

    private TextView sysStorageTitleView;
    private TextView sysStorageSubtitleView;
    private View sysStorageProgressBar;
    private TextView sysStorageSubDetailsView;

    private TextView sysBatteryLevelView;
    private TextView sysBatteryStatusView;
    private TextView sysBatteryHealthBadgeView;
    private TextView sysBatteryTempView;
    private TextView sysBatteryVoltageView;
    private TextView sysBatteryTechView;

    private TextView sysNetworkCountBadge;
    private TextView sysNetworkSummaryView;
    private LinearLayout sysNetworkListContainer;

    private TextView sysBgAppsCountBadge;
    private LinearLayout sysBgAppsListContainer;
    private List<DeviceTelemetryManager.BackgroundAppInfo> cachedBgApps = new ArrayList<>();

    // Deep System Cleaner (CCleaner) views
    private TextView deepTotalJunkView;
    private TextView deepAppCacheSubView;
    private TextView deepThumbnailSubView;
    private TextView deepTempLogsSubView;
    private TextView perm1StatusView;
    private TextView perm2StatusView;
    private TextView perm3StatusView;
    private Button perm1Btn;
    private Button perm2Btn;
    private Button perm3Btn;
    private LinearLayout deepProgressLayout;
    private TextView deepProgressTitle;
    private ProgressBar deepProgressBar;
    private TextView deepProgressFreed;
    private Button deepProgressStopBtn;
    private Button runDeepCleanBtn;
    private Button runTempPurgeBtn;
    private LinearLayout deepAppListContainer;
    private TextView deepAppsCountBadge;
    private DeepCleanManager.JunkOverview currentJunkOverview;

    // Universal Media Downloader & Harvester (Card 8) views
    private EditText downloaderUrlInput;
    private TextView downloaderSourceBadge;
    private PremiumButton downloaderAnalyzeBtn;
    private ProgressBar downloaderLoadingBar;
    private TextView downloaderStatusView;
    private LinearLayout downloaderPreviewCard;
    private ImageView downloaderThumbView;
    private TextView downloaderTitleView;
    private TextView downloaderMetaBadgeView;
    private LinearLayout downloaderFormatsContainer;
    private LinearLayout downloaderQueueContainer;
    private TextView downloaderQueueBadge;
    private LinearLayout whatsappStatusContainer;
    private TextView whatsappStatusCountBadge;
    private PremiumButton igSessionBtn;
    private MediaDownloadManager.DownloadQueueListener downloadQueueListener;
    private com.devendra.healthtracker.calculator.CalculatorCardController calculatorCardController;

    private Handler tickerHandler;
    private Runnable tickerRunnable;
    private Handler telemetryHandler;
    private Runnable telemetryTicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isDarkTheme = ThemeManager.isDarkMode(this);
        ThemeManager.applyStatusBar(this, isDarkTheme);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;

        // Initialize dynamic circadian schedule from preferences
        ScheduleManager.init(this);

        // Ensure background alarms are registered if engine is enabled
        if (AppEngineManager.isAppEnabled(this)) {
            ScheduleManager.scheduleAllAlarms(this);
        }

        // Master FrameLayout supporting Left Sidebar drawer overlay
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(isDarkTheme));

        // 1. Navigation Container (Launcher Grid vs Active Main System Screen)
        mainContainer = new FrameLayout(this);
        mainContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // A. 3-Column Responsive Grid Launcher Screen (with Left Sidebar trigger header)
        launcherContainer = createLauncherContainer();
        mainContainer.addView(launcherContainer);

        // B. Active Main System Container (Opened as a full primary system, not a sub-base)
        featureCardContainer = new FrameLayout(this);
        featureCardContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        featureCardContainer.setVisibility(View.GONE);
        mainContainer.addView(featureCardContainer);

        rootLayout.addView(mainContainer);

        // 2. Left Sidebar Scrim Backdrop
        sidebarScrim = new View(this);
        sidebarScrim.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        sidebarScrim.setBackgroundColor(Color.argb(140, 0, 0, 0));
        sidebarScrim.setVisibility(View.GONE);
        sidebarScrim.setAlpha(0f);
        sidebarScrim.setOnClickListener(v -> closeSidebar());
        rootLayout.addView(sidebarScrim);

        // 3. Left Sidebar Drawer Panel (Hosts Edit, Light/Dark theme, Master Engine Toggle)
        sidebarPanel = createLeftSidebarView();
        rootLayout.addView(sidebarPanel);

        setContentView(rootLayout);

        // Keep legacy pager instance initialized for backward safety
        pager = new SnapHorizontalScrollView(this);
        pager.setTotalPages(10);

        // Map core capability views directly: Only Routine, Checklist, Cleaner active
        featureViews.put(FeatureRegistry.ID_ROUTINE, createCadenceContent());
        featureViews.put(FeatureRegistry.ID_CHECKLIST, createChecklistContent());
        featureViews.put(FeatureRegistry.ID_DEEPCLEAN, createDeepCleanContent());

        /* Disabled for now per user request - code preserved, not purged
        featureViews.put(FeatureRegistry.ID_POMODORO, createPomodoroContent());
        featureViews.put(FeatureRegistry.ID_WELLBEING, createWellbeingContent());
        featureViews.put(FeatureRegistry.ID_SYSMON, createSystemMonitorContent());
        featureViews.put(FeatureRegistry.ID_HYDRATION, createBiometricsContent());
        featureViews.put(FeatureRegistry.ID_BLUEPRINT, createBlueprintContent());
        featureViews.put(FeatureRegistry.ID_MEDIA, createMediaDownloaderContent());
        featureViews.put(FeatureRegistry.ID_CALCULATOR, createCalculatorContent());
        featureViews.put(FeatureRegistry.ID_CALENDAR, createCalendarContent());
        */

        // Download queue listener disabled for now
        // downloadQueueListener = this::renderDownloadQueue;
        // MediaDownloadManager.getInstance(this).registerListener(downloadQueueListener);

        // Handle initial target feature request from intent
        if (getIntent() != null) {
            if (getIntent().hasExtra("openPage")) {
                int p = getIntent().getIntExtra("openPage", -1);
                List<String> def = FeatureRegistry.getDefaultOrder();
                if (p >= 0 && p < def.size()) {
                    openFeature(def.get(p));
                }
            }
        }

        // Start live 1-second ticker
        startLiveTicker();
        updateEngineStateUI();
        updateHeroCadenceStatus();
    }

    @Override
    public void onBackPressed() {
        if (isSidebarOpen) {
            closeSidebar();
        } else if (currentOpenFeatureId != null) {
            closeFeatureAndReturnToGrid();
        } else if (featureGrid != null && featureGrid.isEditMode()) {
            featureGrid.setEditMode(false);
            updateEditButtonState(false);
        } else {
            super.onBackPressed();
        }
    }

    public void openSidebar() {
        if (isSidebarOpen || sidebarPanel == null || sidebarScrim == null) return;
        isSidebarOpen = true;
        sidebarScrim.bringToFront();
        sidebarPanel.bringToFront();
        sidebarScrim.setVisibility(View.VISIBLE);
        sidebarScrim.animate().alpha(1f).setDuration(220).start();

        sidebarPanel.setVisibility(View.VISIBLE);
        sidebarPanel.animate()
            .translationX(0f)
            .setDuration(250)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    public void closeSidebar() {
        if (!isSidebarOpen || sidebarPanel == null || sidebarScrim == null) return;
        isSidebarOpen = false;
        sidebarScrim.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            sidebarScrim.setVisibility(View.GONE);
        }).start();

        int width = sidebarWidth > 0 ? sidebarWidth : dp(285);
        sidebarPanel.animate()
            .translationX(-width)
            .setDuration(220)
            .setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> {
                sidebarPanel.setVisibility(View.GONE);
            }).start();
    }

    public void openFeature(String id) {
        if (id == null || !FeatureRegistry.hasFeature(id)) return;
        View page = featureViews.get(id);
        if (page == null) return;
        currentOpenFeatureId = id;

        featureCardContainer.removeAllViews();

        boolean isDark = isDarkTheme;

        // 1. Evocative Procedural Atmospheric Background Layer (Unique for each capability card)
        AtmosphericCardBackgroundView cardBg = new AtmosphericCardBackgroundView(this, id, isDark);
        featureCardContainer.addView(cardBg, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 2. Translucent Frosted Glass System Overlay Container
        LinearLayout contentWrapper = new LinearLayout(this);
        contentWrapper.setOrientation(LinearLayout.VERTICAL);
        contentWrapper.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Frosted Glass Primary System Bar (NO Back button in UI per user mandate)
        LinearLayout sysBar = new LinearLayout(this);
        sysBar.setOrientation(LinearLayout.HORIZONTAL);
        sysBar.setGravity(Gravity.CENTER_VERTICAL);
        sysBar.setPadding(dp(16), dp(10), dp(16), dp(10));
        sysBar.setBackgroundColor(isDark ? Color.argb(190, 15, 18, 28) : Color.argb(215, 245, 247, 250));

        FeatureItem item = FeatureRegistry.getFeature(id);
        TextView titleView = new TextView(this);
        titleView.setText(item != null ? item.getFullName() : "SYSTEM");
        titleView.setTextSize(14);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(isDark ? Color.WHITE : Color.parseColor("#0F172A"));
        titleView.setShadowLayer(dp(4), 0, dp(1), isDark ? Color.BLACK : Color.argb(120, 255, 255, 255));
        titleView.setPadding(dp(4), 0, 0, 0);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(tvLp);
        sysBar.addView(titleView);

        // Sidebar Button [☰] to access System Controls / Switch Capabilities
        PremiumIconButton menuBtn = new PremiumIconButton(this, "☰", isDark);
        menuBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
        TouchFeedback.attach(menuBtn, v -> openSidebar());
        sysBar.addView(menuBtn);

        contentWrapper.addView(sysBar);

        View hairline = new View(this);
        hairline.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        hairline.setBackgroundColor(isDark ? Color.argb(70, 255, 255, 255) : Color.argb(40, 0, 0, 0));
        contentWrapper.addView(hairline);

        // Full Screen System Content Scroll Container with Swipe-Back Gesture Container
        if (page.getParent() instanceof ViewGroup) {
            ((ViewGroup) page.getParent()).removeView(page);
        }
        page.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setPadding(dp(14), dp(10), dp(14), dp(28));
        scroll.addView(page);

        // Swiping back gesture container ("swiping back gesture should be enough")
        featureSwipeLayout = new SwipeGestureLayout(this);
        LinearLayout.LayoutParams fslLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        featureSwipeLayout.setLayoutParams(fslLp);
        featureSwipeLayout.addView(scroll);
        featureSwipeLayout.setOnSwipeListener(new SwipeGestureLayout.OnSwipeListener() {
            @Override
            public void onSwipeRight() {
                // Swipe from left to right acts as native swipe-back!
                closeFeatureAndReturnToGrid();
            }

            @Override
            public void onSwipeLeft() {
            }
        });

        contentWrapper.addView(featureSwipeLayout);
        featureCardContainer.addView(contentWrapper);

        if (launcherContainer != null) {
            launcherContainer.setVisibility(View.GONE);
        } else if (launcherScrollView != null) {
            launcherScrollView.setVisibility(View.GONE);
        }
        featureCardContainer.setVisibility(View.VISIBLE);

        // Trigger on-open feature refreshes for active features
        if (FeatureRegistry.ID_DEEPCLEAN.equals(id)) {
            refreshDeepCleanUI();
        } else if (FeatureRegistry.ID_CHECKLIST.equals(id)) {
            refreshChecklistUI();
        } else if (FeatureRegistry.ID_ROUTINE.equals(id)) {
            updateActivePhaseUI();
        }
    }

    public void closeFeatureAndReturnToGrid() {
        currentOpenFeatureId = null;
        featureCardContainer.setVisibility(View.GONE);
        if (launcherContainer != null) {
            launcherContainer.setVisibility(View.VISIBLE);
        } else {
            launcherScrollView.setVisibility(View.VISIBLE);
        }
        updateHeroCadenceStatus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            if (intent.hasExtra("openPage")) {
                int p = intent.getIntExtra("openPage", -1);
                List<String> def = FeatureRegistry.getDefaultOrder();
                if (p >= 0 && p < def.size()) {
                    openFeature(def.get(p));
                }
            }
            if (intent.getBooleanExtra("cleanComplete", false)) {
                int count = intent.getIntExtra("cleanedCount", 0);
                long freed = intent.getLongExtra("freedBytes", 0);
                Toast.makeText(this, "🎉 Deep Clean Complete! Freed " + DeepCleanManager.formatBytes(freed) + " across " + count + " apps!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChecklistUI();
        refreshDeepCleanUI();
        updateActivePhaseUI();
        updateSoundControlsUI();
        updateEngineStateUI();
        renderCadenceList();
        updateRoutineProgressUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void startTelemetryTracking() {
        if (telemetryHandler == null) {
            telemetryHandler = new Handler(Looper.getMainLooper());
        }
        stopTelemetryTracking();
        telemetryTicker = new Runnable() {
            @Override
            public void run() {
                WellbeingHistoryManager.recordActiveAppTime(MainActivity.this, 1);
                if (telemetryHandler != null) {
                    telemetryHandler.postDelayed(this, 60000);
                }
            }
        };
        telemetryHandler.postDelayed(telemetryTicker, 60000);
    }

    private void stopTelemetryTracking() {
        if (telemetryHandler != null && telemetryTicker != null) {
            telemetryHandler.removeCallbacks(telemetryTicker);
            telemetryTicker = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 707 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("saf_whatsapp_uri", uri.toString())
                            .apply();
                    refreshWhatsAppStatusesUI();
                    Toast.makeText(this, "✅ WhatsApp Status access granted!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to persist SAF permission", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tickerHandler != null && tickerRunnable != null) {
            tickerHandler.removeCallbacks(tickerRunnable);
        }
        if (downloadQueueListener != null) {
            MediaDownloadManager.getInstance(this).unregisterListener(downloadQueueListener);
        }
    }

    // ==========================================
    // 1. LAUNCHER CONTAINER & LEFT SIDEBAR DRAWER
    // ==========================================
    private View createLauncherContainer() {
        homeGestureContainer = new SwipeGestureLayout(this);
        homeGestureContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 1. Procedural Aesthetic Scenery Background (Mountains, stars/sun, calm horizon)
        homeSceneryView = new AestheticSceneryView(this, isDarkTheme);
        homeGestureContainer.addView(homeSceneryView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 2. Foreground Overlay
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setPadding(dp(20), dp(18), dp(20), dp(20));

        // A. Top Row: Time & Date + Special Daily Note below it (Removed top-right active/paused indicator)
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.VERTICAL);
        topRow.setPadding(0, dp(4), 0, dp(14));

        homeClockView = new TextView(this);
        homeClockView.setText(new SimpleDateFormat("HH:mm • EEEE, MMM d", Locale.getDefault()).format(new Date()));
        homeClockView.setTextSize(13);
        homeClockView.setTypeface(Typeface.DEFAULT_BOLD);
        homeClockView.setTextColor(isDarkTheme ? Color.parseColor("#F1F5F9") : Color.parseColor("#0F172A"));
        homeClockView.setShadowLayer(dp(4), 0, dp(1), isDarkTheme ? Color.argb(200, 0, 0, 0) : Color.argb(120, 255, 255, 255));
        topRow.addView(homeClockView);

        /* Calendar note disabled for now per user request - code preserved, not purged
        homeCalendarNoteView = new TextView(this);
        homeCalendarNoteView.setTextSize(11);
        homeCalendarNoteView.setTextColor(isDarkTheme ? Color.parseColor("#CBD5E1") : Color.parseColor("#475569"));
        homeCalendarNoteView.setShadowLayer(dp(3), 0, dp(1), isDarkTheme ? Color.BLACK : Color.WHITE);
        homeCalendarNoteView.setPadding(0, dp(3), 0, 0);
        homeCalendarNoteView.setSingleLine(true);
        homeCalendarNoteView.setEllipsize(TextUtils.TruncateAt.END);
        updateHomeCalendarNote();
        topRow.addView(homeCalendarNoteView);
        */

        TouchFeedback.attach(topRow, v -> openFeature(FeatureRegistry.ID_ROUTINE));

        overlay.addView(topRow);

        // B. Main Content Row: Left Hero (Never Give Up) + Right Vertical Stack (The Three Tiles)
        LinearLayout centerRow = new LinearLayout(this);
        centerRow.setOrientation(LinearLayout.HORIZONTAL);
        centerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        centerRow.setLayoutParams(crLp);

        // LEFT: Hero Inspiration Typography (Tap to open Main Routine)
        LinearLayout leftHero = new LinearLayout(this);
        leftHero.setOrientation(LinearLayout.VERTICAL);
        leftHero.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lhLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        lhLp.setMargins(0, 0, dp(12), 0);
        leftHero.setLayoutParams(lhLp);

        homeNeverGiveUpTitle = new TextView(this);
        homeNeverGiveUpTitle.setText("NEVER\nGIVE UP.");
        homeNeverGiveUpTitle.setTextSize(34);
        homeNeverGiveUpTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        homeNeverGiveUpTitle.setLetterSpacing(0.06f);
        homeNeverGiveUpTitle.setTextColor(isDarkTheme ? Color.WHITE : Color.parseColor("#0F172A"));
        homeNeverGiveUpTitle.setShadowLayer(dp(10), 0, dp(3), isDarkTheme ? Color.argb(230, 0, 0, 0) : Color.argb(90, 255, 255, 255));
        leftHero.addView(homeNeverGiveUpTitle);

        homeNeverGiveUpSub = new TextView(this);
        homeNeverGiveUpSub.setText("Small daily disciplines compound into greatness.");
        homeNeverGiveUpSub.setTextSize(13);
        homeNeverGiveUpSub.setTextColor(isDarkTheme ? Color.parseColor("#E2E8F0") : Color.parseColor("#334155"));
        homeNeverGiveUpSub.setShadowLayer(dp(4), 0, dp(1), isDarkTheme ? Color.BLACK : Color.WHITE);
        homeNeverGiveUpSub.setPadding(0, dp(8), 0, dp(14));
        leftHero.addView(homeNeverGiveUpSub);

        homeHeroCadenceView = new TextView(this);
        homeHeroCadenceView.setTextSize(11);
        homeHeroCadenceView.setTypeface(Typeface.DEFAULT_BOLD);
        homeHeroCadenceView.setTextColor(isDarkTheme ? Color.parseColor("#FDE047") : Color.parseColor("#D97706"));
        homeHeroCadenceView.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable cadBg = new GradientDrawable();
        cadBg.setColor(isDarkTheme ? Color.argb(180, 20, 24, 39) : Color.argb(210, 254, 243, 199));
        cadBg.setCornerRadius(dp(16));
        cadBg.setStroke(dp(1), isDarkTheme ? Color.argb(100, 253, 224, 71) : Color.argb(120, 217, 119, 6));
        homeHeroCadenceView.setBackground(cadBg);
        leftHero.addView(homeHeroCadenceView);

        TouchFeedback.attach(leftHero, v -> openFeature(FeatureRegistry.ID_ROUTINE));
        TouchFeedback.attach(homeHeroCadenceView, v -> openFeature(FeatureRegistry.ID_ROUTINE));

        centerRow.addView(leftHero);

        // RIGHT: The Three Tiles ("these three tiles should be in right")
        LinearLayout rightTilesCol = new LinearLayout(this);
        rightTilesCol.setOrientation(LinearLayout.VERTICAL);
        rightTilesCol.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(
            dp(136), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rightTilesCol.setLayoutParams(rcLp);

        // Tile 1: Master Engine
        LinearLayout tile1 = new LinearLayout(this);
        tile1.setOrientation(LinearLayout.VERTICAL);
        tile1.setBackground(createFrostedTileBackground(isDarkTheme));
        tile1.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams t1Lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        t1Lp.setMargins(0, 0, 0, dp(10));
        tile1.setLayoutParams(t1Lp);

        TextView t1Title = new TextView(this);
        t1Title.setText("⚡ Engine");
        t1Title.setTextSize(12);
        t1Title.setTypeface(Typeface.DEFAULT_BOLD);
        t1Title.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        tile1.addView(t1Title);

        homeEngineSwitch = new ModernToggleSwitch(this);
        homeEngineSwitch.setChecked(AppEngineManager.isAppEnabled(this), false);
        homeEngineSwitch.setDarkTheme(isDarkTheme);
        homeEngineSwitch.setOnCheckedChangeListener((sw, isChecked) -> setAppEngineState(isChecked));
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(42), dp(24));
        swLp.setMargins(0, dp(6), 0, dp(4));
        homeEngineSwitch.setLayoutParams(swLp);
        tile1.addView(homeEngineSwitch);

        homeEngineStatusText = new TextView(this);
        homeEngineStatusText.setText(AppEngineManager.isAppEnabled(this) ? "🟢 Active" : "⏸️ Paused");
        homeEngineStatusText.setTextSize(10);
        homeEngineStatusText.setTypeface(Typeface.DEFAULT_BOLD);
        homeEngineStatusText.setTextColor(AppEngineManager.isAppEnabled(this) ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        tile1.addView(homeEngineStatusText);

        rightTilesCol.addView(tile1);

        // Tile 2: Theme Mode (Light / Dark)
        LinearLayout tile2 = new LinearLayout(this);
        tile2.setOrientation(LinearLayout.VERTICAL);
        tile2.setBackground(createFrostedTileBackground(isDarkTheme));
        tile2.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams t2Lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        t2Lp.setMargins(0, 0, 0, dp(10));
        tile2.setLayoutParams(t2Lp);

        TextView t2Title = new TextView(this);
        t2Title.setText("🌗 Theme");
        t2Title.setTextSize(12);
        t2Title.setTypeface(Typeface.DEFAULT_BOLD);
        t2Title.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        tile2.addView(t2Title);

        TextView t2Sub = new TextView(this);
        t2Sub.setText(isDarkTheme ? "🌙 Obsidian" : "☀️ Slate");
        t2Sub.setTextSize(11);
        t2Sub.setTextColor(isDarkTheme ? Color.parseColor("#94A3B8") : Color.parseColor("#475569"));
        t2Sub.setPadding(0, dp(4), 0, 0);
        tile2.addView(t2Sub);

        TouchFeedback.attach(tile2, v -> {
            ThemeManager.setDarkMode(this, !isDarkTheme);
            recreate();
        });
        rightTilesCol.addView(tile2);

        // Tile 3: System Capabilities
        LinearLayout tile3 = new LinearLayout(this);
        tile3.setOrientation(LinearLayout.VERTICAL);
        tile3.setBackground(createFrostedTileBackground(isDarkTheme));
        tile3.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams t3Lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tile3.setLayoutParams(t3Lp);

        TextView t3Title = new TextView(this);
        t3Title.setText("☰ Systems");
        t3Title.setTextSize(12);
        t3Title.setTypeface(Typeface.DEFAULT_BOLD);
        t3Title.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        tile3.addView(t3Title);

        TextView t3Sub = new TextView(this);
        t3Sub.setText("3 Systems →");
        t3Sub.setTextSize(11);
        t3Sub.setTextColor(isDarkTheme ? Color.parseColor("#F43F5E") : Color.parseColor("#E11D48"));
        t3Sub.setPadding(0, dp(4), 0, 0);
        tile3.addView(t3Sub);

        TouchFeedback.attach(tile3, v -> openSidebar());
        rightTilesCol.addView(tile3);

        centerRow.addView(rightTilesCol);
        overlay.addView(centerRow);

        // C. Bottom Navigation Hint
        TextView bottomHint = new TextView(this);
        bottomHint.setText("👉 Swipe right anywhere to open Systems (Routine, Checklist, Cleaner)");
        bottomHint.setTextSize(11);
        bottomHint.setTextColor(isDarkTheme ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        bottomHint.setShadowLayer(dp(3), 0, dp(1), isDarkTheme ? Color.BLACK : Color.WHITE);
        bottomHint.setPadding(0, dp(8), 0, dp(4));
        overlay.addView(bottomHint);

        homeGestureContainer.addView(overlay);

        // 3. Swipe Gesture Detection on Home Screen ("swiping on it from left to right should open the panel")
        homeGestureContainer.setOnSwipeListener(new SwipeGestureLayout.OnSwipeListener() {
            @Override
            public void onSwipeRight() {
                openSidebar();
            }

            @Override
            public void onSwipeLeft() {
                if (isSidebarOpen) {
                    closeSidebar();
                }
            }
        });

        return homeGestureContainer;
    }

    private GradientDrawable createFrostedTileBackground(boolean isDark) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isDark ? Color.argb(215, 18, 23, 34) : Color.argb(225, 255, 255, 255));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), isDark ? Color.argb(45, 255, 255, 255) : Color.argb(60, 0, 0, 0));
        return gd;
    }

    private View createLeftSidebarView() {
        sidebarWidth = dp(285);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams pLp = new FrameLayout.LayoutParams(
            sidebarWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.TOP
        );
        panel.setLayoutParams(pLp);
        panel.setBackgroundColor(isDarkTheme ? ThemeManager.BG_DARK : Color.WHITE);
        panel.setTranslationX(-sidebarWidth);
        panel.setVisibility(View.GONE);
        panel.setClickable(true);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(16), dp(14), dp(14), dp(12));
        headerRow.setBackgroundColor(ThemeManager.getTopBarBackgroundColor(isDarkTheme));

        TextView headerTitle = new TextView(this);
        headerTitle.setText("⚙️ SYSTEM CONTROLS");
        headerTitle.setTextSize(14);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerTitle.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerTitle.setLayoutParams(htLp);
        headerRow.addView(headerTitle);

        PremiumIconButton closeBtn = new PremiumIconButton(this, "✕", isDarkTheme);
        closeBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        closeBtn.setOnClickListener(v -> closeSidebar());
        headerRow.addView(closeBtn);

        panel.addView(headerRow);

        View headerHairline = new View(this);
        headerHairline.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        headerHairline.setBackgroundColor(ThemeManager.getHairlineColor(isDarkTheme));
        panel.addView(headerHairline);

        // Scrollable controls
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);
        controlsLayout.setPadding(dp(14), dp(14), dp(14), dp(24));

        // 1. Master System Engine (That Toggle!)
        LinearLayout engineCard = createCardLayout(
            isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
            isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
        );
        engineCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        engineCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ecLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ecLp.setMargins(0, 0, 0, dp(12));
        engineCard.setLayoutParams(ecLp);

        LinearLayout engineRow = new LinearLayout(this);
        engineRow.setOrientation(LinearLayout.HORIZONTAL);
        engineRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView engTitle = new TextView(this);
        engTitle.setText("Master Engine");
        engTitle.setTextSize(13);
        engTitle.setTypeface(Typeface.DEFAULT_BOLD);
        engTitle.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        engTitle.setLayoutParams(etLp);
        engineRow.addView(engTitle);

        sidebarEngineSwitch = new ModernToggleSwitch(this);
        sidebarEngineSwitch.setChecked(AppEngineManager.isAppEnabled(this), false);
        sidebarEngineSwitch.setDarkTheme(isDarkTheme);
        sidebarEngineSwitch.setOnCheckedChangeListener((sw, isChecked) -> setAppEngineState(isChecked));
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(44), dp(26));
        sidebarEngineSwitch.setLayoutParams(swLp);
        engineRow.addView(sidebarEngineSwitch);
        engineCard.addView(engineRow);

        TextView engDesc = new TextView(this);
        engDesc.setText("Background alarms & cadence");
        engDesc.setTextSize(11);
        engDesc.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF64748B);
        engDesc.setPadding(0, dp(4), 0, dp(2));
        engineCard.addView(engDesc);

        sidebarStatusText = new TextView(this);
        sidebarStatusText.setText(AppEngineManager.isAppEnabled(this) ? "🟢 ACTIVE & SCHEDULED" : "⏸️ ENGINE PAUSED");
        sidebarStatusText.setTextSize(10);
        sidebarStatusText.setTypeface(Typeface.DEFAULT_BOLD);
        sidebarStatusText.setTextColor(AppEngineManager.isAppEnabled(this) ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        engineCard.addView(sidebarStatusText);

        controlsLayout.addView(engineCard);

        // 2. Light / Dark Theme Edit
        LinearLayout themeCard = createCardLayout(
            isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
            isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
        );
        themeCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        themeCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tcLp.setMargins(0, 0, 0, dp(12));
        themeCard.setLayoutParams(tcLp);

        TextView themeTitle = new TextView(this);
        themeTitle.setText("Theme Mode");
        themeTitle.setTextSize(13);
        themeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        themeTitle.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        themeCard.addView(themeTitle);

        TextView themeDesc = new TextView(this);
        themeDesc.setText("Obsidian Midnight vs Slate Light");
        themeDesc.setTextSize(11);
        themeDesc.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF64748B);
        themeDesc.setPadding(0, dp(2), 0, dp(8));
        themeCard.addView(themeDesc);

        ThemeToggleView themeToggle = new ThemeToggleView(this);
        themeToggle.setDarkMode(isDarkTheme, false);
        themeToggle.setOnThemeChangeListener(newDark -> {
            ThemeManager.setDarkMode(this, newDark);
            recreate();
        });
        LinearLayout.LayoutParams ttLp = new LinearLayout.LayoutParams(dp(120), dp(30));
        themeToggle.setLayoutParams(ttLp);
        themeCard.addView(themeToggle);

        controlsLayout.addView(themeCard);

        // 3. Grid Customization (Edit Mode)
        LinearLayout editCard = createCardLayout(
            isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
            isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
        );
        editCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        editCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ec2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ec2Lp.setMargins(0, 0, 0, dp(14));
        editCard.setLayoutParams(ec2Lp);

        TextView editTitle = new TextView(this);
        editTitle.setText("Grid Customization");
        editTitle.setTextSize(13);
        editTitle.setTypeface(Typeface.DEFAULT_BOLD);
        editTitle.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
        editCard.addView(editTitle);

        TextView editDesc = new TextView(this);
        editDesc.setText("Reorder & swap capability tiles");
        editDesc.setTextSize(11);
        editDesc.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF64748B);
        editDesc.setPadding(0, dp(2), 0, dp(8));
        editCard.addView(editDesc);

        editLayoutBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
        editLayoutBtn.setText(featureGrid != null && featureGrid.isEditMode() ? "✓ Done Editing" : "Edit Grid Tiles");
        editLayoutBtn.setTextSize(12);
        editLayoutBtn.setPadding(dp(14), dp(6), dp(14), dp(6));
        updateEditButtonState(featureGrid != null && featureGrid.isEditMode());
        editLayoutBtn.setOnClickListener(v -> {
            if (featureGrid != null) {
                boolean newEdit = !featureGrid.isEditMode();
                featureGrid.setEditMode(newEdit);
                updateEditButtonState(newEdit);
                closeSidebar();
            }
        });
        LinearLayout.LayoutParams ebLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        editLayoutBtn.setLayoutParams(ebLp);
        editCard.addView(editLayoutBtn);

        /* Grid Customization disabled for 3-capability minimal profile
        controlsLayout.addView(editCard);
        */

        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div.setBackgroundColor(ThemeManager.getHairlineColor(isDarkTheme));
        controlsLayout.addView(div);

        // 4. Quick Navigation List
        TextView navHeader = new TextView(this);
        navHeader.setText("SYSTEM CAPABILITIES");
        navHeader.setTextSize(10);
        navHeader.setTypeface(Typeface.DEFAULT_BOLD);
        navHeader.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF64748B);
        navHeader.setPadding(0, dp(12), 0, dp(8));
        controlsLayout.addView(navHeader);

        List<FeatureItem> sortedFeatures = new ArrayList<>(FeatureRegistry.getAllFeatures());
        Collections.sort(sortedFeatures, (a, b) -> Integer.compare(a.getDefaultPriority(), b.getDefaultPriority()));

        for (FeatureItem item : sortedFeatures) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackground(createOutlinedDrawable(
                isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
                isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0"),
                dp(10)
            ));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowLp.setMargins(0, 0, 0, dp(6));
            row.setLayoutParams(rowLp);

            String emoji = "";
            String[] parts = item.getFullName().split(" ", 2);
            if (parts.length > 0) {
                emoji = parts[0] + " ";
            }

            TextView rName = new TextView(this);
            rName.setText(emoji + item.getShortName());
            rName.setTextSize(13);
            rName.setTypeface(Typeface.DEFAULT_BOLD);
            rName.setTextColor(ThemeManager.getTextBrightColor(isDarkTheme));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            rName.setLayoutParams(rLp);
            row.addView(rName);

            TextView rSub = new TextView(this);
            rSub.setText(item.getSubtitle());
            rSub.setTextSize(11);
            rSub.setTextColor(isDarkTheme ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
            rSub.setPadding(dp(6), 0, dp(8), 0);
            row.addView(rSub);

            TextView arrow = new TextView(this);
            arrow.setText("→");
            arrow.setTextSize(13);
            arrow.setTypeface(Typeface.DEFAULT_BOLD);
            arrow.setTextColor(isDarkTheme ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8"));
            row.addView(arrow);

            TouchFeedback.attach(row, v -> {
                closeSidebar();
                openFeature(item.getId());
            });

            controlsLayout.addView(row);
        }

        scroll.addView(controlsLayout);
        panel.addView(scroll);

        return panel;
    }

    private void updateEditButtonState(boolean isEditing) {
        if (editLayoutBtn == null) return;
        if (editLayoutBtn instanceof PremiumButton) {
            PremiumButton pb = (PremiumButton) editLayoutBtn;
            if (isEditing) {
                pb.setStyle(PremiumButton.Style.PRIMARY);
                pb.setText("✓ Done Editing");
            } else {
                pb.setStyle(PremiumButton.Style.SECONDARY);
                pb.setText("Edit Grid Tiles");
            }
        } else {
            editLayoutBtn.setText(isEditing ? "✓ Done Editing" : "Edit Grid Tiles");
        }
    }

    private View createLauncherScreen() {
        launcherScrollView = new ScrollView(this);
        launcherScrollView.setVerticalScrollBarEnabled(false);
        launcherScrollView.setFillViewport(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, dp(32));

        // 1. Hero Command Center Card
        LinearLayout heroCard = createCardLayout(
            isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
            isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
        );
        heroCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(dp(14), dp(12), dp(14), dp(12));
        heroCard.setLayoutParams(heroLp);

        LinearLayout heroTopRow = new LinearLayout(this);
        heroTopRow.setOrientation(LinearLayout.HORIZONTAL);
        heroTopRow.setGravity(Gravity.CENTER_VERTICAL);

        heroCadenceBadge = new TextView(this);
        heroCadenceBadge.setText("CADENCE COMMAND CENTER");
        heroCadenceBadge.setTextSize(10);
        heroCadenceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        heroCadenceBadge.setTextColor(isDarkTheme ? 0xFFFB7185 : 0xFFE11D48);
        heroCadenceBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(ThemeManager.getBadgeBackground(isDarkTheme, "#E11D48"));
        badgeBg.setStroke(dp(1), ThemeManager.getBadgeBorder(isDarkTheme, "#E11D48"));
        badgeBg.setCornerRadius(dp(8));
        heroCadenceBadge.setBackground(badgeBg);
        heroTopRow.addView(heroCadenceBadge);

        View spacer = new View(this);
        heroTopRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        TextView dateView = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        dateView.setText(sdf.format(new Date()));
        dateView.setTextSize(11);
        dateView.setTypeface(Typeface.DEFAULT_BOLD);
        dateView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF64748B);
        heroTopRow.addView(dateView);

        TextView chevron = new TextView(this);
        chevron.setText(" →");
        chevron.setTextSize(12);
        chevron.setTypeface(Typeface.DEFAULT_BOLD);
        chevron.setTextColor(isDarkTheme ? 0xFFFB7185 : 0xFFE11D48);
        heroTopRow.addView(chevron);

        heroCard.addView(heroTopRow);

        if (heroCard instanceof PremiumCard) {
            ((PremiumCard) heroCard).setInteractive(true);
            heroCard.setOnClickListener(v -> openFeature(FeatureRegistry.ID_ROUTINE));
        }

        heroCadenceTitle = new TextView(this);
        heroCadenceTitle.setText("PHASE VII • DIGESTION");
        heroCadenceTitle.setTextSize(17);
        heroCadenceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        heroCadenceTitle.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : 0xFF0F172A);
        heroCadenceTitle.setPadding(0, dp(6), 0, dp(2));
        heroCard.addView(heroCadenceTitle);

        heroCadenceSub = new TextView(this);
        heroCadenceSub.setText("All biological cadence alarms and focus monitoring active");
        heroCadenceSub.setTextSize(12);
        heroCadenceSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF64748B);
        heroCard.addView(heroCadenceSub);

        heroQuickStatus = new TextView(this);
        heroQuickStatus.setText("⚡ System Status: Optimal • 10 Capabilities Ready");
        heroQuickStatus.setTextSize(11);
        heroQuickStatus.setTypeface(Typeface.MONOSPACE);
        heroQuickStatus.setTextColor(isDarkTheme ? 0xFF34D399 : 0xFF10B981);
        heroQuickStatus.setPadding(0, dp(6), 0, 0);
        heroCard.addView(heroQuickStatus);

        layout.addView(heroCard);

        // 2. Section Header: Capabilities & Tools
        LinearLayout secHeader = new LinearLayout(this);
        secHeader.setOrientation(LinearLayout.HORIZONTAL);
        secHeader.setGravity(Gravity.CENTER_VERTICAL);
        secHeader.setPadding(dp(16), dp(4), dp(16), dp(6));

        TextView secTitle = new TextView(this);
        secTitle.setText("CAPABILITIES & TOOLS");
        secTitle.setTextSize(11);
        secTitle.setTypeface(Typeface.DEFAULT_BOLD);
        secTitle.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF64748B);
        secHeader.addView(secTitle);

        View secSpacer = new View(this);
        secHeader.addView(secSpacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        launcherSecSub = new TextView(this);
        launcherSecSub.setText("3-Column Grid • Tap Edit to swap");
        launcherSecSub.setTextSize(10);
        launcherSecSub.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF94A3B8);
        secHeader.addView(launcherSecSub);

        layout.addView(secHeader);

        // 3. 3-Column Reorderable Grid
        featureGrid = new ReorderableFeatureGrid(this);
        List<String> order = FeatureOrderManager.getOrder(this);
        List<FeatureItem> orderedItems = new ArrayList<>();
        for (String id : order) {
            FeatureItem fi = FeatureRegistry.getFeature(id);
            if (fi != null) orderedItems.add(fi);
        }
        featureGrid.setFeatures(orderedItems, isDarkTheme);
        featureGrid.setFeatureListener(feature -> openFeature(feature.getId()));
        featureGrid.setOrderListener(new ReorderableFeatureGrid.OnOrderChangeListener() {
            @Override
            public void onOrderChanged(List<String> newOrder) {
                // Order persisted automatically
            }

            @Override
            public void onEditModeChanged(boolean isEditing) {
                updateEditButtonState(isEditing);
                if (launcherSecSub != null) {
                    if (isEditing) {
                        launcherSecSub.setText("⇄ Tap any 2 cards to swap");
                        launcherSecSub.setTextColor(isDarkTheme ? 0xFFFB7185 : 0xFFE11D48);
                    } else {
                        launcherSecSub.setText("3-Column Grid • Tap Edit to swap");
                        launcherSecSub.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF94A3B8);
                    }
                }
            }

            @Override
            public void onSwapSelectionChanged(FeatureItem selectedItem) {
                if (launcherSecSub != null && featureGrid != null && featureGrid.isEditMode()) {
                    if (selectedItem != null) {
                        launcherSecSub.setText("⇄ 1st: " + selectedItem.getShortName() + " • Tap 2nd card to swap");
                        launcherSecSub.setTextColor(isDarkTheme ? 0xFF818CF8 : 0xFF4F46E5);
                    } else {
                        launcherSecSub.setText("⇄ Tap any 2 cards to swap");
                        launcherSecSub.setTextColor(isDarkTheme ? 0xFFFB7185 : 0xFFE11D48);
                    }
                }
            }
        });
        layout.addView(featureGrid);

        launcherScrollView.addView(layout);
        return launcherScrollView;
    }

    private void updateHeroCadenceStatus() {
        ScheduleManager.Phase current = ScheduleManager.getCurrentPhase();
        if (current != null) {
            String title = current.romanWindow + " • " + current.getDisplayTitle();
            if (heroCadenceTitle != null) {
                heroCadenceTitle.setText(title);
            }
            if (heroCadenceSub != null) {
                heroCadenceSub.setText("What to do: " + current.getDisplayProtocol());
            }
            if (homeHeroCadenceView != null) {
                homeHeroCadenceView.setText("⏰ " + title);
            }
        }
    }

    private void updateDotIndicators(int activePage) {
        // Main screen uses 3-column launcher grid
    }

    // ==========================================
    // 2. FLASHCARD CONTAINER WRAPPER
    // ==========================================
    private View wrapInFlashcard(View content, String badgeTitle, String accentColor) {
        // Page container: MATCH_PARENT width
        LinearLayout pageWrapper = new LinearLayout(this);
        pageWrapper.setOrientation(LinearLayout.VERTICAL);
        pageWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pageWrapper.setPadding(dp(12), dp(4), dp(12), dp(16));

        // The Physical Flashcard Frame
        LinearLayout cardFrame = new LinearLayout(this);
        cardFrame.setOrientation(LinearLayout.VERTICAL);
        cardFrame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Elevated Card Background with 24dp rounded corners and subtle stroke
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE);
        cardBg.setCornerRadius(dp(24));
        cardBg.setStroke(dp(1), isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0"));
        cardFrame.setBackground(cardBg);
        cardFrame.setPadding(dp(16), dp(16), dp(16), dp(12));

        // Inner Card Header Pill
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(0, 0, 0, dp(12));

        TextView badge = new TextView(this);
        badge.setText(badgeTitle);
        badge.setTextSize(10);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        int badgeFg = isDarkTheme ? ThemeManager.parseColorHex(accentColor, 0xFFFB7185) : Color.parseColor(accentColor);
        badge.setTextColor(badgeFg);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(ThemeManager.getBadgeBackground(isDarkTheme, accentColor));
        badgeBg.setStroke(dp(1), ThemeManager.getBadgeBorder(isDarkTheme, accentColor));
        badgeBg.setCornerRadius(dp(12));
        badge.setBackground(badgeBg);
        badgeRow.addView(badge);

        cardFrame.addView(badgeRow);

        // Scrollable content inside the card
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scroll.setVerticalScrollBarEnabled(false);
        if (content.getParent() instanceof ViewGroup) {
            ((ViewGroup) content.getParent()).removeView(content);
        }
        scroll.addView(content);
        cardFrame.addView(scroll);

        pageWrapper.addView(cardFrame);
        return pageWrapper;
    }

    // ==========================================
    // 2B. CARD 0: CHECKLIST & TO-DO CONTENT
    // ==========================================
    private View createChecklistContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // Header summary card
        LinearLayout summaryCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        summaryCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        summaryCard.setOrientation(LinearLayout.VERTICAL);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("TASK & SHOPPING MATRIX");
        headerTitle.setTextSize(11);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerTitle.setTextColor(Color.parseColor("#10B981"));
        summaryCard.addView(headerTitle);

        checklistStatsText = new TextView(this);
        checklistStatsText.setText("0 of 0 tasks completed");
        checklistStatsText.setTextSize(14);
        checklistStatsText.setTypeface(Typeface.DEFAULT_BOLD);
        checklistStatsText.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        checklistStatsText.setPadding(0, dp(2), 0, dp(6));
        summaryCard.addView(checklistStatsText);

        checklistProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        checklistProgressBar.setMax(100);
        checklistProgressBar.setProgress(0);
        checklistProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        checklistProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(isDarkTheme ? 0xFF333A48 : Color.parseColor("#E2E8F0")));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        checklistProgressBar.setLayoutParams(pbLp);
        summaryCard.addView(checklistProgressBar);

        content.addView(summaryCard);

        // Quick Add Box
        LinearLayout addCard = createCardLayout(Color.WHITE, Color.parseColor("#CBD5E1"));
        addCard.setOrientation(LinearLayout.HORIZONTAL);
        addCard.setGravity(Gravity.CENTER_VERTICAL);
        addCard.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addLp.setMargins(0, dp(12), 0, dp(12));
        addCard.setLayoutParams(addLp);

        checklistInput = new EditText(this);
        checklistInput.setHint("Add task, grocery item, or idea...");
        checklistInput.setHintTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
        checklistInput.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        checklistInput.setTextSize(13);
        checklistInput.setBackgroundColor(Color.TRANSPARENT);
        checklistInput.setSingleLine(true);
        checklistInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        checklistInput.setOnEditorActionListener((v, actionId, event) -> {
            addChecklistItem();
            return true;
        });
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        checklistInput.setLayoutParams(inLp);
        addCard.addView(checklistInput);

        PremiumButton addBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
        addBtn.setText("+ Add Task");
        addBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
        addBtn.setOnClickListener(v -> addChecklistItem());
        addCard.addView(addBtn);

        content.addView(addCard);

        Button syncRoutineBtn = createOutlinedButton("📥 Import Today's Routine Blocks & Ingredients", Color.parseColor("#0284C7"), Color.parseColor("#BAE6FD"));
        LinearLayout.LayoutParams srbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srbLp.setMargins(0, 0, 0, dp(10));
        syncRoutineBtn.setLayoutParams(srbLp);
        syncRoutineBtn.setTextSize(11);
        syncRoutineBtn.setOnClickListener(v -> {
            int added = RoutineCompletionManager.importBlocksToChecklist(this, ScheduleManager.getAllGranularBlocks());
            Toast.makeText(this, "Imported " + added + " routine items into Tasks", Toast.LENGTH_SHORT).show();
            refreshChecklistUI();
            refreshWellbeingData();
        });
        content.addView(syncRoutineBtn);

        // Filter Bar (All / Active / Done)
        LinearLayout filterBar = new LinearLayout(this);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);
        filterBar.setPadding(dp(2), 0, dp(2), dp(10));

        filterAllBtn = createFilterButton("All (0)", "ALL");
        filterActiveBtn = createFilterButton("Active (0)", "ACTIVE");
        filterDoneBtn = createFilterButton("Done (0)", "COMPLETED");

        filterBar.addView(filterAllBtn);
        filterBar.addView(filterActiveBtn);
        filterBar.addView(filterDoneBtn);
        content.addView(filterBar);

        // Tasks Container
        checklistItemsContainer = new LinearLayout(this);
        checklistItemsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(checklistItemsContainer);

        // Clear completed button
        Button clearBtn = createOutlinedButton("🗑️ Clear Completed Items", Color.parseColor("#DC2626"), Color.parseColor("#FECDD3"));
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.setMargins(0, dp(14), 0, 0);
        clearBtn.setLayoutParams(cbLp);
        clearBtn.setOnClickListener(v -> {
            int removed = ChecklistManager.clearCompleted(this);
            Toast.makeText(this, removed > 0 ? ("Cleared " + removed + " completed items") : "No completed items to clear", Toast.LENGTH_SHORT).show();
            refreshChecklistUI();
            refreshWellbeingData();
        });
        content.addView(clearBtn);

        return content;
    }

    private Button createFilterButton(String text, String filterKey) {
        PremiumButton b = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
        b.setText(text);
        b.setTextSize(11);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        b.setPadding(dp(4), dp(6), dp(4), dp(6));
        updateFilterButtonAppearance(b, filterKey);
        b.setOnClickListener(v -> {
            currentChecklistFilter = filterKey;
            updateFilterButtonAppearance(filterAllBtn, "ALL");
            updateFilterButtonAppearance(filterActiveBtn, "ACTIVE");
            updateFilterButtonAppearance(filterDoneBtn, "COMPLETED");
            refreshChecklistUI();
        });
        return b;
    }

    private void updateFilterButtonAppearance(Button b, String filterKey) {
        if (b == null) return;
        boolean isSelected = filterKey.equals(currentChecklistFilter);
        if (b instanceof PremiumButton) {
            PremiumButton pb = (PremiumButton) b;
            pb.setStyle(isSelected ? PremiumButton.Style.SUCCESS : PremiumButton.Style.SECONDARY);
        } else {
            b.setTextColor(isSelected ? Color.WHITE : (isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B")));
        }
    }

    private void addChecklistItem() {
        if (checklistInput == null) return;
        String text = checklistInput.getText().toString().trim();
        if (text.isEmpty()) return;
        ChecklistManager.addItem(this, text);
        checklistInput.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(checklistInput.getWindowToken(), 0);
        }
        refreshChecklistUI();
        refreshWellbeingData();
    }

    private void refreshChecklistUI() {
        if (checklistItemsContainer == null) return;
        checklistItemsContainer.removeAllViews();

        List<ChecklistItem> all = ChecklistManager.getItems(this);
        int total = all.size();
        int completed = 0;
        for (ChecklistItem i : all) {
            if (i.completed) completed++;
        }
        int active = total - completed;

        if (checklistStatsText != null) {
            int pct = total > 0 ? (completed * 100 / total) : 0;
            checklistStatsText.setText(completed + " of " + total + " tasks completed (" + pct + "%)");
        }
        if (checklistProgressBar != null) {
            int pct = total > 0 ? (completed * 100 / total) : 0;
            checklistProgressBar.setProgress(pct);
        }

        if (filterAllBtn != null) filterAllBtn.setText("All (" + total + ")");
        if (filterActiveBtn != null) filterActiveBtn.setText("Active (" + active + ")");
        if (filterDoneBtn != null) filterDoneBtn.setText("Done (" + completed + ")");

        List<ChecklistItem> displayed = ChecklistManager.filter(all, currentChecklistFilter);

        if (displayed.isEmpty()) {
            LinearLayout emptyCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            emptyCard.setPadding(dp(16), dp(20), dp(16), dp(20));
            emptyCard.setGravity(Gravity.CENTER);
            emptyCard.setOrientation(LinearLayout.VERTICAL);

            TextView icon = new TextView(this);
            icon.setText("📋");
            icon.setTextSize(28);
            icon.setGravity(Gravity.CENTER);
            emptyCard.addView(icon);

            TextView emptyTitle = new TextView(this);
            emptyTitle.setText("No items in this list");
            emptyTitle.setTextSize(13);
            emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            emptyTitle.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            emptyTitle.setGravity(Gravity.CENTER);
            emptyTitle.setPadding(0, dp(4), 0, 0);
            emptyCard.addView(emptyTitle);

            TextView emptyDesc = new TextView(this);
            emptyDesc.setText("Add tasks above to organize your day.");
            emptyDesc.setTextSize(11);
            emptyDesc.setTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
            emptyDesc.setGravity(Gravity.CENTER);
            emptyCard.addView(emptyDesc);

            checklistItemsContainer.addView(emptyCard);
            return;
        }

        for (ChecklistItem item : displayed) {
            LinearLayout row = createCardLayout(Color.WHITE, Color.parseColor(item.completed ? "#F8FAFC" : "#FFFFFF"));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rlp);

            // Checkbox / Check circle button
            PremiumCheckbox checkCb = new PremiumCheckbox(this, isDarkTheme);
            checkCb.setChecked(item.completed, false);
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(dp(26), dp(26));
            cbLp.setMargins(0, 0, dp(10), 0);
            checkCb.setLayoutParams(cbLp);
            checkCb.setOnCheckedChangeListener((cb, isChecked) -> {
                ChecklistManager.toggleItem(this, item.id);
                refreshChecklistUI();
                refreshWellbeingData();
            });
            row.addView(checkCb);

            // Text title
            TextView titleView = new TextView(this);
            titleView.setText(item.title);
            titleView.setTextSize(13);
            if (item.completed) {
                titleView.setPaintFlags(titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                titleView.setTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
            } else {
                titleView.setPaintFlags(titleView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                titleView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
                titleView.setTypeface(Typeface.DEFAULT_BOLD);
            }
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleView.setLayoutParams(tvLp);
            titleView.setOnClickListener(v -> checkCb.performClick());
            row.addView(titleView);

            // Delete Button (✕)
            PremiumIconButton delBtn = new PremiumIconButton(this, "✕", isDarkTheme);
            delBtn.setTextSize(12);
            delBtn.setDestructive(true);
            delBtn.setOnClickListener(v -> {
                ChecklistManager.deleteItem(this, item.id);
                refreshChecklistUI();
            });
            row.addView(delBtn);

            checklistItemsContainer.addView(row);
        }
    }

    private GradientDrawable createOutlinedDrawable(int bgColor, int strokeColor, int radius) {
        int finalBg;
        int finalStroke;
        if (isDarkTheme) {
            if (bgColor == Color.WHITE || bgColor == 0xFFFFFFFF) {
                finalBg = Color.argb(175, 16, 20, 32);
            } else {
                int r = (bgColor >> 16) & 0xFF;
                int g = (bgColor >> 8) & 0xFF;
                int b = bgColor & 0xFF;
                finalBg = Color.argb(180, Math.min(255, 16 + r / 6), Math.min(255, 20 + g / 6), Math.min(255, 32 + b / 6));
            }
            finalStroke = Color.argb(90, 255, 255, 255);
        } else {
            if (bgColor == Color.WHITE || bgColor == 0xFFFFFFFF) {
                finalBg = Color.argb(218, 255, 255, 255);
            } else {
                int r = (bgColor >> 16) & 0xFF;
                int g = (bgColor >> 8) & 0xFF;
                int b = bgColor & 0xFF;
                finalBg = Color.argb(220, r, g, b);
            }
            finalStroke = Color.argb(70, 0, 0, 0);
        }
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(finalBg);
        gd.setCornerRadius(radius);
        gd.setStroke(dp(1), finalStroke);
        return gd;
    }

    // ==========================================
    // 3. CARD 0: DIGITAL WELLBEING CONTENT
    // ==========================================
    private View createWellbeingContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // Permission Notice
        permissionBanner = createCardLayout(Color.parseColor("#FFF7ED"), Color.parseColor("#FED7AA"));
        permissionBanner.setPadding(dp(12), dp(10), dp(12), dp(10));
        permissionBanner.setOrientation(LinearLayout.VERTICAL);

        TextView permTitle = new TextView(this);
        permTitle.setText("⚠️ Live App Usage Access Required");
        permTitle.setTextSize(11);
        permTitle.setTypeface(Typeface.DEFAULT_BOLD);
        permTitle.setTextColor(Color.parseColor("#C2410C"));
        permissionBanner.addView(permTitle);

        TextView permDesc = new TextView(this);
        permDesc.setText("Grant Usage Access in Android Settings to read real-time screen telemetry.");
        permDesc.setTextSize(10);
        permDesc.setTextColor(Color.parseColor("#9A3412"));
        permDesc.setPadding(0, dp(2), 0, dp(6));
        permissionBanner.addView(permDesc);

        Button permBtn = createStyledButton("Grant Usage Permission", Color.parseColor("#EA580C"));
        permBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open usage settings", Toast.LENGTH_SHORT).show();
            }
        });
        permissionBanner.addView(permBtn);
        content.addView(permissionBanner);

        // 1. Hero Today Wellbeing Telemetry Card (Multi-Metric Daily Aggregation)
        LinearLayout todayCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        todayCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        todayCard.setOrientation(LinearLayout.VERTICAL);

        TextView todayHeader = new TextView(this);
        todayHeader.setText("TODAY'S WELLBEING TELEMETRY");
        todayHeader.setTextSize(10);
        todayHeader.setTypeface(Typeface.DEFAULT_BOLD);
        todayHeader.setTextColor(Color.parseColor("#0284C7"));
        todayCard.addView(todayHeader);

        TextView todaySub = new TextView(this);
        todaySub.setText("Daily aggregation: Focus, completed tasks & app session engagement");
        todaySub.setTextSize(10);
        todaySub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        todaySub.setPadding(0, dp(1), 0, dp(10));
        todayCard.addView(todaySub);

        // 2x2 Metric Tiles Grid
        LinearLayout gridRow1 = new LinearLayout(this);
        gridRow1.setOrientation(LinearLayout.HORIZONTAL);
        gridRow1.setPadding(0, 0, 0, dp(8));

        LinearLayout tileFocus = createMetricTile("⏱️ FOCUS TIME", "#0284C7");
        wbFocusTimeView = (TextView) tileFocus.getChildAt(1);
        LinearLayout.LayoutParams tflp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tflp.setMargins(0, 0, dp(4), 0);
        tileFocus.setLayoutParams(tflp);
        gridRow1.addView(tileFocus);

        LinearLayout tileSessions = createMetricTile("🍅 SESSIONS", "#E11D48");
        wbSessionsView = (TextView) tileSessions.getChildAt(1);
        LinearLayout.LayoutParams tslp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tslp.setMargins(dp(4), 0, 0, 0);
        tileSessions.setLayoutParams(tslp);
        gridRow1.addView(tileSessions);
        todayCard.addView(gridRow1);

        LinearLayout gridRow2 = new LinearLayout(this);
        gridRow2.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout tileTasks = createMetricTile("✅ TASKS DONE", "#10B981");
        wbTasksDoneView = (TextView) tileTasks.getChildAt(1);
        LinearLayout.LayoutParams ttlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        ttlp.setMargins(0, 0, dp(4), 0);
        tileTasks.setLayoutParams(ttlp);
        gridRow2.addView(tileTasks);

        LinearLayout tileActive = createMetricTile("📱 IN-APP ACTIVE", "#8B5CF6");
        wbAppActiveView = (TextView) tileActive.getChildAt(1);
        LinearLayout.LayoutParams talp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        talp.setMargins(dp(4), 0, 0, 0);
        tileActive.setLayoutParams(talp);
        gridRow2.addView(tileActive);
        todayCard.addView(gridRow2);

        content.addView(todayCard);

        // 2. 7-Day Activity & Focus Trends Card (Rolling History)
        LinearLayout trendsCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        trendsCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        trendsCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tclp.setMargins(0, dp(12), 0, dp(12));
        trendsCard.setLayoutParams(tclp);

        TextView trendsHeader = new TextView(this);
        trendsHeader.setText("7-DAY ACTIVITY & FOCUS TRENDS");
        trendsHeader.setTextSize(10);
        trendsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        trendsHeader.setTextColor(Color.parseColor("#0284C7"));
        trendsCard.addView(trendsHeader);

        TextView trendsSub = new TextView(this);
        trendsSub.setText("Daily rolling history & rollover (30-day retention engine)");
        trendsSub.setTextSize(10);
        trendsSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        trendsSub.setPadding(0, dp(1), 0, dp(10));
        trendsCard.addView(trendsSub);

        sevenDayTrendsContainer = new LinearLayout(this);
        sevenDayTrendsContainer.setOrientation(LinearLayout.VERTICAL);
        trendsCard.addView(sevenDayTrendsContainer);

        content.addView(trendsCard);

        // Hero Screen Time Metric (Aesthetic Sky Blue Card)
        LinearLayout heroCard = createCardLayout(Color.parseColor("#F0F9FF"), Color.parseColor("#BAE6FD"));
        heroCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        heroCard.setOrientation(LinearLayout.VERTICAL);

        TextView heroLabel = new TextView(this);
        heroLabel.setText("SYSTEM DEVICE SCREEN TIME");
        heroLabel.setTextSize(10);
        heroLabel.setTypeface(Typeface.DEFAULT_BOLD);
        heroLabel.setTextColor(Color.parseColor("#0284C7"));
        heroCard.addView(heroLabel);

        screenTimeTotalView = new TextView(this);
        screenTimeTotalView.setText("3h 48m");
        screenTimeTotalView.setTextSize(30);
        screenTimeTotalView.setTypeface(Typeface.DEFAULT_BOLD);
        screenTimeTotalView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        screenTimeTotalView.setPadding(0, dp(2), 0, dp(2));
        heroCard.addView(screenTimeTotalView);

        focusScoreView = new TextView(this);
        focusScoreView.setText("⚡ Dopamine Score: 84/100 · High Discipline");
        focusScoreView.setTextSize(11);
        focusScoreView.setTextColor(Color.parseColor("#059669"));
        heroCard.addView(focusScoreView);

        content.addView(heroCard);

        // Category Breakdown Bar
        TextView catHeader = new TextView(this);
        catHeader.setText("TIME DISTRIBUTION BY CATEGORY");
        catHeader.setTextSize(10);
        catHeader.setTypeface(Typeface.DEFAULT_BOLD);
        catHeader.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        catHeader.setPadding(dp(2), dp(14), 0, dp(6));
        content.addView(catHeader);

        categoryBarContainer = new LinearLayout(this);
        categoryBarContainer.setOrientation(LinearLayout.HORIZONTAL);
        categoryBarContainer.setPadding(0, 0, 0, dp(10));
        content.addView(categoryBarContainer);

        // App Telemetry List
        TextView appsHeader = new TextView(this);
        appsHeader.setText("APPLICATION USAGE TELEMETRY");
        appsHeader.setTextSize(10);
        appsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        appsHeader.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        appsHeader.setPadding(dp(2), dp(6), 0, dp(8));
        content.addView(appsHeader);

        wellbeingListContainer = new LinearLayout(this);
        wellbeingListContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(wellbeingListContainer);

        return content;
    }

    private LinearLayout createMetricTile(String label, String colorHex) {
        LinearLayout tile = createCardLayout(Color.parseColor("#F8FAFC"), Color.parseColor("#E2E8F0"));
        tile.setPadding(dp(10), dp(8), dp(10), dp(8));
        tile.setOrientation(LinearLayout.VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(9);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setTextColor(Color.parseColor(colorHex));
        tile.addView(lbl);

        TextView val = new TextView(this);
        val.setText("0");
        val.setTextSize(18);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        val.setPadding(0, dp(2), 0, 0);
        tile.addView(val);

        return tile;
    }

    private void refreshWellbeingData() {
        // 1. Refresh In-App Telemetry & Trends
        DailyWellbeingRecord todayRec = WellbeingHistoryManager.getTodayRecord(this);
        if (wbFocusTimeView != null) {
            wbFocusTimeView.setText(todayRec.totalFocusMinutes + "m");
        }
        if (wbSessionsView != null) {
            wbSessionsView.setText(todayRec.completedPomodoros + " done");
        }
        if (wbTasksDoneView != null) {
            wbTasksDoneView.setText(todayRec.tasksCompleted + " tasks");
        }
        if (wbAppActiveView != null) {
            wbAppActiveView.setText(todayRec.activeAppMinutes + "m");
        }

        if (sevenDayTrendsContainer != null) {
            sevenDayTrendsContainer.removeAllViews();
            List<DailyWellbeingRecord> pastDays = WellbeingHistoryManager.getPastDays(this, 7);
            int maxFocus = 60;
            for (DailyWellbeingRecord d : pastDays) {
                if (d.totalFocusMinutes > maxFocus) maxFocus = d.totalFocusMinutes;
            }

            String todayStr = WellbeingHistoryManager.getTodayDateString();
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outFormat = new SimpleDateFormat("EEE dd", Locale.US);

            for (DailyWellbeingRecord d : pastDays) {
                LinearLayout dayRow = new LinearLayout(this);
                dayRow.setOrientation(LinearLayout.HORIZONTAL);
                dayRow.setGravity(Gravity.CENTER_VERTICAL);
                dayRow.setPadding(0, dp(4), 0, dp(4));

                boolean isToday = todayStr.equals(d.date);
                String displayDate;
                try {
                    Date parsed = inFormat.parse(d.date);
                    displayDate = isToday ? "Today" : outFormat.format(parsed);
                } catch (Exception e) {
                    displayDate = d.date;
                }

                TextView dayLabel = new TextView(this);
                dayLabel.setText(displayDate);
                dayLabel.setTextSize(11);
                dayLabel.setTypeface(Typeface.DEFAULT_BOLD);
                dayLabel.setTextColor(Color.parseColor(isToday ? "#E11D48" : (isDarkTheme ? "#CBD5E1" : "#475569")));
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT);
                dayLabel.setLayoutParams(dlp);
                dayRow.addView(dayLabel);

                ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                bar.setMax(maxFocus);
                bar.setProgress(d.totalFocusMinutes);
                bar.setProgressTintList(ColorStateList.valueOf(Color.parseColor(isToday ? "#E11D48" : "#38BDF8")));
                bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, dp(6), 1.0f);
                blp.setMargins(dp(6), 0, dp(8), 0);
                bar.setLayoutParams(blp);
                dayRow.addView(bar);

                TextView statsLabel = new TextView(this);
                statsLabel.setText(d.totalFocusMinutes + "m · ✅ " + d.tasksCompleted + " · 🍅 " + d.completedPomodoros);
                statsLabel.setTextSize(10);
                statsLabel.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
                statsLabel.setGravity(Gravity.END);
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                statsLabel.setLayoutParams(slp);
                dayRow.addView(statsLabel);

                sevenDayTrendsContainer.addView(dayRow);
            }
        }

        // 2. Refresh Device App Usage
        boolean hasPerm = WellbeingManager.hasUsageStatsPermission(this);
        if (permissionBanner != null) {
            permissionBanner.setVisibility(hasPerm ? View.GONE : View.VISIBLE);
        }

        List<WellbeingManager.AppUsageInfo> apps = WellbeingManager.getTodayAppUsage(this);
        long totalMillis = WellbeingManager.getTotalScreenTime(apps);
        if (screenTimeTotalView != null) {
            screenTimeTotalView.setText(WellbeingManager.formatDuration(totalMillis));
        }

        if (wellbeingListContainer == null) return;
        wellbeingListContainer.removeAllViews();

        long maxTime = apps.isEmpty() ? 1 : apps.get(0).totalTimeMillis;

        // Render Category Breakdown Bars
        if (categoryBarContainer != null) {
            categoryBarContainer.removeAllViews();
            Map<String, Long> catSums = new HashMap<>();
            for (WellbeingManager.AppUsageInfo a : apps) {
                catSums.put(a.category, catSums.getOrDefault(a.category, 0L) + a.totalTimeMillis);
            }

            for (Map.Entry<String, Long> entry : catSums.entrySet()) {
                float weight = (float) entry.getValue() / (float) (totalMillis > 0 ? totalMillis : 1);
                View barSlice = new View(this);
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, dp(6), Math.max(0.05f, weight));
                slp.setMargins(dp(1), 0, dp(1), 0);
                barSlice.setLayoutParams(slp);
                barSlice.setBackgroundColor(WellbeingManager.getCategoryColor(entry.getKey()));
                categoryBarContainer.addView(barSlice);
            }
        }

        // Render Individual App Cards (Clean White Cards with subtle borders)
        for (WellbeingManager.AppUsageInfo app : apps) {
            LinearLayout item = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ilp.setMargins(0, 0, 0, dp(8));
            item.setLayoutParams(ilp);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            if (app.icon != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(app.icon);
                LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(26), dp(26));
                ivLp.setMargins(0, 0, dp(10), 0);
                iv.setLayoutParams(ivLp);
                topRow.addView(iv);
            } else {
                TextView dot = new TextView(this);
                dot.setText("●");
                dot.setTextSize(14);
                dot.setTextColor(app.categoryColor);
                dot.setPadding(0, 0, dp(8), 0);
                topRow.addView(dot);
            }

            TextView appNameView = new TextView(this);
            appNameView.setText(app.appName);
            appNameView.setTextSize(13);
            appNameView.setTypeface(Typeface.DEFAULT_BOLD);
            appNameView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            appNameView.setLayoutParams(nameLp);
            topRow.addView(appNameView);

            TextView timeView = new TextView(this);
            timeView.setText(WellbeingManager.formatDuration(app.totalTimeMillis));
            timeView.setTextSize(12);
            timeView.setTypeface(Typeface.DEFAULT_BOLD);
            timeView.setTextColor(Color.parseColor("#0284C7"));
            topRow.addView(timeView);

            item.addView(topRow);

            LinearLayout barRow = new LinearLayout(this);
            barRow.setOrientation(LinearLayout.HORIZONTAL);
            barRow.setGravity(Gravity.CENTER_VERTICAL);
            barRow.setPadding(0, dp(6), 0, 0);

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            int percent = (int) ((app.totalTimeMillis * 100) / (maxTime > 0 ? maxTime : 1));
            pb.setProgress(percent);
            pb.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#38BDF8")));
            pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(0, dp(4), 1.0f);
            pbLp.setMargins(0, 0, dp(8), 0);
            pb.setLayoutParams(pbLp);
            barRow.addView(pb);

            TextView catView = new TextView(this);
            catView.setText(app.category);
            catView.setTextSize(9);
            catView.setTypeface(Typeface.DEFAULT_BOLD);
            catView.setTextColor(app.categoryColor);
            barRow.addView(catView);

            item.addView(barRow);
            wellbeingListContainer.addView(item);
        }
    }

    // ==========================================
    // 4. CARD 1: CORE CADENCE CONTENT
    // ==========================================
    private View createCadenceContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // 0. Circadian Schedule Timings Config Card (Custom Wake-up & Sleep Times)
        LinearLayout scheduleConfigCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        scheduleConfigCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        scheduleConfigCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sccLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sccLp.setMargins(0, 0, 0, dp(12));
        scheduleConfigCard.setLayoutParams(sccLp);

        LinearLayout scHeaderRow = new LinearLayout(this);
        scHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        scHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView scTitle = new TextView(this);
        scTitle.setText("⏰ CIRCADIAN SCHEDULE TIMINGS");
        scTitle.setTextSize(11);
        scTitle.setTypeface(Typeface.DEFAULT_BOLD);
        scTitle.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#475569"));
        LinearLayout.LayoutParams sctLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        scTitle.setLayoutParams(sctLp);
        scHeaderRow.addView(scTitle);

        TextView scHint = new TextView(this);
        scHint.setText("Tap pill to edit ⚙️");
        scHint.setTextSize(10);
        scHint.setTextColor(Color.parseColor("#0284C7"));
        scHeaderRow.addView(scHint);

        scheduleConfigCard.addView(scHeaderRow);

        // 2 Interactive Time Pills Row
        LinearLayout pillsRow = new LinearLayout(this);
        pillsRow.setOrientation(LinearLayout.HORIZONTAL);
        pillsRow.setPadding(0, dp(8), 0, dp(6));

        // Wake-up Pill (Sky Blue Tint)
        LinearLayout wakePill = new LinearLayout(this);
        wakePill.setOrientation(LinearLayout.VERTICAL);
        wakePill.setGravity(Gravity.CENTER);
        wakePill.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable wbg = new GradientDrawable();
        wbg.setColor(isDarkTheme ? 0xFF132838 : Color.parseColor("#F0F9FF"));
        wbg.setCornerRadius(dp(12));
        wbg.setStroke(dp(1), isDarkTheme ? 0xFF1B4965 : Color.parseColor("#BAE6FD"));
        wakePill.setBackground(wbg);
        TouchFeedback.attach(wakePill, v -> showTimePicker(true), 0.96f);
        LinearLayout.LayoutParams wpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        wpLp.setMargins(0, 0, dp(6), 0);
        wakePill.setLayoutParams(wpLp);

        TextView wakeLabel = new TextView(this);
        wakeLabel.setText("☀️ WAKE UP");
        wakeLabel.setTextSize(10);
        wakeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        wakeLabel.setTextColor(Color.parseColor("#0284C7"));
        wakePill.addView(wakeLabel);

        wakeTimeView = new TextView(this);
        wakeTimeView.setText("08:00 AM");
        wakeTimeView.setTextSize(15);
        wakeTimeView.setTypeface(Typeface.DEFAULT_BOLD);
        wakeTimeView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        wakePill.addView(wakeTimeView);

        pillsRow.addView(wakePill);

        // Sleep Pill (Purple Tint)
        LinearLayout sleepPill = new LinearLayout(this);
        sleepPill.setOrientation(LinearLayout.VERTICAL);
        sleepPill.setGravity(Gravity.CENTER);
        sleepPill.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(isDarkTheme ? 0xFF281E38 : Color.parseColor("#FAF5FF"));
        sbg.setCornerRadius(dp(12));
        sbg.setStroke(dp(1), isDarkTheme ? 0xFF3D2B56 : Color.parseColor("#E9D5FF"));
        sleepPill.setBackground(sbg);
        TouchFeedback.attach(sleepPill, v -> showTimePicker(false), 0.96f);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        spLp.setMargins(dp(6), 0, 0, 0);
        sleepPill.setLayoutParams(spLp);

        TextView sleepLabel = new TextView(this);
        sleepLabel.setText("🌙 SLEEP TIME");
        sleepLabel.setTextSize(10);
        sleepLabel.setTypeface(Typeface.DEFAULT_BOLD);
        sleepLabel.setTextColor(Color.parseColor("#7E22CE"));
        sleepPill.addView(sleepLabel);

        sleepTimeView = new TextView(this);
        sleepTimeView.setText("12:00 AM");
        sleepTimeView.setTextSize(15);
        sleepTimeView.setTypeface(Typeface.DEFAULT_BOLD);
        sleepTimeView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        sleepPill.addView(sleepTimeView);

        pillsRow.addView(sleepPill);
        scheduleConfigCard.addView(pillsRow);

        scheduleSummaryView = new TextView(this);
        scheduleSummaryView.setTextSize(10);
        scheduleSummaryView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        scheduleSummaryView.setPadding(dp(2), dp(2), dp(2), 0);
        scheduleConfigCard.addView(scheduleSummaryView);

        content.addView(scheduleConfigCard);
        updateScheduleTimingsUI();

        // Active Routine Card (Aesthetic Rose Red Card)
        LinearLayout activeCard = createCardLayout(Color.parseColor("#FFF1F2"), Color.parseColor("#FDA4AF"));
        activeCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView activeBadge = new TextView(this);
        activeBadge.setText("⏰ CURRENT ROUTINE");
        activeBadge.setTextSize(10);
        activeBadge.setTypeface(Typeface.DEFAULT_BOLD);
        activeBadge.setTextColor(Color.parseColor("#E11D48"));
        activeCard.addView(activeBadge);

        currentRomanView = new TextView(this);
        currentRomanView.setText("CALCULATING...");
        currentRomanView.setTextSize(13);
        currentRomanView.setTypeface(Typeface.DEFAULT_BOLD);
        currentRomanView.setTextColor(Color.parseColor("#E11D48"));
        currentRomanView.setPadding(0, dp(4), 0, dp(2));
        activeCard.addView(currentRomanView);

        currentTitleView = new TextView(this);
        currentTitleView.setText("Updating routine...");
        currentTitleView.setTextSize(17);
        currentTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        currentTitleView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        currentTitleView.setPadding(0, 0, 0, dp(2));
        activeCard.addView(currentTitleView);

        currentCountdownView = new TextView(this);
        currentCountdownView.setText("Next boundary in --:--");
        currentCountdownView.setTextSize(11);
        currentCountdownView.setTextColor(Color.parseColor("#0284C7"));
        currentCountdownView.setPadding(0, 0, 0, dp(6));
        activeCard.addView(currentCountdownView);

        currentDirectiveView = new TextView(this);
        currentDirectiveView.setText("What to do: Loading protocol...");
        currentDirectiveView.setTextSize(11);
        currentDirectiveView.setTextColor(isDarkTheme ? 0xFFE2E8F0 : Color.parseColor("#334155"));
        currentDirectiveView.setPadding(0, 0, 0, dp(3));
        activeCard.addView(currentDirectiveView);

        currentWhyView = new TextView(this);
        currentWhyView.setText("Why: Loading benefits...");
        currentWhyView.setTextSize(10);
        currentWhyView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        activeCard.addView(currentWhyView);

        content.addView(activeCard);

        // Action Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(12), 0, dp(12));

        Button testBtn = createIosGlossyButton(
            "🔔 Test",
            Color.parseColor("#38BDF8"),
            Color.parseColor("#0EA5E9"),
            Color.parseColor("#0284C7"),
            Color.parseColor("#7DD3FC")
        );
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        p1.setMargins(0, 0, dp(4), 0);
        testBtn.setLayoutParams(p1);
        testBtn.setOnClickListener(v -> triggerTestAlarm());
        btnRow.addView(testBtn);

        Button stopBtn = createIosGlossyButton(
            "⏹ Stop",
            Color.parseColor("#FB7185"),
            Color.parseColor("#F43F5E"),
            Color.parseColor("#E11D48"),
            Color.parseColor("#FDA4AF")
        );
        LinearLayout.LayoutParams pStop = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pStop.setMargins(dp(4), 0, 0, 0);
        stopBtn.setLayoutParams(pStop);
        stopBtn.setOnClickListener(v -> {
            AlarmAudioService.stopAlarm(this);
            Toast.makeText(this, "Song stopped", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(stopBtn);

        /* Disabled for now per user request - code preserved, not purged
        Button blueprintBtn = createIosGlossyButton(
            "📖 Blueprint",
            Color.parseColor("#475569"),
            Color.parseColor("#334155"),
            Color.parseColor("#0F172A"),
            Color.parseColor("#94A3B8")
        );
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        p2.setMargins(dp(4), 0, 0, 0);
        blueprintBtn.setLayoutParams(p2);
        blueprintBtn.setOnClickListener(v -> {
            openFeature(FeatureRegistry.ID_BLUEPRINT);
        });
        btnRow.addView(blueprintBtn);
        */

        content.addView(btnRow);

        // Routine Completion Card
        LinearLayout completionCard = createCardLayout(isDarkTheme ? 0xFF1E293B : Color.parseColor("#F8FAFC"), isDarkTheme ? 0xFF334155 : Color.parseColor("#E2E8F0"));
        completionCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        completionCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccLp.setMargins(0, 0, 0, dp(12));
        completionCard.setLayoutParams(ccLp);

        LinearLayout ccHeaderRow = new LinearLayout(this);
        ccHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        ccHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView ccTitle = new TextView(this);
        ccTitle.setText("✅ DAILY ROUTINE CHECKLIST & PROGRESS");
        ccTitle.setTextSize(11);
        ccTitle.setTypeface(Typeface.DEFAULT_BOLD);
        ccTitle.setTextColor(Color.parseColor("#10B981"));
        LinearLayout.LayoutParams cctLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        ccTitle.setLayoutParams(cctLp);
        ccHeaderRow.addView(ccTitle);

        routineProgressStatsView = new TextView(this);
        routineProgressStatsView.setText("0 / 0 completed (0%)");
        routineProgressStatsView.setTextSize(11);
        routineProgressStatsView.setTypeface(Typeface.DEFAULT_BOLD);
        routineProgressStatsView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#475569"));
        ccHeaderRow.addView(routineProgressStatsView);
        completionCard.addView(ccHeaderRow);

        routineProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        routineProgressBar.setMax(100);
        routineProgressBar.setProgress(0);
        routineProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        routineProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(isDarkTheme ? 0xFF333A48 : Color.parseColor("#E2E8F0")));
        LinearLayout.LayoutParams rpbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        rpbLp.setMargins(0, dp(8), 0, dp(10));
        routineProgressBar.setLayoutParams(rpbLp);
        completionCard.addView(routineProgressBar);

        // Action Buttons Row
        LinearLayout ccBtnRow = new LinearLayout(this);
        ccBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button resetRoutineBtn = createOutlinedButton("🔄 Reset Today", Color.parseColor("#EF4444"), Color.parseColor("#FECDD3"));
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        rrLp.setMargins(0, 0, dp(4), 0);
        resetRoutineBtn.setLayoutParams(rrLp);
        resetRoutineBtn.setTextSize(10);
        resetRoutineBtn.setPadding(dp(4), dp(6), dp(4), dp(6));
        resetRoutineBtn.setOnClickListener(v -> {
            RoutineCompletionManager.resetToday(this);
            Toast.makeText(this, "Daily routine tickmarks reset for today", Toast.LENGTH_SHORT).show();
            renderCadenceList();
        });
        ccBtnRow.addView(resetRoutineBtn);

        Button importRoutineBtn = createOutlinedButton("📥 Copy to Tasks", Color.parseColor("#0284C7"), Color.parseColor("#BAE6FD"));
        LinearLayout.LayoutParams irLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        irLp.setMargins(dp(4), 0, 0, 0);
        importRoutineBtn.setLayoutParams(irLp);
        importRoutineBtn.setTextSize(10);
        importRoutineBtn.setPadding(dp(4), dp(6), dp(4), dp(6));
        importRoutineBtn.setOnClickListener(v -> {
            int added = RoutineCompletionManager.importBlocksToChecklist(this, ScheduleManager.getAllGranularBlocks());
            Toast.makeText(this, "Copied " + added + " routine items to Checklist card", Toast.LENGTH_SHORT).show();
            refreshChecklistUI();
            refreshWellbeingData();
        });
        ccBtnRow.addView(importRoutineBtn);

        completionCard.addView(ccBtnRow);
        content.addView(completionCard);

        // Schedule Header
        TextView matrixHeader = new TextView(this);
        matrixHeader.setText("DAILY SCHEDULE & GRANULAR BLOCKS");
        matrixHeader.setTextSize(10);
        matrixHeader.setTypeface(Typeface.DEFAULT_BOLD);
        matrixHeader.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        matrixHeader.setPadding(dp(2), dp(4), 0, dp(6));
        content.addView(matrixHeader);

        phaseListContainer = new LinearLayout(this);
        phaseListContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(phaseListContainer);

        renderCadenceList();
        return content;
    }

    private void renderCadenceList() {
        if (phaseListContainer == null) return;
        phaseListContainer.removeAllViews();

        for (ScheduleManager.Phase p : ScheduleManager.PHASES) {
            LinearLayout item = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            item.setLayoutParams(lp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleCol.setLayoutParams(tcLp);

            LinearLayout timeRow = new LinearLayout(this);
            timeRow.setOrientation(LinearLayout.HORIZONTAL);
            timeRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView expChevron = new TextView(this);
            expChevron.setText("▾");
            expChevron.setTextSize(12);
            expChevron.setTypeface(Typeface.DEFAULT_BOLD);
            expChevron.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF64748B);
            expChevron.setPadding(0, 0, dp(6), 0);
            timeRow.addView(expChevron);

            TextView timeText = new TextView(this);
            timeText.setText(p.getFormattedTime12());
            timeText.setTextSize(12);
            timeText.setTypeface(Typeface.DEFAULT_BOLD);
            timeText.setTextColor(Color.parseColor("#E11D48"));
            timeRow.addView(timeText);

            titleCol.addView(timeRow);

            TextView titleText = new TextView(this);
            titleText.setText(p.getDisplayTitle());
            titleText.setTextSize(13);
            titleText.setTypeface(Typeface.DEFAULT_BOLD);
            titleText.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
            titleText.setPadding(0, dp(2), 0, 0);
            titleCol.addView(titleText);

            headerRow.addView(titleCol);

            // Completion badge pill
            int totalBlocksInPhase = (p.granularBlocks != null) ? p.granularBlocks.size() : 0;
            int completedInPhase = 0;
            if (p.granularBlocks != null) {
                for (RoutineBlock b : p.granularBlocks) {
                    if (RoutineCompletionManager.isBlockCompleted(this, b.id)) {
                        completedInPhase++;
                    }
                }
            }
            boolean isPhaseComplete = totalBlocksInPhase > 0 && completedInPhase == totalBlocksInPhase;

            TextView phasePill = new TextView(this);
            phasePill.setText(isPhaseComplete ? (completedInPhase + "/" + totalBlocksInPhase + " ✓") : (completedInPhase + "/" + totalBlocksInPhase));
            phasePill.setTextSize(11);
            phasePill.setTypeface(Typeface.DEFAULT_BOLD);
            phasePill.setTextColor(isPhaseComplete ? Color.WHITE : (isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#475569")));
            phasePill.setPadding(dp(8), dp(4), dp(8), dp(4));
            GradientDrawable pbg = new GradientDrawable();
            pbg.setCornerRadius(dp(12));
            pbg.setColor(isPhaseComplete ? Color.parseColor("#10B981") : (isDarkTheme ? 0xFF334155 : Color.parseColor("#E2E8F0")));
            phasePill.setBackground(pbg);
            headerRow.addView(phasePill);

            item.addView(headerRow);

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(0, dp(8), 0, 0);
            details.setVisibility(View.VISIBLE);

            TextView dir = new TextView(this);
            dir.setText("Protocol: " + p.getDisplayProtocol());
            dir.setTextSize(11);
            dir.setTextColor(Color.parseColor("#0284C7"));
            dir.setPadding(0, 0, 0, dp(3));
            details.addView(dir);

            TextView why = new TextView(this);
            why.setText("Why: " + p.biologicalWhy);
            why.setTextSize(10);
            why.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            why.setPadding(0, 0, 0, dp(8));
            details.addView(why);

            // Granular ingredient and action blocks
            if (p.granularBlocks != null && !p.granularBlocks.isEmpty()) {
                TextView blocksHeader = new TextView(this);
                blocksHeader.setText("TICKABLE INGREDIENTS & DIRECTIVES (" + p.granularBlocks.size() + "):");
                blocksHeader.setTextSize(9);
                blocksHeader.setTypeface(Typeface.DEFAULT_BOLD);
                blocksHeader.setTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#64748B"));
                blocksHeader.setPadding(0, dp(2), 0, dp(6));
                details.addView(blocksHeader);

                for (RoutineBlock b : p.granularBlocks) {
                    details.addView(createGranularBlockRow(b, p));
                }
            }

            item.addView(details);

            if (item instanceof PremiumCard) {
                ((PremiumCard) item).setInteractive(false);
            }

            headerRow.setOnClickListener(v -> {
                boolean visible = details.getVisibility() == View.VISIBLE;
                details.setVisibility(visible ? View.GONE : View.VISIBLE);
                expChevron.setText(visible ? "▸" : "▾");
            });

            phaseListContainer.addView(item);
        }

        updateRoutineProgressUI();
    }

    private View createGranularBlockRow(RoutineBlock b, ScheduleManager.Phase p) {
        boolean isDone = RoutineCompletionManager.isBlockCompleted(this, b.id);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(rlp);

        GradientDrawable rbg = new GradientDrawable();
        rbg.setCornerRadius(dp(8));
        if (isDone) {
            rbg.setColor(isDarkTheme ? 0xFF143026 : Color.parseColor("#ECFDF5"));
            rbg.setStroke(dp(1), isDarkTheme ? 0xFF059669 : Color.parseColor("#A7F3D0"));
        } else {
            rbg.setColor(isDarkTheme ? 0xFF1E293B : Color.parseColor("#F8FAFC"));
            rbg.setStroke(dp(1), isDarkTheme ? 0xFF334155 : Color.parseColor("#E2E8F0"));
        }
        row.setBackground(rbg);

        // Checkbox Button
        TextView checkBtn = new TextView(this);
        checkBtn.setText(isDone ? "✓" : "○");
        checkBtn.setTextSize(14);
        checkBtn.setTypeface(Typeface.DEFAULT_BOLD);
        checkBtn.setGravity(Gravity.CENTER);
        checkBtn.setTextColor(isDone ? Color.WHITE : (isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#64748B")));
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        cbLp.setMargins(0, 0, dp(10), 0);
        checkBtn.setLayoutParams(cbLp);

        GradientDrawable cbg = new GradientDrawable();
        cbg.setCornerRadius(dp(6));
        cbg.setColor(isDone ? Color.parseColor("#10B981") : (isDarkTheme ? 0xFF0F172A : Color.WHITE));
        cbg.setStroke(dp(1), isDone ? Color.parseColor("#059669") : (isDarkTheme ? 0xFF475569 : Color.parseColor("#CBD5E1")));
        checkBtn.setBackground(cbg);
        row.addView(checkBtn);

        // Text Column
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(tclp);

        TextView titleTv = new TextView(this);
        titleTv.setText(b.title);
        titleTv.setTextSize(12);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        if (isDone) {
            titleTv.setTextColor(Color.parseColor("#10B981"));
            titleTv.setPaintFlags(titleTv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            titleTv.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
            titleTv.setPaintFlags(titleTv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        textCol.addView(titleTv);

        if (b.detail != null && !b.detail.isEmpty()) {
            TextView detailTv = new TextView(this);
            detailTv.setText(b.detail);
            detailTv.setTextSize(10);
            detailTv.setTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#64748B"));
            detailTv.setPadding(0, dp(2), 0, 0);
            textCol.addView(detailTv);
        }
        row.addView(textCol);

        // Time hint
        if (b.timeHint != null && !b.timeHint.isEmpty()) {
            TextView timeTv = new TextView(this);
            timeTv.setText(b.timeHint);
            timeTv.setTextSize(9);
            timeTv.setTextColor(isDone ? Color.parseColor("#059669") : Color.parseColor("#0284C7"));
            timeTv.setPadding(dp(6), 0, 0, 0);
            row.addView(timeTv);
        }

        View.OnClickListener toggleListener = v -> {
            RoutineCompletionManager.toggleBlock(this, b.id);
            renderCadenceList();
            refreshWellbeingData();
        };

        row.setOnClickListener(toggleListener);
        checkBtn.setOnClickListener(toggleListener);

        return row;
    }

    private void updateRoutineProgressUI() {
        List<RoutineBlock> all = ScheduleManager.getAllGranularBlocks();
        int total = all.size();
        int done = RoutineCompletionManager.getCompletedCount(this, all);
        int pct = total > 0 ? (done * 100 / total) : 0;

        if (routineProgressStatsView != null) {
            routineProgressStatsView.setText(done + " / " + total + " completed (" + pct + "%)");
        }
        if (routineProgressBar != null) {
            routineProgressBar.setProgress(pct);
        }
    }

    // ==========================================
    // 5. SYSTEM & HARDWARE MONITOR CONTENT (CARD 4)
    // ==========================================
    private View createSystemMonitorContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // ==========================================
        // SECTION 1: HARDWARE VITALS
        // ==========================================
        LinearLayout vitalsCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        vitalsCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        vitalsCard.setOrientation(LinearLayout.VERTICAL);

        TextView vitalsHeader = new TextView(this);
        vitalsHeader.setText("HARDWARE VITALS & SENSORS");
        vitalsHeader.setTextSize(10);
        vitalsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        vitalsHeader.setTextColor(Color.parseColor("#6366F1"));
        vitalsCard.addView(vitalsHeader);

        TextView vitalsSub = new TextView(this);
        vitalsSub.setText("Physical telemetry: Live RAM, internal flash storage & battery health");
        vitalsSub.setTextSize(10);
        vitalsSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        vitalsSub.setPadding(0, dp(1), 0, dp(10));
        vitalsCard.addView(vitalsSub);

        // A. RAM Meter Tile
        LinearLayout ramTile = createCardLayout(Color.parseColor("#F8FAFC"), Color.parseColor("#E2E8F0"));
        ramTile.setPadding(dp(12), dp(10), dp(12), dp(10));
        ramTile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout ramTopRow = new LinearLayout(this);
        ramTopRow.setOrientation(LinearLayout.HORIZONTAL);
        ramTopRow.setGravity(Gravity.CENTER_VERTICAL);

        sysRamTitleView = new TextView(this);
        sysRamTitleView.setText("🧠 RAM MEMORY");
        sysRamTitleView.setTextSize(11);
        sysRamTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        sysRamTitleView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#1E293B"));
        LinearLayout.LayoutParams ramTitleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        sysRamTitleView.setLayoutParams(ramTitleLp);
        ramTopRow.addView(sysRamTitleView);

        sysRamSubtitleView = new TextView(this);
        sysRamSubtitleView.setText("0 GB / 0 GB (0%)");
        sysRamSubtitleView.setTextSize(11);
        sysRamSubtitleView.setTypeface(Typeface.DEFAULT_BOLD);
        sysRamSubtitleView.setTextColor(Color.parseColor("#6366F1"));
        ramTopRow.addView(sysRamSubtitleView);
        ramTile.addView(ramTopRow);

        // Progress Track
        android.widget.FrameLayout ramTrack = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams ramTrackLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        ramTrackLp.setMargins(0, dp(8), 0, dp(6));
        ramTrack.setLayoutParams(ramTrackLp);
        ramTrack.setBackground(createPillDrawable(isDarkTheme ? 0xFF333A48 : Color.parseColor("#E2E8F0"), dp(4)));

        sysRamProgressBar = new View(this);
        android.widget.FrameLayout.LayoutParams rpbLp = new android.widget.FrameLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.MATCH_PARENT);
        sysRamProgressBar.setLayoutParams(rpbLp);
        sysRamProgressBar.setBackground(createPillDrawable(Color.parseColor("#10B981"), dp(4)));
        ramTrack.addView(sysRamProgressBar);
        ramTile.addView(ramTrack);

        sysRamSubDetailsView = new TextView(this);
        sysRamSubDetailsView.setText("Free: -- | Total: --");
        sysRamSubDetailsView.setTextSize(9);
        sysRamSubDetailsView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        ramTile.addView(sysRamSubDetailsView);
        vitalsCard.addView(ramTile);

        // B. Storage Meter Tile
        LinearLayout storageTile = createCardLayout(Color.parseColor("#F8FAFC"), Color.parseColor("#E2E8F0"));
        storageTile.setPadding(dp(12), dp(10), dp(12), dp(10));
        storageTile.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.setMargins(0, dp(8), 0, 0);
        storageTile.setLayoutParams(stLp);

        LinearLayout storageTopRow = new LinearLayout(this);
        storageTopRow.setOrientation(LinearLayout.HORIZONTAL);
        storageTopRow.setGravity(Gravity.CENTER_VERTICAL);

        sysStorageTitleView = new TextView(this);
        sysStorageTitleView.setText("💾 INTERNAL FLASH STORAGE");
        sysStorageTitleView.setTextSize(11);
        sysStorageTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        sysStorageTitleView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#1E293B"));
        LinearLayout.LayoutParams stTitleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        sysStorageTitleView.setLayoutParams(stTitleLp);
        storageTopRow.addView(sysStorageTitleView);

        sysStorageSubtitleView = new TextView(this);
        sysStorageSubtitleView.setText("0 GB / 0 GB (0%)");
        sysStorageSubtitleView.setTextSize(11);
        sysStorageSubtitleView.setTypeface(Typeface.DEFAULT_BOLD);
        sysStorageSubtitleView.setTextColor(Color.parseColor("#0284C7"));
        storageTopRow.addView(sysStorageSubtitleView);
        storageTile.addView(storageTopRow);

        // Storage Progress Track
        android.widget.FrameLayout storageTrack = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams strackLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        strackLp.setMargins(0, dp(8), 0, dp(6));
        storageTrack.setLayoutParams(strackLp);
        storageTrack.setBackground(createPillDrawable(isDarkTheme ? 0xFF333A48 : Color.parseColor("#E2E8F0"), dp(4)));

        sysStorageProgressBar = new View(this);
        android.widget.FrameLayout.LayoutParams spbLp = new android.widget.FrameLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.MATCH_PARENT);
        sysStorageProgressBar.setLayoutParams(spbLp);
        sysStorageProgressBar.setBackground(createPillDrawable(Color.parseColor("#0284C7"), dp(4)));
        storageTrack.addView(sysStorageProgressBar);
        storageTile.addView(storageTrack);

        sysStorageSubDetailsView = new TextView(this);
        sysStorageSubDetailsView.setText("Free: -- | Total: --");
        sysStorageSubDetailsView.setTextSize(9);
        sysStorageSubDetailsView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        storageTile.addView(sysStorageSubDetailsView);
        vitalsCard.addView(storageTile);

        // C. Battery & Health 2x2 Grid
        LinearLayout battGrid = new LinearLayout(this);
        battGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bgLp.setMargins(0, dp(10), 0, 0);
        battGrid.setLayoutParams(bgLp);

        LinearLayout battRow1 = new LinearLayout(this);
        battRow1.setOrientation(LinearLayout.HORIZONTAL);
        battRow1.setPadding(0, 0, 0, dp(6));

        // Tile 1: Level & Status
        LinearLayout tLevel = createMetricTile("🔋 LEVEL & STATUS", "#10B981");
        sysBatteryLevelView = (TextView) tLevel.getChildAt(1);
        sysBatteryStatusView = new TextView(this);
        sysBatteryStatusView.setText("Checking...");
        sysBatteryStatusView.setTextSize(10);
        sysBatteryStatusView.setTextColor(Color.parseColor("#10B981"));
        sysBatteryStatusView.setPadding(0, dp(2), 0, 0);
        tLevel.addView(sysBatteryStatusView);
        LinearLayout.LayoutParams tllp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tllp.setMargins(0, 0, dp(4), 0);
        tLevel.setLayoutParams(tllp);
        battRow1.addView(tLevel);

        // Tile 2: Health Rating
        LinearLayout tHealth = createMetricTile("🩺 CELL HEALTH", "#0284C7");
        sysBatteryHealthBadgeView = (TextView) tHealth.getChildAt(1);
        sysBatteryTechView = new TextView(this);
        sysBatteryTechView.setText("Li-ion");
        sysBatteryTechView.setTextSize(10);
        sysBatteryTechView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        sysBatteryTechView.setPadding(0, dp(2), 0, 0);
        tHealth.addView(sysBatteryTechView);
        LinearLayout.LayoutParams thlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        thlp.setMargins(dp(4), 0, 0, 0);
        tHealth.setLayoutParams(thlp);
        battRow1.addView(tHealth);
        battGrid.addView(battRow1);

        LinearLayout battRow2 = new LinearLayout(this);
        battRow2.setOrientation(LinearLayout.HORIZONTAL);

        // Tile 3: Temperature
        LinearLayout tTemp = createMetricTile("🌡️ TEMPERATURE", "#F59E0B");
        sysBatteryTempView = (TextView) tTemp.getChildAt(1);
        TextView tTempSub = new TextView(this);
        tTempSub.setText("Thermal Status");
        tTempSub.setTextSize(10);
        tTempSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        tTempSub.setPadding(0, dp(2), 0, 0);
        tTemp.addView(tTempSub);
        LinearLayout.LayoutParams ttlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        ttlp.setMargins(0, 0, dp(4), 0);
        tTemp.setLayoutParams(ttlp);
        battRow2.addView(tTemp);

        // Tile 4: Voltage
        LinearLayout tVolt = createMetricTile("⚡ VOLTAGE", "#6366F1");
        sysBatteryVoltageView = (TextView) tVolt.getChildAt(1);
        TextView tVoltSub = new TextView(this);
        tVoltSub.setText("Cell Potential");
        tVoltSub.setTextSize(10);
        tVoltSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        tVoltSub.setPadding(0, dp(2), 0, 0);
        tVolt.addView(tVoltSub);
        LinearLayout.LayoutParams tvlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvlp.setMargins(dp(4), 0, 0, 0);
        tVolt.setLayoutParams(tvlp);
        battRow2.addView(tVolt);
        battGrid.addView(battRow2);

        vitalsCard.addView(battGrid);
        content.addView(vitalsCard);

        // ==========================================
        // SECTION 2: ACTIVE NETWORK USAGE
        // ==========================================
        LinearLayout netCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        netCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        netCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ncLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ncLp.setMargins(0, dp(12), 0, 0);
        netCard.setLayoutParams(ncLp);

        LinearLayout netHeaderRow = new LinearLayout(this);
        netHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        netHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView netHeader = new TextView(this);
        netHeader.setText("ACTIVE NETWORK USAGE");
        netHeader.setTextSize(10);
        netHeader.setTypeface(Typeface.DEFAULT_BOLD);
        netHeader.setTextColor(Color.parseColor("#0284C7"));
        LinearLayout.LayoutParams nhLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        netHeader.setLayoutParams(nhLp);
        netHeaderRow.addView(netHeader);

        sysNetworkCountBadge = new TextView(this);
        sysNetworkCountBadge.setText("0 Apps Active");
        sysNetworkCountBadge.setTextSize(9);
        sysNetworkCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        sysNetworkCountBadge.setTextColor(Color.parseColor("#0369A1"));
        sysNetworkCountBadge.setBackground(createPillDrawable(Color.parseColor("#E0F2FE"), dp(10)));
        sysNetworkCountBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        netHeaderRow.addView(sysNetworkCountBadge);
        netCard.addView(netHeaderRow);

        TextView netSub = new TextView(this);
        netSub.setText("Network traffic consumed per application since boot (Download Rx + Upload Tx)");
        netSub.setTextSize(10);
        netSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        netSub.setPadding(0, dp(1), 0, dp(8));
        netCard.addView(netSub);

        sysNetworkSummaryView = new TextView(this);
        sysNetworkSummaryView.setText("Total Traffic: Calculating...");
        sysNetworkSummaryView.setTextSize(10);
        sysNetworkSummaryView.setTypeface(Typeface.DEFAULT_BOLD);
        sysNetworkSummaryView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        sysNetworkSummaryView.setBackground(createPillDrawable(isDarkTheme ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F8FAFC"), dp(8)));
        sysNetworkSummaryView.setPadding(dp(10), dp(6), dp(10), dp(6));
        netCard.addView(sysNetworkSummaryView);

        sysNetworkListContainer = new LinearLayout(this);
        sysNetworkListContainer.setOrientation(LinearLayout.VERTICAL);
        sysNetworkListContainer.setPadding(0, dp(8), 0, 0);
        netCard.addView(sysNetworkListContainer);

        content.addView(netCard);

        // ==========================================
        // SECTION 3: BACKGROUND RUNNING APPS & MEMORY CLEANER
        // ==========================================
        LinearLayout bgCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        bgCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        bgCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bgcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bgcLp.setMargins(0, dp(12), 0, 0);
        bgCard.setLayoutParams(bgcLp);

        LinearLayout bgHeaderRow = new LinearLayout(this);
        bgHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        bgHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView bgHeader = new TextView(this);
        bgHeader.setText("BACKGROUND RUNNING APPS");
        bgHeader.setTextSize(10);
        bgHeader.setTypeface(Typeface.DEFAULT_BOLD);
        bgHeader.setTextColor(Color.parseColor("#6366F1"));
        LinearLayout.LayoutParams bghLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        bgHeader.setLayoutParams(bghLp);
        bgHeaderRow.addView(bgHeader);

        sysBgAppsCountBadge = new TextView(this);
        sysBgAppsCountBadge.setText("0 Apps Running");
        sysBgAppsCountBadge.setTextSize(9);
        sysBgAppsCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        sysBgAppsCountBadge.setTextColor(isDarkTheme ? 0xFFA5B4FC : Color.parseColor("#4F46E5"));
        sysBgAppsCountBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF312E81 : Color.parseColor("#EEF2FF"), dp(10)));
        sysBgAppsCountBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        bgHeaderRow.addView(sysBgAppsCountBadge);
        bgCard.addView(bgHeaderRow);

        TextView bgSub = new TextView(this);
        bgSub.setText("Toggle each app to halt background tasks and instantly reclaim RAM");
        bgSub.setTextSize(10);
        bgSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        bgSub.setPadding(0, dp(1), 0, dp(10));
        bgCard.addView(bgSub);

        // Action Buttons Row (iOS Glossy Buttons: Free RAM & Clean Storage Cache)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, dp(10));

        Button boostBtn = createIosGlossyButton(
            "⚡ Free RAM & Boost",
            Color.parseColor("#6366F1"),
            Color.parseColor("#4F46E5"),
            Color.parseColor("#4338CA"),
            Color.parseColor("#3730A3")
        );
        LinearLayout.LayoutParams bblp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        bblp.setMargins(0, 0, dp(4), 0);
        boostBtn.setLayoutParams(bblp);
        boostBtn.setOnClickListener(v -> {
            int halted = DeviceTelemetryManager.boostAllRam(this, cachedBgApps);
            Toast.makeText(this, "⚡ Boost Complete! Reclaimed RAM across " + halted + " processes.", Toast.LENGTH_SHORT).show();
            updateBackgroundAppsUI(cachedBgApps);
            new Thread(() -> {
                DeviceTelemetryManager.HardwareStats s = DeviceTelemetryManager.getHardwareStats(this);
                runOnUiThread(() -> updateHardwareVitalsUI(s));
            }).start();
        });
        actionRow.addView(boostBtn);

        Button cleanBtn = createIosGlossyButton(
            "🧹 Clean Storage Cache",
            Color.parseColor("#0284C7"),
            Color.parseColor("#0369A1"),
            Color.parseColor("#075985"),
            Color.parseColor("#0C4A6E")
        );
        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        cblp.setMargins(dp(4), 0, 0, 0);
        cleanBtn.setLayoutParams(cblp);
        cleanBtn.setOnClickListener(v -> {
            long cleaned = DeviceTelemetryManager.cleanStorageCache(this);
            String msg = cleaned > 0
                ? "🧹 Cache optimized! Reclaimed " + DeviceTelemetryManager.formatBytes(cleaned) + " storage."
                : "🧹 Storage cache clean & trimmed!";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                DeviceTelemetryManager.HardwareStats s = DeviceTelemetryManager.getHardwareStats(this);
                runOnUiThread(() -> updateHardwareVitalsUI(s));
            }).start();
        });
        actionRow.addView(cleanBtn);
        bgCard.addView(actionRow);

        // Background Apps List Container
        sysBgAppsListContainer = new LinearLayout(this);
        sysBgAppsListContainer.setOrientation(LinearLayout.VERTICAL);
        bgCard.addView(sysBgAppsListContainer);

        content.addView(bgCard);

        return content;
    }

    private void refreshSystemMonitorUI() {
        new Thread(() -> {
            DeviceTelemetryManager.HardwareStats stats = DeviceTelemetryManager.getHardwareStats(this);
            List<DeviceTelemetryManager.AppNetworkUsage> netApps = DeviceTelemetryManager.getNetworkUsageApps(this);
            List<DeviceTelemetryManager.BackgroundAppInfo> bgApps = DeviceTelemetryManager.getRunningBackgroundApps(this);
            cachedBgApps = bgApps;

            runOnUiThread(() -> {
                updateHardwareVitalsUI(stats);
                updateNetworkUsageUI(netApps);
                updateBackgroundAppsUI(bgApps);
            });
        }).start();
    }

    private void updateHardwareVitalsUI(DeviceTelemetryManager.HardwareStats stats) {
        if (stats == null) return;

        // RAM
        if (sysRamSubtitleView != null) {
            sysRamSubtitleView.setText(
                DeviceTelemetryManager.formatBytes(stats.usedRamBytes) + " / " +
                DeviceTelemetryManager.formatBytes(stats.totalRamBytes) + " (" + stats.ramPercent + "%)"
            );
        }
        if (sysRamSubDetailsView != null) {
            sysRamSubDetailsView.setText(
                "Free: " + DeviceTelemetryManager.formatBytes(stats.freeRamBytes) + " | Total: " + DeviceTelemetryManager.formatBytes(stats.totalRamBytes)
            );
        }
        if (sysRamProgressBar != null) {
            int availableWidth = Math.max(dp(120), screenWidth - dp(64));
            int fillWidth = Math.max(dp(8), (availableWidth * stats.ramPercent) / 100);
            ViewGroup.LayoutParams lp = sysRamProgressBar.getLayoutParams();
            lp.width = fillWidth;
            sysRamProgressBar.setLayoutParams(lp);

            int barColor = (stats.ramPercent < 70)
                ? Color.parseColor("#10B981")
                : (stats.ramPercent < 85 ? Color.parseColor("#F59E0B") : Color.parseColor("#E11D48"));
            sysRamProgressBar.setBackground(createPillDrawable(barColor, dp(4)));
        }

        // Storage
        if (sysStorageSubtitleView != null) {
            sysStorageSubtitleView.setText(
                DeviceTelemetryManager.formatBytes(stats.usedStorageBytes) + " / " +
                DeviceTelemetryManager.formatBytes(stats.totalStorageBytes) + " (" + stats.storagePercent + "%)"
            );
        }
        if (sysStorageSubDetailsView != null) {
            sysStorageSubDetailsView.setText(
                "Free: " + DeviceTelemetryManager.formatBytes(stats.freeStorageBytes) + " | Total: " + DeviceTelemetryManager.formatBytes(stats.totalStorageBytes)
            );
        }
        if (sysStorageProgressBar != null) {
            int availableWidth = Math.max(dp(120), screenWidth - dp(64));
            int fillWidth = Math.max(dp(8), (availableWidth * stats.storagePercent) / 100);
            ViewGroup.LayoutParams lp = sysStorageProgressBar.getLayoutParams();
            lp.width = fillWidth;
            sysStorageProgressBar.setLayoutParams(lp);
        }

        // Battery
        if (sysBatteryLevelView != null) {
            sysBatteryLevelView.setText(stats.batteryLevel + "%");
        }
        if (sysBatteryStatusView != null) {
            sysBatteryStatusView.setText(stats.chargingStatusText);
            sysBatteryStatusView.setTextColor(stats.isCharging ? Color.parseColor("#10B981") : (isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B")));
        }
        if (sysBatteryHealthBadgeView != null) {
            sysBatteryHealthBadgeView.setText(stats.batteryHealth);
            sysBatteryHealthBadgeView.setTextColor(stats.batteryHealth.equalsIgnoreCase("Good") ? Color.parseColor("#10B981") : Color.parseColor("#E11D48"));
        }
        if (sysBatteryTechView != null) {
            sysBatteryTechView.setText(stats.batteryTechnology);
        }
        if (sysBatteryTempView != null) {
            sysBatteryTempView.setText(String.format(Locale.US, "%.1f °C", stats.batteryTempC));
        }
        if (sysBatteryVoltageView != null) {
            sysBatteryVoltageView.setText(String.format(Locale.US, "%.2f V", stats.batteryVoltageV));
        }
    }

    private void updateNetworkUsageUI(List<DeviceTelemetryManager.AppNetworkUsage> netApps) {
        if (sysNetworkListContainer == null) return;
        sysNetworkListContainer.removeAllViews();

        int count = netApps != null ? netApps.size() : 0;
        if (sysNetworkCountBadge != null) {
            sysNetworkCountBadge.setText(count + " Apps Active");
        }

        long totalAllBytes = 0;
        long totalRx = 0;
        long totalTx = 0;

        if (netApps != null) {
            for (DeviceTelemetryManager.AppNetworkUsage app : netApps) {
                totalRx += app.rxBytes;
                totalTx += app.txBytes;
                totalAllBytes += app.totalBytes;
            }
        }

        if (sysNetworkSummaryView != null) {
            sysNetworkSummaryView.setText(
                "Total Traffic: " + DeviceTelemetryManager.formatBytes(totalAllBytes) +
                " (⬇️ " + DeviceTelemetryManager.formatBytes(totalRx) + " | ⬆️ " + DeviceTelemetryManager.formatBytes(totalTx) + ")"
            );
        }

        if (netApps == null || netApps.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No active network traffic recorded since boot.");
            emptyText.setTextSize(10);
            emptyText.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#94A3B8"));
            emptyText.setPadding(0, dp(6), 0, dp(6));
            sysNetworkListContainer.addView(emptyText);
            return;
        }

        // Display top active network consumers
        int limit = Math.min(netApps.size(), 6);
        for (int i = 0; i < limit; i++) {
            DeviceTelemetryManager.AppNetworkUsage app = netApps.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            iconLp.setMargins(0, 0, dp(10), 0);
            icon.setLayoutParams(iconLp);
            if (app.icon != null) {
                icon.setImageDrawable(app.icon);
            } else {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            row.addView(icon);

            LinearLayout nameCol = new LinearLayout(this);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            nameCol.setLayoutParams(nameLp);

            TextView name = new TextView(this);
            name.setText(app.appName);
            name.setTextSize(12);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#1E293B"));
            name.setSingleLine(true);
            nameCol.addView(name);

            TextView pkg = new TextView(this);
            pkg.setText(app.packageName);
            pkg.setTextSize(9);
            pkg.setTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
            pkg.setSingleLine(true);
            nameCol.addView(pkg);
            row.addView(nameCol);

            LinearLayout statCol = new LinearLayout(this);
            statCol.setOrientation(LinearLayout.VERTICAL);
            statCol.setGravity(Gravity.END);

            TextView totalTxt = new TextView(this);
            totalTxt.setText(DeviceTelemetryManager.formatBytes(app.totalBytes));
            totalTxt.setTextSize(12);
            totalTxt.setTypeface(Typeface.DEFAULT_BOLD);
            totalTxt.setTextColor(Color.parseColor("#0284C7"));
            statCol.addView(totalTxt);

            TextView rxTxTxt = new TextView(this);
            rxTxTxt.setText("⬇️ " + DeviceTelemetryManager.formatShortBytes(app.rxBytes) + "  ⬆️ " + DeviceTelemetryManager.formatShortBytes(app.txBytes));
            rxTxTxt.setTextSize(9);
            rxTxTxt.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            statCol.addView(rxTxTxt);
            row.addView(statCol);

            sysNetworkListContainer.addView(row);

            if (i < limit - 1) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                div.setBackgroundColor(isDarkTheme ? DesignTokens.getBorderSubtle(true) : Color.parseColor("#F1F5F9"));
                sysNetworkListContainer.addView(div);
            }
        }
    }

    private void updateBackgroundAppsUI(List<DeviceTelemetryManager.BackgroundAppInfo> bgApps) {
        if (sysBgAppsListContainer == null) return;
        sysBgAppsListContainer.removeAllViews();

        int count = bgApps != null ? bgApps.size() : 0;
        int runningCount = 0;
        long totalBgRam = 0;
        if (bgApps != null) {
            for (DeviceTelemetryManager.BackgroundAppInfo app : bgApps) {
                if (app.isRunning) {
                    totalBgRam += app.ramBytes;
                    runningCount++;
                }
            }
        }

        if (sysBgAppsCountBadge != null) {
            if (runningCount > 0) {
                sysBgAppsCountBadge.setText(count + " Apps (" + DeviceTelemetryManager.formatBytes(totalBgRam) + " RAM)");
                sysBgAppsCountBadge.setTextColor(isDarkTheme ? 0xFFA5B4FC : Color.parseColor("#4F46E5"));
                sysBgAppsCountBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF312E81 : Color.parseColor("#EEF2FF"), dp(10)));
            } else {
                sysBgAppsCountBadge.setText(count + " Apps (All Paused • RAM Freed)");
                sysBgAppsCountBadge.setTextColor(isDarkTheme ? 0xFF6EE7B7 : Color.parseColor("#059669"));
                sysBgAppsCountBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF064E3B : Color.parseColor("#ECFDF5"), dp(10)));
            }
        }

        if (bgApps == null || bgApps.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No background apps detected.");
            emptyText.setTextSize(10);
            emptyText.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#94A3B8"));
            emptyText.setPadding(0, dp(6), 0, dp(6));
            sysBgAppsListContainer.addView(emptyText);
            return;
        }

        for (DeviceTelemetryManager.BackgroundAppInfo app : bgApps) {
            LinearLayout item = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLp.setMargins(0, 0, 0, dp(6));
            item.setLayoutParams(itemLp);

            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(36), dp(36));
            iconLp.setMargins(0, 0, dp(10), 0);
            icon.setLayoutParams(iconLp);
            if (app.icon != null) {
                icon.setImageDrawable(app.icon);
            } else {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            item.addView(icon);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            col.setLayoutParams(colLp);

            TextView name = new TextView(this);
            name.setText(app.appName);
            name.setTextSize(12);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#1E293B"));
            name.setSingleLine(true);
            col.addView(name);

            TextView statusText = new TextView(this);
            if (app.isRunning) {
                statusText.setText("⚡ " + DeviceTelemetryManager.formatBytes(app.ramBytes) + " RAM");
                statusText.setTextColor(Color.parseColor("#6366F1"));
            } else {
                statusText.setText("✓ Stopped • Freed " + DeviceTelemetryManager.formatBytes(app.ramBytes));
                statusText.setTextColor(Color.parseColor("#10B981"));
            }
            statusText.setTextSize(10);
            statusText.setTypeface(Typeface.DEFAULT_BOLD);
            col.addView(statusText);

            TextView pkg = new TextView(this);
            pkg.setText(app.packageName);
            pkg.setTextSize(9);
            pkg.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#94A3B8"));
            pkg.setSingleLine(true);
            col.addView(pkg);

            item.addView(col);

            ModernToggleSwitch appSwitch = new ModernToggleSwitch(this);
            appSwitch.setChecked(app.isRunning, false);
            appSwitch.setDarkTheme(isDarkTheme);
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(44), dp(26));
            swLp.setMargins(dp(8), 0, 0, 0);
            appSwitch.setLayoutParams(swLp);

            appSwitch.setOnCheckedChangeListener((sw, isChecked) -> {
                if (!isChecked) {
                    DeviceTelemetryManager.killBackgroundProcess(MainActivity.this, app.packageName);
                    app.isRunning = false;
                    statusText.setText("✓ Stopped • Freed " + DeviceTelemetryManager.formatBytes(app.ramBytes));
                    statusText.setTextColor(Color.parseColor("#10B981"));
                    Toast.makeText(MainActivity.this, "Freed " + DeviceTelemetryManager.formatBytes(app.ramBytes) + " for " + app.appName, Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        DeviceTelemetryManager.HardwareStats s = DeviceTelemetryManager.getHardwareStats(MainActivity.this);
                        runOnUiThread(() -> updateHardwareVitalsUI(s));
                    }).start();
                } else {
                    app.isRunning = true;
                    statusText.setText("⚡ " + DeviceTelemetryManager.formatBytes(app.ramBytes) + " RAM");
                    statusText.setTextColor(Color.parseColor("#6366F1"));
                    Toast.makeText(MainActivity.this, app.appName + " process paused. Open app to restart.", Toast.LENGTH_SHORT).show();
                }
            });

            item.addView(appSwitch);
            item.setOnClickListener(v -> appSwitch.toggle());
            sysBgAppsListContainer.addView(item);
        }
    }

    // ==========================================
    // 5.5. CARD 5: DEEP SYSTEM CLEANER (CCLEANER)
    // ==========================================
    private View createDeepCleanContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // 1. Hero Junk Meter Card
        LinearLayout heroCard = createCardLayout(Color.WHITE, Color.parseColor("#CCFBF1"));
        heroCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        heroCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout heroHeaderRow = new LinearLayout(this);
        heroHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        heroHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView heroHeader = new TextView(this);
        heroHeader.setText("🧹 DEEP CACHE & RESIDUAL STORAGE");
        heroHeader.setTextSize(10);
        heroHeader.setTypeface(Typeface.DEFAULT_BOLD);
        heroHeader.setTextColor(Color.parseColor("#0D9488"));
        LinearLayout.LayoutParams hlLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        heroHeader.setLayoutParams(hlLp);
        heroHeaderRow.addView(heroHeader);

        TextView heroBadge = new TextView(this);
        heroBadge.setText("ZERO-ROOT");
        heroBadge.setTextSize(9);
        heroBadge.setTypeface(Typeface.DEFAULT_BOLD);
        heroBadge.setTextColor(Color.parseColor("#0D9488"));
        heroBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        heroBadge.setBackground(createPillDrawable(Color.parseColor("#F0FDFA"), dp(4)));
        heroHeaderRow.addView(heroBadge);
        heroCard.addView(heroHeaderRow);

        deepTotalJunkView = new TextView(this);
        deepTotalJunkView.setText("Scanning...");
        deepTotalJunkView.setTextSize(26);
        deepTotalJunkView.setTypeface(Typeface.DEFAULT_BOLD);
        deepTotalJunkView.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0D9488"));
        deepTotalJunkView.setPadding(0, dp(4), 0, dp(2));
        heroCard.addView(deepTotalJunkView);

        TextView heroSub = new TextView(this);
        heroSub.setText("TOTAL RECLAIMABLE APPLICATION CACHE & RESIDUAL JUNK");
        heroSub.setTextSize(9);
        heroSub.setTypeface(Typeface.DEFAULT_BOLD);
        heroSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        heroSub.setPadding(0, 0, 0, dp(12));
        heroCard.addView(heroSub);

        // 3-Tile Breakdown Row
        LinearLayout breakdownRow = new LinearLayout(this);
        breakdownRow.setOrientation(LinearLayout.HORIZONTAL);

        // Tile 1: App Cache
        LinearLayout tile1 = createCardLayout(Color.parseColor("#F0FDFA"), Color.parseColor("#CCFBF1"));
        tile1.setOrientation(LinearLayout.VERTICAL);
        tile1.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams t1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        t1Lp.setMargins(0, 0, dp(3), 0);
        tile1.setLayoutParams(t1Lp);

        TextView t1Lbl = new TextView(this);
        t1Lbl.setText("📱 App Cache");
        t1Lbl.setTextSize(9);
        t1Lbl.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0F766E"));
        tile1.addView(t1Lbl);

        deepAppCacheSubView = new TextView(this);
        deepAppCacheSubView.setText("--");
        deepAppCacheSubView.setTextSize(11);
        deepAppCacheSubView.setTypeface(Typeface.DEFAULT_BOLD);
        deepAppCacheSubView.setTextColor(isDarkTheme ? 0xFF5EEAD4 : Color.parseColor("#0D9488"));
        tile1.addView(deepAppCacheSubView);
        breakdownRow.addView(tile1);

        // Tile 2: Thumbnails
        LinearLayout tile2 = createCardLayout(Color.parseColor("#F0FDFA"), Color.parseColor("#CCFBF1"));
        tile2.setOrientation(LinearLayout.VERTICAL);
        tile2.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams t2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        t2Lp.setMargins(dp(3), 0, dp(3), 0);
        tile2.setLayoutParams(t2Lp);

        TextView t2Lbl = new TextView(this);
        t2Lbl.setText("🖼️ Thumbnails");
        t2Lbl.setTextSize(9);
        t2Lbl.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0F766E"));
        tile2.addView(t2Lbl);

        deepThumbnailSubView = new TextView(this);
        deepThumbnailSubView.setText("--");
        deepThumbnailSubView.setTextSize(11);
        deepThumbnailSubView.setTypeface(Typeface.DEFAULT_BOLD);
        deepThumbnailSubView.setTextColor(isDarkTheme ? 0xFF5EEAD4 : Color.parseColor("#0D9488"));
        tile2.addView(deepThumbnailSubView);
        breakdownRow.addView(tile2);

        // Tile 3: Temp & Logs
        LinearLayout tile3 = createCardLayout(Color.parseColor("#F0FDFA"), Color.parseColor("#CCFBF1"));
        tile3.setOrientation(LinearLayout.VERTICAL);
        tile3.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams t3Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        t3Lp.setMargins(dp(3), 0, 0, 0);
        tile3.setLayoutParams(t3Lp);

        TextView t3Lbl = new TextView(this);
        t3Lbl.setText("📝 Temp & Logs");
        t3Lbl.setTextSize(9);
        t3Lbl.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0F766E"));
        tile3.addView(t3Lbl);

        deepTempLogsSubView = new TextView(this);
        deepTempLogsSubView.setText("--");
        deepTempLogsSubView.setTextSize(11);
        deepTempLogsSubView.setTypeface(Typeface.DEFAULT_BOLD);
        deepTempLogsSubView.setTextColor(isDarkTheme ? 0xFF5EEAD4 : Color.parseColor("#0D9488"));
        tile3.addView(deepTempLogsSubView);
        breakdownRow.addView(tile3);

        heroCard.addView(breakdownRow);

        // 2. Action Buttons Row (iOS Glossy Style)
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(12), 0, 0);

        runDeepCleanBtn = createIosGlossyButton(
            "🚀 Run Deep Clean",
            Color.parseColor("#14B8A6"),
            Color.parseColor("#0D9488"),
            Color.parseColor("#0F766E"),
            Color.parseColor("#2DD4BF")
        );
        LinearLayout.LayoutParams rdcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        rdcLp.setMargins(0, 0, dp(4), 0);
        runDeepCleanBtn.setLayoutParams(rdcLp);
        runDeepCleanBtn.setOnClickListener(v -> triggerAutomatedDeepClean());
        btnRow.addView(runDeepCleanBtn);

        runTempPurgeBtn = createIosGlossyButton(
            "🧹 Purge Temp",
            Color.parseColor("#475569"),
            Color.parseColor("#334155"),
            Color.parseColor("#1E293B"),
            Color.parseColor("#64748B")
        );
        LinearLayout.LayoutParams rtpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);
        rtpLp.setMargins(dp(4), 0, 0, 0);
        runTempPurgeBtn.setLayoutParams(rtpLp);
        runTempPurgeBtn.setOnClickListener(v -> triggerTempPurge());
        btnRow.addView(runTempPurgeBtn);

        heroCard.addView(btnRow);
        content.addView(heroCard);

        // Live Cleaning Progress Container (Hidden by default)
        deepProgressLayout = createCardLayout(Color.parseColor("#F0FDFA"), Color.parseColor("#5EEAD4"));
        deepProgressLayout.setOrientation(LinearLayout.VERTICAL);
        deepProgressLayout.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams dplLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dplLp.setMargins(0, dp(10), 0, 0);
        deepProgressLayout.setLayoutParams(dplLp);
        deepProgressLayout.setVisibility(View.GONE);

        deepProgressTitle = new TextView(this);
        deepProgressTitle.setText("Cleaning in progress...");
        deepProgressTitle.setTextSize(12);
        deepProgressTitle.setTypeface(Typeface.DEFAULT_BOLD);
        deepProgressTitle.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0F766E"));
        deepProgressLayout.addView(deepProgressTitle);

        deepProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        deepProgressBar.setMax(100);
        deepProgressBar.setProgress(0);
        deepProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#0D9488")));
        deepProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(isDarkTheme ? 0xFF134E4A : Color.parseColor("#CCFBF1")));
        LinearLayout.LayoutParams dpbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        dpbLp.setMargins(0, dp(6), 0, dp(6));
        deepProgressBar.setLayoutParams(dpbLp);
        deepProgressLayout.addView(deepProgressBar);

        LinearLayout progBottom = new LinearLayout(this);
        progBottom.setOrientation(LinearLayout.HORIZONTAL);
        progBottom.setGravity(Gravity.CENTER_VERTICAL);

        deepProgressFreed = new TextView(this);
        deepProgressFreed.setText("Freed: 0 B");
        deepProgressFreed.setTextSize(10);
        deepProgressFreed.setTypeface(Typeface.DEFAULT_BOLD);
        deepProgressFreed.setTextColor(isDarkTheme ? 0xFF5EEAD4 : Color.parseColor("#0D9488"));
        LinearLayout.LayoutParams dpfLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        deepProgressFreed.setLayoutParams(dpfLp);
        progBottom.addView(deepProgressFreed);

        deepProgressStopBtn = createStyledButton("❌ Stop", Color.parseColor("#FEE2E2"));
        deepProgressStopBtn.setTextColor(Color.parseColor("#EF4444"));
        deepProgressStopBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        deepProgressStopBtn.setTextSize(10);
        deepProgressStopBtn.setOnClickListener(v -> {
            DeepCleanManager.stopAutomatedClean(this);
            deepProgressLayout.setVisibility(View.GONE);
            Toast.makeText(this, "Clean stopped", Toast.LENGTH_SHORT).show();
        });
        progBottom.addView(deepProgressStopBtn);

        deepProgressLayout.addView(progBottom);
        content.addView(deepProgressLayout);

        // ==========================================
        // SECTION 2: 3 DEEP PERMISSIONS (CCLEANER SPEC)
        // ==========================================
        LinearLayout permCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        permCard.setOrientation(LinearLayout.VERTICAL);
        permCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams pclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pclp.setMargins(0, dp(10), 0, 0);
        permCard.setLayoutParams(pclp);

        TextView permHeader = new TextView(this);
        permHeader.setText("🛡️ CCLEANER PRO ACCESS (3 DEEP PERMISSIONS)");
        permHeader.setTextSize(10);
        permHeader.setTypeface(Typeface.DEFAULT_BOLD);
        permHeader.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0D9488"));
        permCard.addView(permHeader);

        TextView permSub = new TextView(this);
        permSub.setText("Enables deep filesystem scans, StorageStats computation, and zero-root cache automation");
        permSub.setTextSize(10);
        permSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        permSub.setPadding(0, dp(1), 0, dp(10));
        permCard.addView(permSub);

        // Perm 1: Usage Access
        permCard.addView(createPermissionItemRow(
            "📊 1. Usage Access (StorageStats)",
            "Required to read deep per-app cache and internal storage sizes",
            perm1StatusView = new TextView(this),
            perm1Btn = createStyledButton("Grant", Color.parseColor("#0D9488")),
            v -> startActivity(DeepCleanManager.getUsageAccessIntent(this))
        ));

        View div1 = new View(this);
        div1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div1.setBackgroundColor(isDarkTheme ? DesignTokens.getBorderSubtle(true) : Color.parseColor("#F1F5F9"));
        permCard.addView(div1);

        // Perm 2: All Files Storage Access
        permCard.addView(createPermissionItemRow(
            "📂 2. All Files Storage Access",
            "Required to scan and purge thumbnail dumps and orphaned temp files",
            perm2StatusView = new TextView(this),
            perm2Btn = createStyledButton("Grant", Color.parseColor("#0D9488")),
            v -> startActivity(DeepCleanManager.getAllFilesAccessIntent(this))
        ));

        View div2 = new View(this);
        div2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div2.setBackgroundColor(isDarkTheme ? DesignTokens.getBorderSubtle(true) : Color.parseColor("#F1F5F9"));
        permCard.addView(div2);

        // Perm 3: Automated Accessibility Service
        permCard.addView(createPermissionItemRow(
            "🤖 3. Automated Accessibility Service",
            "Required to cycle through applications and clear caches one by one without root",
            perm3StatusView = new TextView(this),
            perm3Btn = createStyledButton("Enable", Color.parseColor("#0D9488")),
            v -> startActivity(DeepCleanManager.getAccessibilityIntent(this))
        ));

        content.addView(permCard);

        // ==========================================
        // SECTION 3: APPLICATION CACHE LEADERBOARD
        // ==========================================
        LinearLayout appCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        appCard.setOrientation(LinearLayout.VERTICAL);
        appCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams aclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aclp.setMargins(0, dp(10), 0, 0);
        appCard.setLayoutParams(aclp);

        LinearLayout appHeadRow = new LinearLayout(this);
        appHeadRow.setOrientation(LinearLayout.HORIZONTAL);
        appHeadRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView appHead = new TextView(this);
        appHead.setText("APPLICATIONS BY CACHE FOOTPRINT");
        appHead.setTextSize(10);
        appHead.setTypeface(Typeface.DEFAULT_BOLD);
        appHead.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0D9488"));
        LinearLayout.LayoutParams ahlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        appHead.setLayoutParams(ahlp);
        appHeadRow.addView(appHead);

        deepAppsCountBadge = new TextView(this);
        deepAppsCountBadge.setText("0 APPS");
        deepAppsCountBadge.setTextSize(9);
        deepAppsCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        deepAppsCountBadge.setTextColor(isDarkTheme ? 0xFF2DD4BF : Color.parseColor("#0D9488"));
        deepAppsCountBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        deepAppsCountBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF134E4A : Color.parseColor("#F0FDFA"), dp(4)));
        appHeadRow.addView(deepAppsCountBadge);
        appCard.addView(appHeadRow);

        TextView appSub = new TextView(this);
        appSub.setText("Sorted descending by internal cache volume. Tap 'Clean' to clear cache individually.");
        appSub.setTextSize(10);
        appSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        appSub.setPadding(0, dp(1), 0, dp(8));
        appCard.addView(appSub);

        deepAppListContainer = new LinearLayout(this);
        deepAppListContainer.setOrientation(LinearLayout.VERTICAL);
        appCard.addView(deepAppListContainer);

        content.addView(appCard);

        return content;
    }

    private View createPermissionItemRow(String title, String subtitle, TextView statusView, Button actionBtn, View.OnClickListener clickListener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        col.setLayoutParams(colLp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#1E293B"));
        col.addView(t);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(0, dp(3), 0, dp(3));

        statusView.setTextSize(8.5f);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
        statusView.setSingleLine(true);
        statusView.setPadding(dp(6), dp(2), dp(6), dp(2));
        badgeRow.addView(statusView);
        col.addView(badgeRow);

        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextSize(9);
        sub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        sub.setPadding(0, 0, dp(4), 0);
        col.addView(sub);
        row.addView(col);

        actionBtn.setTextSize(10);
        actionBtn.setTextColor(Color.WHITE);
        actionBtn.setPadding(dp(12), dp(5), dp(12), dp(5));
        actionBtn.setOnClickListener(clickListener);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        bLp.setMargins(dp(8), 0, 0, 0);
        actionBtn.setLayoutParams(bLp);
        row.addView(actionBtn);

        return row;
    }

    private void triggerAutomatedDeepClean() {
        if (!DeepCleanManager.isAccessibilityEnabled(this)) {
            new AlertDialog.Builder(this)
                .setTitle("🤖 Enable Accessibility Runner")
                .setMessage("To automatically cycle through applications and tap 'Clear Cache' without root (just like CCleaner), the Deep Cleaner needs its Accessibility Service enabled.\n\nWould you like to open Accessibility Settings now, or run an Instant Temp Purge instead?")
                .setPositiveButton("Open Settings", (d, w) -> startActivity(DeepCleanManager.getAccessibilityIntent(this)))
                .setNegativeButton("Instant Temp Purge", (d, w) -> triggerTempPurge())
                .setNeutralButton("Cancel", null)
                .show();
            return;
        }

        if (currentJunkOverview == null || currentJunkOverview.appList == null || currentJunkOverview.appList.isEmpty()) {
            Toast.makeText(this, "Scanning applications... Please wait a moment.", Toast.LENGTH_SHORT).show();
            refreshDeepCleanUI();
            return;
        }

        List<String> targets = new ArrayList<>();
        for (DeepCleanManager.AppJunkInfo app : currentJunkOverview.appList) {
            if (app.cacheBytes > 0) {
                targets.add(app.packageName);
            }
        }
        if (targets.isEmpty()) {
            for (DeepCleanManager.AppJunkInfo app : currentJunkOverview.appList) {
                targets.add(app.packageName);
                if (targets.size() >= 15) break;
            }
        }

        if (targets.isEmpty()) {
            Toast.makeText(this, "No apps requiring cache cleanup!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deepProgressLayout != null) {
            deepProgressLayout.setVisibility(View.VISIBLE);
            deepProgressTitle.setText("Starting automated deep clean...");
            deepProgressBar.setMax(targets.size());
            deepProgressBar.setProgress(0);
        }

        DeepCleanManager.startAutomatedClean(this, targets, new DeepCleanManager.CleaningListener() {
            @Override
            public void onProgress(int current, int total, String currentPackage, String appName, long freedBytes) {
                if (deepProgressLayout != null) {
                    deepProgressLayout.setVisibility(View.VISIBLE);
                    deepProgressTitle.setText("Cleaning " + appName + " (" + current + " of " + total + ")...");
                    deepProgressBar.setProgress(current);
                    deepProgressFreed.setText("Freed: " + DeepCleanManager.formatBytes(freedBytes));
                }
            }

            @Override
            public void onComplete(int totalCleaned, long totalFreedBytes) {
                if (deepProgressLayout != null) {
                    deepProgressLayout.setVisibility(View.GONE);
                }
                Toast.makeText(MainActivity.this, "🎉 Deep Clean Complete! Freed " + DeepCleanManager.formatBytes(totalFreedBytes) + " across " + totalCleaned + " apps!", Toast.LENGTH_LONG).show();
                refreshDeepCleanUI();
            }

            @Override
            public void onCancelled() {
                if (deepProgressLayout != null) {
                    deepProgressLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (deepProgressLayout != null) {
                    deepProgressLayout.setVisibility(View.GONE);
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerTempPurge() {
        Toast.makeText(this, "Purging residual thumbnails, temp, and cache files...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            DeepCleanManager.PurgeReport report = DeepCleanManager.purgeResidualAndTempFiles(MainActivity.this);
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "✅ " + report.summaryMessage, Toast.LENGTH_LONG).show();
                refreshDeepCleanUI();
            });
        }).start();
    }

    private void refreshDeepCleanUI() {
        // 1. Update 3 Permissions Status
        boolean p1 = DeepCleanManager.hasUsageAccess(this);
        boolean p2 = DeepCleanManager.hasAllFilesAccess(this);
        boolean p3 = DeepCleanManager.isAccessibilityEnabled(this);

        if (perm1StatusView != null) {
            perm1StatusView.setText(p1 ? "GRANTED ✅" : "REQUIRED ⚠️");
            perm1StatusView.setTextColor(p1 ? Color.parseColor("#10B981") : Color.parseColor("#D97706"));
            perm1StatusView.setBackground(createPillDrawable(p1 ? Color.parseColor("#ECFDF5") : Color.parseColor("#FFFBEB"), dp(4)));
        }
        if (perm1Btn != null) {
            perm1Btn.setText(p1 ? "Settings" : "Grant");
            perm1Btn.setBackground(createPillDrawable(p1 ? Color.parseColor("#64748B") : Color.parseColor("#0D9488"), dp(6)));
        }

        if (perm2StatusView != null) {
            perm2StatusView.setText(p2 ? "GRANTED ✅" : "REQUIRED ⚠️");
            perm2StatusView.setTextColor(p2 ? Color.parseColor("#10B981") : Color.parseColor("#D97706"));
            perm2StatusView.setBackground(createPillDrawable(p2 ? Color.parseColor("#ECFDF5") : Color.parseColor("#FFFBEB"), dp(4)));
        }
        if (perm2Btn != null) {
            perm2Btn.setText(p2 ? "Settings" : "Grant");
            perm2Btn.setBackground(createPillDrawable(p2 ? Color.parseColor("#64748B") : Color.parseColor("#0D9488"), dp(6)));
        }

        if (perm3StatusView != null) {
            perm3StatusView.setText(p3 ? "ACTIVE ✅" : "INACTIVE ⚠️");
            perm3StatusView.setTextColor(p3 ? Color.parseColor("#10B981") : Color.parseColor("#D97706"));
            perm3StatusView.setBackground(createPillDrawable(p3 ? Color.parseColor("#ECFDF5") : Color.parseColor("#FFFBEB"), dp(4)));
        }
        if (perm3Btn != null) {
            perm3Btn.setText(p3 ? "Settings" : "Enable");
            perm3Btn.setBackground(createPillDrawable(p3 ? Color.parseColor("#64748B") : Color.parseColor("#0D9488"), dp(6)));
        }

        // 2. Scan Storage & Apps Junk in Background Thread
        new Thread(() -> {
            DeepCleanManager.JunkOverview overview = DeepCleanManager.scanAllJunk(MainActivity.this);
            currentJunkOverview = overview;

            runOnUiThread(() -> {
                if (deepTotalJunkView != null) {
                    deepTotalJunkView.setText(overview.getFormattedTotal());
                }
                if (deepAppCacheSubView != null) {
                    deepAppCacheSubView.setText(overview.getFormattedAppCache());
                }
                if (deepThumbnailSubView != null) {
                    deepThumbnailSubView.setText(overview.getFormattedThumbnails());
                }
                if (deepTempLogsSubView != null) {
                    deepTempLogsSubView.setText(overview.getFormattedTempLogs());
                }
                if (deepAppsCountBadge != null) {
                    deepAppsCountBadge.setText(overview.appList.size() + " APPS");
                }
                renderDeepAppList(overview.appList);
            });
        }).start();
    }

    private void renderDeepAppList(List<DeepCleanManager.AppJunkInfo> apps) {
        if (deepAppListContainer == null) return;
        deepAppListContainer.removeAllViews();

        if (apps == null || apps.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No applications found.");
            empty.setTextSize(11);
            empty.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#94A3B8"));
            empty.setPadding(0, dp(10), 0, dp(10));
            deepAppListContainer.addView(empty);
            return;
        }

        int limit = Math.min(apps.size(), 30);
        for (int i = 0; i < limit; i++) {
            DeepCleanManager.AppJunkInfo app = apps.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));

            ImageView iconView = new ImageView(this);
            if (app.icon != null) {
                iconView.setImageDrawable(app.icon);
            } else {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            icLp.setMargins(0, 0, dp(10), 0);
            iconView.setLayoutParams(icLp);
            row.addView(iconView);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            col.setLayoutParams(colLp);

            TextView name = new TextView(this);
            name.setText(app.appName);
            name.setTextSize(12);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#1E293B"));
            name.setSingleLine(true);
            col.addView(name);

            LinearLayout detailsRow = new LinearLayout(this);
            detailsRow.setOrientation(LinearLayout.HORIZONTAL);
            detailsRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView cacheTxt = new TextView(this);
            cacheTxt.setText(app.isCleaned ? "✓ Cache Cleared" : (app.cacheBytes > 0 ? app.getFormattedCache() + " Cache" : "0 B Cache"));
            cacheTxt.setTextSize(10);
            cacheTxt.setTypeface(Typeface.DEFAULT_BOLD);
            cacheTxt.setTextColor(app.isCleaned ? Color.parseColor("#10B981") : (app.cacheBytes > 1024 * 1024 * 50 ? Color.parseColor("#E11D48") : Color.parseColor("#D97706")));
            detailsRow.addView(cacheTxt);

            TextView dot = new TextView(this);
            dot.setText(" • ");
            dot.setTextSize(9);
            dot.setTextColor(Color.parseColor("#94A3B8"));
            detailsRow.addView(dot);

            TextView totTxt = new TextView(this);
            totTxt.setText(app.getFormattedTotal() + " total");
            totTxt.setTextSize(9);
            totTxt.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
            detailsRow.addView(totTxt);

            col.addView(detailsRow);
            row.addView(col);

            PremiumButton cleanBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            cleanBtn.setText("Clean");
            cleanBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_TEAL_DARK : DesignTokens.ACCENT_TEAL_PRIMARY);
            cleanBtn.setTextSize(11);
            cleanBtn.setOnClickListener(v -> {
                if (DeepCleanManager.isAccessibilityEnabled(MainActivity.this)) {
                    List<String> singleTarget = new ArrayList<>();
                    singleTarget.add(app.packageName);
                    DeepCleanManager.startAutomatedClean(MainActivity.this, singleTarget, new DeepCleanManager.CleaningListener() {
                        @Override
                        public void onProgress(int current, int total, String currentPackage, String appName, long freedBytes) {}
                        @Override
                        public void onComplete(int totalCleaned, long totalFreedBytes) {
                            runOnUiThread(() -> refreshDeepCleanUI());
                        }
                        @Override
                        public void onCancelled() {}
                        @Override
                        public void onError(String errorMessage) {}
                    });
                } else {
                    DeepCleanManager.launchAppDetails(MainActivity.this, app.packageName);
                }
            });
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            cbLp.setMargins(dp(6), 0, 0, 0);
            cleanBtn.setLayoutParams(cbLp);
            row.addView(cleanBtn);

            deepAppListContainer.addView(row);

            if (i < limit - 1) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                div.setBackgroundColor(isDarkTheme ? DesignTokens.getBorderSubtle(true) : Color.parseColor("#F8FAFC"));
                deepAppListContainer.addView(div);
            }
        }
    }

    // ==========================================
    // 5. CARD 2: BIOMETRICS CONTENT
    // ==========================================
    private View createBiometricsContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // Hydration Card (Aesthetic Sky Blue Card)
        LinearLayout waterCard = createCardLayout(
            isDarkTheme ? 0xFF0C2436 : Color.parseColor("#F0F9FF"),
            isDarkTheme ? 0xFF144566 : Color.parseColor("#BAE6FD"));
        waterCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        waterCard.setOrientation(LinearLayout.VERTICAL);

        TextView waterHeader = new TextView(this);
        waterHeader.setText("💧 DAILY HYDRATION GOAL");
        waterHeader.setTextSize(10);
        waterHeader.setTypeface(Typeface.DEFAULT_BOLD);
        waterHeader.setTextColor(isDarkTheme ? 0xFF38BDF8 : Color.parseColor("#0284C7"));
        waterCard.addView(waterHeader);

        hydrationTextView = new TextView(this);
        hydrationTextView.setText("2250 / 3000 ml (75%)");
        hydrationTextView.setTextSize(20);
        hydrationTextView.setTypeface(Typeface.DEFAULT_BOLD);
        hydrationTextView.setTextColor(isDarkTheme ? 0xFFF0F9FF : Color.parseColor("#0F172A"));
        hydrationTextView.setPadding(0, dp(3), 0, dp(6));
        waterCard.addView(hydrationTextView);

        hydrationProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        hydrationProgressBar.setMax(BiometricsManager.DAILY_WATER_GOAL_ML);
        hydrationProgressBar.setProgress(2250);
        hydrationProgressBar.setProgressTintList(ColorStateList.valueOf(isDarkTheme ? 0xFF38BDF8 : Color.parseColor("#0284C7")));
        hydrationProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(isDarkTheme ? 0xFF163248 : Color.parseColor("#E0F2FE")));
        hydrationProgressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));
        waterCard.addView(hydrationProgressBar);

        PremiumStepper waterStepper = new PremiumStepper(this, isDarkTheme);
        waterStepper.setAccentColor(isDarkTheme ? DesignTokens.ACCENT_SKY_DARK : DesignTokens.ACCENT_SKY_PRIMARY);
        waterStepper.setLabel("Quick Log: ±250 ml");
        LinearLayout.LayoutParams stepperLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stepperLp.setMargins(0, dp(10), 0, 0);
        waterStepper.setLayoutParams(stepperLp);
        waterStepper.setOnStepperActionListener(new PremiumStepper.OnStepperActionListener() {
            @Override
            public void onMinus() {
                BiometricsManager.addWater(MainActivity.this, -250);
                refreshHydrationUI();
            }

            @Override
            public void onPlus() {
                BiometricsManager.addWater(MainActivity.this, 250);
                refreshHydrationUI();
            }
        });
        waterCard.addView(waterStepper);
        content.addView(waterCard);

        // Nutritional Protocol Checklist
        TextView macroHeader = new TextView(this);
        macroHeader.setText("DAILY NUTRITIONAL PROTOCOL (5 TARGETS)");
        macroHeader.setTextSize(10);
        macroHeader.setTypeface(Typeface.DEFAULT_BOLD);
        macroHeader.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
        macroHeader.setPadding(dp(2), dp(12), 0, dp(6));
        content.addView(macroHeader);

        String[][] macros = new String[][]{
            {"morning_fuel", "Fuel 1: Roti+PB + Chana + Peanuts + Nuts + Moringa (+Sun: D3)", "34g protein, Tyrosine dopamine, low-GI zinc & Vit C iron catalyst; Sun D3"},
            {"power_lunch", "Fuel 2: Soya Chunks (50g) + Veggies + 1 Roti Cap + Lemon", "30g clean protein + Vit C catalyst for iron; zero post-lunch coma"},
            {"glut4_walk", "🚶 Mandatory 10-Minute Post-Lunch Walk", "AMPK & GLUT4 glucose disposal (zero afternoon coma)"},
            {"evening_eggs", "Fuel 3: 6 PM 4 Boiled Eggs (Tue: Paneer) + Pumpkin Seeds + Tata Creatine", "560mg Choline for Acetylcholine & Tata 1mg Creatine for brain ATP; GABA sleep priming"},
            {"clean_dinner", "Fuel 4: Moong Khichdi + Curd (Finish by 9 PM for 3h Sleep Gap)", "Probiotic GABA & calcium; glymphatic brain flush in deep sleep"}
        };

        for (String[] m : macros) {
            final String key = m[0];
            LinearLayout item = createCardLayout(
                isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
                isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0"));
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            item.setLayoutParams(lp);

            PremiumCheckbox cb = new PremiumCheckbox(this, isDarkTheme);
            cb.setAccentColor(isDarkTheme ? DesignTokens.ACCENT_ROSE_DARK : DesignTokens.ACCENT_ROSE_PRIMARY);
            cb.setChecked(BiometricsManager.isMacroChecked(this, key), false);
            cb.setOnCheckedChangeListener((checkbox, isChecked) -> {
                BiometricsManager.setMacroChecked(this, key, isChecked);
            });
            item.addView(cb);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(dp(10), 0, 0, 0);

            TextView t1 = new TextView(this);
            t1.setText(m[1]);
            t1.setTextSize(12);
            t1.setTypeface(Typeface.DEFAULT_BOLD);
            t1.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
            textCol.addView(t1);

            TextView t2 = new TextView(this);
            t2.setText(m[2]);
            t2.setTextSize(10);
            t2.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
            textCol.addView(t2);

            item.addView(textCol);
            item.setOnClickListener(v -> cb.performClick());
            content.addView(item);
        }

        // Circadian & Energy Protocols
        TextView circHeader = new TextView(this);
        circHeader.setText("CIRCADIAN & ENERGY PROTOCOLS");
        circHeader.setTextSize(10);
        circHeader.setTypeface(Typeface.DEFAULT_BOLD);
        circHeader.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
        circHeader.setPadding(dp(2), dp(12), 0, dp(6));
        content.addView(circHeader);

        String[][] circuits = new String[][]{
            {"🌅 Morning Sunlight Sync (08:30 AM)", "Direct optic reset of master circadian clock & dopamine"},
            {"🚶 Post-Lunch 10-Minute Walk (02:00 PM)", "Clears post-meal blood sugar to eliminate afternoon coma"},
            {"🥚 6 PM Brain Energy Reset", "3 Whole eggs (Tue: Paneer) + Creatine for continuous brain ATP"},
            {"🌙 3-Hour Pre-Bed Fasting Gap", "Empty stomach sleep for full glymphatic brain detox"}
        };

        for (String[] c : circuits) {
            LinearLayout item = createCardLayout(
                isDarkTheme ? ThemeManager.CARD_BG_DARK : Color.WHITE,
                isDarkTheme ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0"));
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            item.setLayoutParams(lp);

            TextView t1 = new TextView(this);
            t1.setText(c[0]);
            t1.setTextSize(12);
            t1.setTypeface(Typeface.DEFAULT_BOLD);
            t1.setTextColor(isDarkTheme ? 0xFF38BDF8 : Color.parseColor("#0284C7"));
            item.addView(t1);

            TextView t2 = new TextView(this);
            t2.setText(c[1]);
            t2.setTextSize(10);
            t2.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
            t2.setPadding(0, dp(2), 0, 0);
            item.addView(t2);

            content.addView(item);
        }

        return content;
    }

    private void refreshHydrationUI() {
        if (hydrationTextView == null || hydrationProgressBar == null) return;
        int ml = BiometricsManager.getWaterIntakeMl(this);
        int goal = BiometricsManager.DAILY_WATER_GOAL_ML;
        int percent = Math.min(100, (ml * 100) / goal);
        hydrationTextView.setText(ml + " / " + goal + " ml (" + percent + "%)");
        hydrationProgressBar.setProgress(ml);
    }

    // ==========================================
    // 6. TICKER & ROUTINE UPDATES
    // ==========================================
    private void startLiveTicker() {
        tickerHandler = new Handler(Looper.getMainLooper());
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                updateActivePhaseUI();
                updateHeroCadenceStatus();
                if (homeClockView != null) {
                    homeClockView.setText(new SimpleDateFormat("HH:mm • EEEE, MMM d", Locale.getDefault()).format(new Date()));
                }
                tickerHandler.postDelayed(this, 1000);
            }
        };
        tickerHandler.post(tickerRunnable);
    }

    private void updateActivePhaseUI() {
        ScheduleManager.Phase current = ScheduleManager.getCurrentPhase();
        if (current == null) {
            if (currentTitleView != null) currentTitleView.setText("All Phases Inactive");
            return;
        }

        if (currentRomanView != null) {
            currentRomanView.setText(current.getFormattedTime12());
        }
        if (currentTitleView != null) {
            currentTitleView.setText(current.getDisplayTitle());
        }
        if (currentDirectiveView != null) {
            currentDirectiveView.setText("What to do: " + current.getDisplayProtocol());
        }
        if (currentWhyView != null) {
            currentWhyView.setText("Why: " + current.biologicalWhy);
        }
        if (currentCountdownView != null) {
            if (AppEngineManager.isAppEnabled(this)) {
                currentCountdownView.setText("Next boundary in " + calculateRemainingTime(current));
                currentCountdownView.setTextColor(Color.parseColor("#0284C7"));
            } else {
                currentCountdownView.setText("⏸️ System Engine PAUSED (All routine alarms stopped)");
                currentCountdownView.setTextColor(Color.parseColor("#DC2626"));
            }
        }
    }

    private String calculateRemainingTime(ScheduleManager.Phase p) {
        if (p == null) return "--:--";
        Calendar now = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, p.endHour);
        end.set(Calendar.MINUTE, p.endMinute);
        end.set(Calendar.SECOND, 0);

        if (end.before(now)) {
            end.add(Calendar.DAY_OF_YEAR, 1);
        }

        long diffMs = end.getTimeInMillis() - now.getTimeInMillis();
        long diffMins = (diffMs / (1000 * 60)) % 60;
        long diffHours = (diffMs / (1000 * 60 * 60));
        return String.format("%02dh %02dm", diffHours, diffMins);
    }

    private void triggerTestAlarm() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            try {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
                Toast.makeText(this, "Please switch 'Allow notifications' ON", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
            return;
        }

        AlarmAudioService.startAlarm(
            this,
            "Test Cadence Alert",
            "LIVE TEST",
            "Playing: Lana Del Rey — Summertime Sadness",
            "Now"
        );

        Toast.makeText(this, "🎵 Playing Lana Del Rey — Summertime Sadness! Use 'Stop' or notification button to stop.", Toast.LENGTH_LONG).show();
    }

    private void showDecoderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        builder.setTitle("📖 Brain Fuel & Bioavailability Guide");

        String msg = "• 90%+ Bioavailability Law: Curd is eaten ONLY at night so calcium never blocks daytime Iron/Zinc at DMT-1 receptors.\n\n"
                   + "• Zero Palak Hassle: 3 Whole Eggs provide 300% more bioavailable retinal Lutein/Zeaxanthin than spinach, with zero oxalates!\n\n"
                   + "• Soya Double-Boil & Lemon: Boiling twice removes oligosaccharides; fresh lemon (Vit C) boosts iron uptake by 300%.\n\n"
                   + "• Soaked & Peeled Nuts / Ground Flax: Removes phytates and breaks outer seed hull to unlock 100% of Omega-3s.\n\n"
                   + "• Creatine at 6:00 PM: Carbohydrate & sodium co-transport pushes 25% more creatine into muscle & cortical neurons.\n\n"
                   + "• 3-Hour Pre-Bed Sleep Gap: Finish dinner by 9:00 PM so stomach is empty for 12:00 AM glymphatic brain detox.";

        builder.setMessage(msg);
        builder.setPositiveButton("Understood", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ==========================================
    // 7. CARD 3: COMPLETE BLUEPRINT VIEWER (.MD)
    // ==========================================
    private View createBlueprintContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // Hero Card (Aesthetic Rose Petal Tint)
        LinearLayout heroCard = createCardLayout(Color.parseColor("#FFF1F2"), Color.parseColor("#FDA4AF"));
        heroCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        heroCard.setOrientation(LinearLayout.VERTICAL);

        TextView heroTitle = new TextView(this);
        heroTitle.setText("📖 HIGH-OCTANE BRAIN FUEL BLUEPRINT");
        heroTitle.setTextSize(12);
        heroTitle.setTypeface(Typeface.DEFAULT_BOLD);
        heroTitle.setTextColor(Color.parseColor("#E11D48"));
        heroCard.addView(heroTitle);

        TextView heroSub = new TextView(this);
        heroSub.setText("90%+ Bioavailability Architecture for AI/ML & Deep Robotics Engineering. Zero nutrient competition, zero anti-nutrient blockage.");
        heroSub.setTextSize(11);
        heroSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#334155"));
        heroSub.setPadding(0, dp(4), 0, dp(10));
        heroCard.addView(heroSub);

        Button backBtnTop = createStyledButton("📅 Jump to Daily Schedule", Color.parseColor("#E11D48"));
        backBtnTop.setOnClickListener(v -> {
            openFeature(FeatureRegistry.ID_ROUTINE);
        });
        heroCard.addView(backBtnTop);
        content.addView(heroCard);

        // Blueprint Data Array: {Title, Badge, Bullet 1, Bullet 2}
        String[][] blueprintSections = new String[][]{
            {
                "1. 🍌 Bananas: 1 to 2 Daily Max (Not 4)",
                "GLYCEMIC CONTROL",
                "• Why NOT 4 bananas: 4 bananas dump ~110g carbs & 56g sugars, triggering an acute insulin spike followed by reactive hypoglycemia (brain fog and dopamine receptor down-regulation).",
                "• Optimal Rule: Exactly 1 banana at breakfast (paired with peanut butter and nuts to flatten the glucose curve) + optionally 1 before 6 PM training."
            },
            {
                "2. 🌰 Walnuts & Almonds: Soaking & Peeling Rule",
                "85%+ MINERALS",
                "• Almond Peeling: Raw almond skins are packed with phytic acid and enzyme-inhibiting tannins that block zinc and iron absorption. Soak 8-10h overnight and slip off brown skin in the morning.",
                "• Walnuts: 2-3 whole walnuts (4-6 halves) soaked for 4-6h soften bitter polyphenols and unlock ALA Omega-3s for neuronal myelin sheath repair."
            },
            {
                "3. 🥛 Curd (Dahi): Strictly at Night with Dinner",
                "DMT-1 RECEPTOR LAW",
                "• Never with Lunch: Calcium (Ca2+) and Iron (Fe2+) fight for the exact same transporter (DMT-1) in the small intestine. Curd at lunch blocks 50-60% of plant iron.",
                "• Eat at 8:30 PM: Consuming curd with Moong Dal Khichdi at night supplies bioavailable calcium & casein for deep-sleep GABA and melatonin, while probiotics rebuild gut flora.",
                "• Next-Day Prep (11:40 PM): Whisk 1/2 tsp starter into 150ml-200ml lukewarm milk (~40°C) and leave in warm spot overnight for fresh, live probiotics tomorrow night."
            },
            {
                "4. 🫘 Soya Chunks: 50g-60g Double-Boiled & Squeezed",
                "30g CLEAN PROTEIN",
                "• Dry vs Boiled: 50g-60g dry expands to 160g-180g boiled (~30g pure protein, optimal single-meal leucine threshold). 80g dry expands to 250g+ and causes digestive heaviness.",
                "• Double-Boil: Boil 5 min, dump boiling water (flushes gas-causing raffinose/stachyose), squeeze firmly twice, sauté with 1/2 fresh lemon (Vitamin C boosts iron uptake 300%)."
            },
            {
                "5. 🌻 Morning Seeds Protocol: Pumpkin, Alsi & Chia",
                "PREPARATION LAW",
                "• 🎃 Pumpkin Seeds (1 Tbsp): Eat raw or lightly dry-roasted with breakfast. Chew thoroughly. Supplies ~3mg bioavailable Zinc and 75mg Magnesium for dopamine synthesis and cellular ATP.\n\n• 🌾 Alsi / Flaxseeds (1 Tbsp): MUST BE ROASTED & GROUND into powder! Whole seeds pass through undigested. Dry-roast in pan for 2 mins, grind, store in airtight jar, and sprinkle 1 Tbsp over your PB roti or morning bowl.\n\n• 💧 Chia Seeds (1 Tbsp): MUST BE SOAKED in 150-200ml water for 15-20 mins (or overnight). The soluble fiber creates a hydrophilic gel that flattens banana glucose spikes and delivers sustained hydration."
            },
            {
                "6. 💡 Zero-Palak Rule: Why You Can 100% Skip It",
                "NO MARKET HASSLE",
                "• Why Skip: Spinach has ~800mg oxalates per 100g (binds calcium/iron) and rots in 48 hours in the fridge.",
                "• The Egg Replacement: 3 Whole Eggs contain the most bioavailable Lutein & Zeaxanthin on Earth (embedded in lipid micelles, 300%+ higher retinal absorption than spinach, zero oxalates).\n• Long-life veggies: Cabbage, carrots, and capsicum last 2-3 weeks in fridge!"
            },
            {
                "7. ⚡ The 7 Bioavailability Catalyst Laws (Zero Nutrient Interference)",
                "MASTER CATALYSTS",
                "1. Calcium-Iron Separation: Curd at night only; never with morning Moringa/Chana or lunch Soya.\n2. Ascorbic Acid: Fresh lemon on morning moringa, chana, and lunch soya converts Fe3+ to soluble Fe2+ (+300% uptake).\n3. Phytate Strip: Soak almonds, peel skin, chew pumpkin seeds.\n4. Fat Carrier: Dr. Excel D3 & moringa carotenoids require breakfast fats (PB/peanuts/nuts).\n5. Piperine Multiplier: Black pepper with turmeric boosts curcumin 2000%.\n6. Osmotic Hydration: Sendha namak in morning water triggers SGLT1 cellular pump.\n7. Creatine Shuttling: Tata 1mg Creatine taken with 6 PM egg protein for insulin-driven CRT uptake."
            },
            {
                "8. 💊 Supplements: Tata Creatine, Moringa & Dr. Excel D3",
                "PROVEN STACK",
                "• Tata 1mg Creatine Monohydrate: 3g-5g pure micronized daily at 5:50 PM in 200ml water with boiled eggs. Regenerates cerebral ATP for cognitive stamina.\n• Organic India Moringa Powder: 1.5g-3g (1/2-1 tsp) daily in morning warm lemon water or with breakfast. Zero calcium clash.\n• Dr. Excel D3 60,000 IU: 1 softgel once weekly on Sunday with breakfast fats (PB/peanuts/nuts). Strictly weekly (never daily)."
            },
            {
                "9. 🛒 Smart Weekly Grocery Shopping List",
                "WEEKLY PLAN",
                "• Eggs: 1-2 crates (30-60) — lasts 4 weeks in fridge.\n• Soya Chunks & Roasted Chana: 1 kg each — 3-6 months pantry shelf life.\n• Peanut Butter, Nuts & Seeds: 500g-1kg — airtight jars.\n• Bananas: Buy 6-7 twice a week.\n• Lemons: Buy 10-12 once a week (crisper drawer).\n• Cabbage & Carrots: Lasts 2-3 weeks in fridge crisper.\n• Moong Dal, Rice, Ghee, Mustard Oil, Sendha Namak: Pantry staples."
            }
        };

        for (String[] sec : blueprintSections) {
            LinearLayout card = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(clp);

            LinearLayout topHeader = new LinearLayout(this);
            topHeader.setOrientation(LinearLayout.HORIZONTAL);
            topHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView secTitle = new TextView(this);
            secTitle.setText(sec[0]);
            secTitle.setTextSize(13);
            secTitle.setTypeface(Typeface.DEFAULT_BOLD);
            secTitle.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
            LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            secTitle.setLayoutParams(stLp);
            topHeader.addView(secTitle);

            TextView badge = new TextView(this);
            badge.setText(sec[1]);
            badge.setTextSize(9);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setTextColor(isDarkTheme ? 0xFF38BDF8 : Color.parseColor("#0284C7"));
            badge.setPadding(dp(6), dp(2), dp(6), dp(2));
            GradientDrawable bbg = new GradientDrawable();
            bbg.setColor(isDarkTheme ? 0xFF0C2436 : Color.parseColor("#F0F9FF"));
            bbg.setCornerRadius(dp(6));
            bbg.setStroke(dp(1), isDarkTheme ? 0xFF144566 : Color.parseColor("#BAE6FD"));
            badge.setBackground(bbg);
            topHeader.addView(badge);
            card.addView(topHeader);

            if (sec.length > 2 && sec[2] != null && !sec[2].isEmpty()) {
                TextView d1 = new TextView(this);
                d1.setText(sec[2]);
                d1.setTextSize(11);
                d1.setTextColor(isDarkTheme ? 0xFFE2E8F0 : Color.parseColor("#334155"));
                d1.setPadding(0, dp(6), 0, dp(3));
                card.addView(d1);
            }

            if (sec.length > 3 && sec[3] != null && !sec[3].isEmpty()) {
                TextView d2 = new TextView(this);
                d2.setText(sec[3]);
                d2.setTextSize(11);
                d2.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
                card.addView(d2);
            }

            content.addView(card);
        }

        Button backBtnBottom = createStyledButton("📅 Back to Daily Schedule", Color.parseColor("#E11D48"));
        backBtnBottom.setOnClickListener(v -> {
            openFeature(FeatureRegistry.ID_ROUTINE);
        });
        content.addView(backBtnBottom);

        return content;
    }

    // ==========================================
    // 6. POMODORO ENGINE CONTENT (CARD 1)
    // ==========================================
    private View createPomodoroContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // Hero Timer Card (Aesthetic Porcelain White with soft border)
        LinearLayout timerCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        timerCard.setPadding(dp(18), dp(18), dp(18), dp(18));

        // Top Header within Card
        LinearLayout cardHeader = new LinearLayout(this);
        cardHeader.setOrientation(LinearLayout.HORIZONTAL);
        cardHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView pomoTitle = new TextView(this);
        pomoTitle.setText("FOCUS INTERVAL");
        pomoTitle.setTextSize(11);
        pomoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pomoTitle.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pomoTitle.setLayoutParams(ptLp);
        cardHeader.addView(pomoTitle);

        pomoPhaseBadgeView = new TextView(this);
        pomoPhaseBadgeView.setText("FOCUS SESSION");
        pomoPhaseBadgeView.setTextSize(10);
        pomoPhaseBadgeView.setTypeface(Typeface.DEFAULT_BOLD);
        pomoPhaseBadgeView.setTextColor(isDarkTheme ? 0xFFFB7185 : Color.parseColor("#E11D48"));
        pomoPhaseBadgeView.setPadding(dp(8), dp(3), dp(8), dp(3));
        pomoPhaseBadgeView.setBackground(createPillDrawable(isDarkTheme ? 0xFF38141C : Color.parseColor("#FFF1F2"), dp(6)));
        cardHeader.addView(pomoPhaseBadgeView);
        timerCard.addView(cardHeader);

        // Huge Countdown Display
        pomoTimerView = new TextView(this);
        pomoTimerView.setText("25:00");
        pomoTimerView.setTextSize(48);
        pomoTimerView.setTypeface(Typeface.DEFAULT_BOLD);
        pomoTimerView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        pomoTimerView.setGravity(Gravity.CENTER);
        pomoTimerView.setPadding(0, dp(12), 0, dp(2));
        timerCard.addView(pomoTimerView);

        // Session status subtext
        pomoSessionView = new TextView(this);
        pomoSessionView.setText("Session 1 / 4  ·  🎯 ⚪ ⚪ ⚪");
        pomoSessionView.setTextSize(12);
        pomoSessionView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        pomoSessionView.setGravity(Gravity.CENTER);
        pomoSessionView.setPadding(0, 0, 0, dp(12));
        timerCard.addView(pomoSessionView);

        // Horizontal Progress Bar
        pomoProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pomoProgressBar.setMax(100);
        pomoProgressBar.setProgress(0);
        pomoProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E11D48")));
        pomoProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(isDarkTheme ? 0xFF333A48 : Color.parseColor("#E2E8F0")));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        pbLp.setMargins(0, 0, 0, dp(16));
        pomoProgressBar.setLayoutParams(pbLp);
        timerCard.addView(pomoProgressBar);

        // Main Controls Row: Toggle (Start/Pause), Reset, Skip
        LinearLayout controlRow = new LinearLayout(this);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);

        pomoToggleBtn = createStyledButton("▶ Start Focus", Color.parseColor("#E11D48"));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
        pomoToggleBtn.setLayoutParams(tLp);
        pomoToggleBtn.setOnClickListener(v -> PomodoroManager.toggle(this));
        controlRow.addView(pomoToggleBtn);

        Button resetBtn = createOutlinedButton("↺ Reset", Color.parseColor("#334155"), Color.parseColor("#CBD5E1"));
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        rLp.setMargins(dp(6), 0, 0, 0);
        resetBtn.setLayoutParams(rLp);
        resetBtn.setOnClickListener(v -> PomodoroManager.reset(this));
        controlRow.addView(resetBtn);

        Button skipBtn = createOutlinedButton("⏭ Skip", Color.parseColor("#334155"), Color.parseColor("#CBD5E1"));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        sLp.setMargins(dp(6), 0, 0, 0);
        skipBtn.setLayoutParams(sLp);
        skipBtn.setOnClickListener(v -> PomodoroManager.skip(this));
        controlRow.addView(skipBtn);

        timerCard.addView(controlRow);
        content.addView(timerCard);

        // Quick Presets Header
        TextView presetHeader = new TextView(this);
        presetHeader.setText("FOCUS PRESETS & INTERVALS");
        presetHeader.setTextSize(10);
        presetHeader.setTypeface(Typeface.DEFAULT_BOLD);
        presetHeader.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        presetHeader.setPadding(dp(4), dp(16), 0, dp(8));
        content.addView(presetHeader);

        // Preset Grid Row 1 (Work presets)
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        Button b25 = createStyledButton("25m Classic Focus", Color.parseColor("#0284C7"));
        LinearLayout.LayoutParams lpB25 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpB25.setMargins(0, 0, dp(4), dp(6));
        b25.setLayoutParams(lpB25);
        b25.setOnClickListener(v -> PomodoroManager.setPreset(this, "WORK", PomodoroManager.DURATION_WORK_DEFAULT));
        row1.addView(b25);

        Button b50 = createStyledButton("50m Deep Flow", Color.parseColor("#0284C7"));
        LinearLayout.LayoutParams lpB50 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpB50.setMargins(dp(4), 0, 0, dp(6));
        b50.setLayoutParams(lpB50);
        b50.setOnClickListener(v -> PomodoroManager.setPreset(this, "WORK", PomodoroManager.DURATION_DEEP_WORK));
        row1.addView(b50);
        content.addView(row1);

        // Preset Grid Row 2 (Break presets)
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        Button b5 = createStyledButton("5m Short Break", Color.parseColor("#059669"));
        LinearLayout.LayoutParams lpB5 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpB5.setMargins(0, 0, dp(4), 0);
        b5.setLayoutParams(lpB5);
        b5.setOnClickListener(v -> PomodoroManager.setPreset(this, "SHORT_BREAK", PomodoroManager.DURATION_SHORT_BREAK));
        row2.addView(b5);

        Button b15 = createStyledButton("15m Long Break", Color.parseColor("#059669"));
        LinearLayout.LayoutParams lpB15 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpB15.setMargins(dp(4), 0, 0, 0);
        b15.setLayoutParams(lpB15);
        b15.setOnClickListener(v -> PomodoroManager.setPreset(this, "LONG_BREAK", PomodoroManager.DURATION_LONG_BREAK));
        row2.addView(b15);
        content.addView(row2);

        // Sound & Notification Alert Controls Card
        LinearLayout alertCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        alertCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        alertCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams aclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aclp.setMargins(0, dp(14), 0, 0);
        alertCard.setLayoutParams(aclp);

        TextView alertHeader = new TextView(this);
        alertHeader.setText("🔊 SOUND & NOTIFICATION CONTROLS");
        alertHeader.setTextSize(10);
        alertHeader.setTypeface(Typeface.DEFAULT_BOLD);
        alertHeader.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        alertCard.addView(alertHeader);

        TextView alertSub = new TextView(this);
        alertSub.setText("Toggle audible chimes and heads-up alerts on session completion");
        alertSub.setTextSize(10);
        alertSub.setTextColor(isDarkTheme ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
        alertSub.setPadding(0, dp(1), 0, dp(10));
        alertCard.addView(alertSub);

        LinearLayout alertRow = new LinearLayout(this);
        alertRow.setOrientation(LinearLayout.HORIZONTAL);

        pomoSoundToggleBtn = createStyledButton(
            SoundManager.isSoundEnabled(this) ? "🔔 Sound Alert: ON" : "🔕 Sound Alert: OFF",
            SoundManager.isSoundEnabled(this) ? Color.parseColor("#E11D48") : Color.parseColor("#64748B")
        );
        LinearLayout.LayoutParams sToggleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        sToggleLp.setMargins(0, 0, dp(4), 0);
        pomoSoundToggleBtn.setLayoutParams(sToggleLp);
        pomoSoundToggleBtn.setOnClickListener(v -> {
            boolean current = SoundManager.isSoundEnabled(this);
            boolean updated = !current;
            SoundManager.setSoundEnabled(this, updated);
            if (updated) {
                SoundManager.previewChime(this);
                Toast.makeText(this, "Sound alerts ENABLED (preview chime played)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sound alerts MUTED", Toast.LENGTH_SHORT).show();
            }
            updateSoundControlsUI();
        });
        alertRow.addView(pomoSoundToggleBtn);

        pomoNotifToggleBtn = createStyledButton(
            SoundManager.isNotificationEnabled(this) ? "📲 Notification: ON" : "📴 Notification: OFF",
            SoundManager.isNotificationEnabled(this) ? Color.parseColor("#0284C7") : Color.parseColor("#64748B")
        );
        LinearLayout.LayoutParams nToggleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        nToggleLp.setMargins(dp(4), 0, 0, 0);
        pomoNotifToggleBtn.setLayoutParams(nToggleLp);
        pomoNotifToggleBtn.setOnClickListener(v -> {
            boolean current = SoundManager.isNotificationEnabled(this);
            boolean updated = !current;
            SoundManager.setNotificationEnabled(this, updated);
            if (updated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
            Toast.makeText(this, updated ? "Completion notifications ENABLED" : "Completion notifications DISABLED", Toast.LENGTH_SHORT).show();
            updateSoundControlsUI();
        });
        alertRow.addView(pomoNotifToggleBtn);
        alertCard.addView(alertRow);

        content.addView(alertCard);

        // Parallel Engine Status Card
        LinearLayout parallelCard = createCardLayout(Color.parseColor("#F0F9FF"), Color.parseColor("#BAE6FD"));
        parallelCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams pclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pclp.setMargins(0, dp(14), 0, 0);
        parallelCard.setLayoutParams(pclp);

        TextView parTitle = new TextView(this);
        parTitle.setText("⚡ PARALLEL ENGINE SYNCHRONIZATION");
        parTitle.setTextSize(10);
        parTitle.setTypeface(Typeface.DEFAULT_BOLD);
        parTitle.setTextColor(isDarkTheme ? 0xFF38BDF8 : Color.parseColor("#0284C7"));
        parallelCard.addView(parTitle);

        pomoParallelDietView = new TextView(this);
        pomoParallelDietView.setText("• Diet Cadence: Active & Independent\n• Pomodoro Engine: Ready\n• Home Screen Widget: Enabled");
        pomoParallelDietView.setTextSize(11);
        pomoParallelDietView.setTextColor(isDarkTheme ? 0xFFE2E8F0 : Color.parseColor("#334155"));
        pomoParallelDietView.setPadding(0, dp(4), 0, 0);
        parallelCard.addView(pomoParallelDietView);

        Button pinWidgetBtn = createStyledButton("📱 Add Widget to Home Screen", Color.parseColor("#0284C7"));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wlp.setMargins(0, dp(10), 0, 0);
        pinWidgetBtn.setLayoutParams(wlp);
        pinWidgetBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);
                ComponentName myProvider = new ComponentName(this, PomodoroWidgetProvider.class);
                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported()) {
                    appWidgetManager.requestPinAppWidget(myProvider, null, null);
                    Toast.makeText(this, "Prompting to pin Pomodoro Widget!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please long-press home screen -> Widgets -> Pomodoro Focus", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Please long-press home screen -> Widgets -> Pomodoro Focus", Toast.LENGTH_LONG).show();
            }
        });
        parallelCard.addView(pinWidgetBtn);

        content.addView(parallelCard);

        // Today's Stats Card
        LinearLayout statsCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        statsCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sclp.setMargins(0, dp(10), 0, 0);
        statsCard.setLayoutParams(sclp);

        TextView statsTitle = new TextView(this);
        statsTitle.setText("📈 TODAY'S PRODUCTIVITY METRICS");
        statsTitle.setTextSize(10);
        statsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        statsTitle.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        statsCard.addView(statsTitle);

        pomoStatsView = new TextView(this);
        pomoStatsView.setText("• Completed Sessions: 0 / 8\n• Total Focus Time: 0 mins\n• Target: 4.0 Hours Daily Deep Work");
        pomoStatsView.setTextSize(11);
        pomoStatsView.setTextColor(isDarkTheme ? 0xFFE2E8F0 : Color.parseColor("#334155"));
        pomoStatsView.setPadding(0, dp(4), 0, 0);
        statsCard.addView(pomoStatsView);
        content.addView(statsCard);

        return content;
    }

    private void refreshPomodoroUI() {
        String phase = PomodoroManager.getPhase(this);
        String status = PomodoroManager.getStatus(this);
        int remaining = PomodoroManager.getRemainingSeconds(this);
        int total = PomodoroManager.getTotalSeconds(this);
        int completed = PomodoroManager.getCompletedToday(this);
        updatePomodoroUI(phase, status, remaining, total, completed);
        updateSoundControlsUI();
    }

    private void updateSoundControlsUI() {
        if (pomoSoundToggleBtn != null) {
            boolean soundOn = SoundManager.isSoundEnabled(this);
            pomoSoundToggleBtn.setText(soundOn ? "🔔 Sound Alert: ON" : "🔕 Sound Alert: OFF");
            if (pomoSoundToggleBtn instanceof PremiumButton) {
                PremiumButton pb = (PremiumButton) pomoSoundToggleBtn;
                pb.setStyle(soundOn ? PremiumButton.Style.PRIMARY : PremiumButton.Style.SECONDARY);
                pb.setCustomAccent(soundOn ? DesignTokens.ACCENT_ROSE_PRIMARY : 0);
            }
        }
        if (pomoNotifToggleBtn != null) {
            boolean notifOn = SoundManager.isNotificationEnabled(this);
            pomoNotifToggleBtn.setText(notifOn ? "📲 Notification: ON" : "📴 Notification: OFF");
            if (pomoNotifToggleBtn instanceof PremiumButton) {
                PremiumButton pb = (PremiumButton) pomoNotifToggleBtn;
                pb.setStyle(notifOn ? PremiumButton.Style.PRIMARY : PremiumButton.Style.SECONDARY);
                pb.setCustomAccent(notifOn ? DesignTokens.ACCENT_SKY_PRIMARY : 0);
            }
        }
    }

    private void toggleAppEngine() {
        boolean newState = !AppEngineManager.isAppEnabled(this);
        setAppEngineState(newState);
    }

    private void setAppEngineState(boolean newState) {
        if (AppEngineManager.isAppEnabled(this) == newState) return;
        AppEngineManager.setAppEnabled(this, newState);

        if (newState) {
            startTelemetryTracking();
            Toast.makeText(this, "🟢 System Engine Activated (Alarms scheduled)", Toast.LENGTH_SHORT).show();
        } else {
            stopTelemetryTracking();
            Toast.makeText(this, "⏸️ System Engine Paused (All routine alarms stopped)", Toast.LENGTH_SHORT).show();
        }

        updateEngineStateUI();
        updateActivePhaseUI();
        refreshPomodoroUI();
    }

    private void updateEngineStateUI() {
        boolean isEnabled = AppEngineManager.isAppEnabled(this);

        // Sync Home Tile & Sidebar Modern iOS Toggle Switches
        if (homeEngineSwitch != null && homeEngineSwitch.isChecked() != isEnabled) {
            homeEngineSwitch.setChecked(isEnabled, true);
        }
        if (homeEngineStatusText != null) {
            homeEngineStatusText.setText(isEnabled ? "🟢 Active" : "⏸️ Paused");
            homeEngineStatusText.setTextColor(isEnabled ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        }
        if (sidebarEngineSwitch != null && sidebarEngineSwitch.isChecked() != isEnabled) {
            sidebarEngineSwitch.setChecked(isEnabled, true);
        }
        if (sidebarStatusText != null) {
            sidebarStatusText.setText(isEnabled ? "🟢 ACTIVE & SCHEDULED" : "⏸️ ENGINE PAUSED");
            sidebarStatusText.setTextColor(isEnabled ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        }
        if (launcherStatusPill != null) {
            launcherStatusPill.setText(isEnabled ? "🟢 ACTIVE" : "⏸️ PAUSED");
            launcherStatusPill.setTextColor(isEnabled ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        }
    }

    private void showTimePicker(boolean isWake) {
        int currentH = isWake ? ScheduleManager.getWakeHour(this) : ScheduleManager.getSleepHour(this);
        int currentM = isWake ? ScheduleManager.getWakeMinute(this) : ScheduleManager.getSleepMinute(this);

        TimePickerDialog dialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                int wakeH = isWake ? hourOfDay : ScheduleManager.getWakeHour(this);
                int wakeM = isWake ? minute : ScheduleManager.getWakeMinute(this);
                int sleepH = !isWake ? hourOfDay : ScheduleManager.getSleepHour(this);
                int sleepM = !isWake ? minute : ScheduleManager.getSleepMinute(this);

                ScheduleManager.setScheduleTimings(this, wakeH, wakeM, sleepH, sleepM);
                updateScheduleTimingsUI();
                renderCadenceList();
                updateActivePhaseUI();

                String label = isWake ? "Wake-up" : "Sleep";
                String formatted = ScheduleManager.Phase.formatTime12(hourOfDay, minute);
                Toast.makeText(this, "✨ " + label + " set to " + formatted + " (Schedule recalculated)", Toast.LENGTH_SHORT).show();
            },
            currentH,
            currentM,
            false
        );
        dialog.setTitle(isWake ? "Set Wake-up Time" : "Set Sleep Time");
        dialog.show();
    }

    private void updateScheduleTimingsUI() {
        if (wakeTimeView == null || sleepTimeView == null) return;
        int wakeH = ScheduleManager.getWakeHour(this);
        int wakeM = ScheduleManager.getWakeMinute(this);
        int sleepH = ScheduleManager.getSleepHour(this);
        int sleepM = ScheduleManager.getSleepMinute(this);

        wakeTimeView.setText(ScheduleManager.Phase.formatTime12(wakeH, wakeM));
        sleepTimeView.setText(ScheduleManager.Phase.formatTime12(sleepH, sleepM));

        int wakeTotal = ((wakeH * 60 + wakeM) % 1440 + 1440) % 1440;
        int sleepTotal = ((sleepH * 60 + sleepM) % 1440 + 1440) % 1440;
        int wakeDuration = (sleepTotal - wakeTotal + 1440) % 1440;
        if (wakeDuration == 0) wakeDuration = 1440;
        int sleepDuration = 1440 - wakeDuration;

        float wakeHours = Math.round((wakeDuration / 60.0f) * 10.0f) / 10.0f;
        float sleepHours = Math.round((sleepDuration / 60.0f) * 10.0f) / 10.0f;
        String wStr = (wakeHours == (int) wakeHours) ? String.valueOf((int) wakeHours) : String.format("%.1f", wakeHours);
        String sStr = (sleepHours == (int) sleepHours) ? String.valueOf((int) sleepHours) : String.format("%.1f", sleepHours);

        if (scheduleSummaryView != null) {
            scheduleSummaryView.setText(wStr + "h awake • " + sStr + "h restorative sleep • " + ScheduleManager.PHASES.size() + " phases aligned");
        }
    }

    private void updatePomodoroUI(String phase, String status, int remaining, int total, int completed) {
        if (pomoTimerView == null) return;

        pomoTimerView.setText(PomodoroManager.formatTime(remaining));

        boolean isRunning = "RUNNING".equals(status);
        if (pomoToggleBtn != null) {
            pomoToggleBtn.setText(isRunning ? "⏸ Pause Focus" : "▶ Start Focus");
            if (pomoToggleBtn instanceof PremiumButton) {
                PremiumButton pb = (PremiumButton) pomoToggleBtn;
                pb.setCustomAccent(isRunning ? (isDarkTheme ? DesignTokens.ACCENT_SKY_DARK : DesignTokens.ACCENT_SKY_PRIMARY)
                                             : (isDarkTheme ? DesignTokens.ACCENT_ROSE_DARK : DesignTokens.ACCENT_ROSE_PRIMARY));
            } else {
                pomoToggleBtn.setBackground(createPillDrawable(isRunning ? Color.parseColor("#0284C7") : Color.parseColor("#E11D48"), dp(8)));
            }
        }

        if (pomoPhaseBadgeView != null) {
            String badgeText = "WORK".equals(phase) ? "FOCUS SESSION" : ("LONG_BREAK".equals(phase) ? "LONG BREAK" : "SHORT BREAK");
            pomoPhaseBadgeView.setText(badgeText);
            int color = "WORK".equals(phase) ? (isDarkTheme ? 0xFFFB7185 : Color.parseColor("#E11D48")) : (isDarkTheme ? 0xFF34D399 : Color.parseColor("#059669"));
            pomoPhaseBadgeView.setTextColor(color);
            int bgColor = "WORK".equals(phase) ? (isDarkTheme ? 0xFF38141C : Color.parseColor("#FFF1F2")) : (isDarkTheme ? 0xFF064E3B : Color.parseColor("#ECFDF5"));
            pomoPhaseBadgeView.setBackground(createPillDrawable(bgColor, dp(6)));
        }

        if (pomoSessionView != null) {
            int sessionInCycle = (completed % 4) + 1;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 4; i++) {
                if (i < sessionInCycle) {
                    sb.append("🍅 ");
                } else if (i == sessionInCycle) {
                    sb.append("🎯 ");
                } else {
                    sb.append("⚪ ");
                }
            }
            pomoSessionView.setText("Session " + sessionInCycle + " / 4  ·  " + sb.toString().trim());
        }

        if (pomoProgressBar != null) {
            int prog = total > 0 ? (int) (((long) (total - remaining) * 100) / total) : 0;
            pomoProgressBar.setProgress(prog);
            int pColor = "WORK".equals(phase) ? Color.parseColor("#E11D48") : Color.parseColor("#059669");
            pomoProgressBar.setProgressTintList(ColorStateList.valueOf(pColor));
        }

        if (pomoStatsView != null) {
            int totalMins = PomodoroManager.getTotalFocusMinutesToday(this);
            pomoStatsView.setText("• Completed Sessions Today: " + completed + "\n• Total Focus Time: " + totalMins + " mins\n• Status: " + (isRunning ? "Active in flow state 🚀" : "Standby / Ready"));
        }

        if (pomoParallelDietView != null) {
            ScheduleManager.Phase curPhase = ScheduleManager.getCurrentPhase();
            String dietName = curPhase != null ? curPhase.getDisplayTitle() : "Active Routine";
            boolean engineOn = AppEngineManager.isAppEnabled(this);
            pomoParallelDietView.setText("• System Engine: " + (engineOn ? "ACTIVE & SCHEDULED" : "PAUSED (OFF)") +
                "\n• Diet Cadence: " + dietName + (engineOn ? " (Active)" : " (Paused)") +
                "\n• Pomodoro Engine: " + (isRunning ? "RUNNING (" + PomodoroManager.formatTime(remaining) + ")" : "READY") +
                "\n• Home Screen Widget: Synchronized");
        }
    }

    private Button createOutlinedButton(String text, int textColor, int borderColor) {
        PremiumButton btn = new PremiumButton(this, PremiumButton.Style.OUTLINED, isDarkTheme);
        btn.setText(text);
        if (textColor == Color.parseColor("#DC2626") || textColor == Color.RED) {
            btn.setStyle(PremiumButton.Style.DESTRUCTIVE);
        } else if (textColor == Color.parseColor("#10B981") || textColor == Color.parseColor("#059669")) {
            btn.setStyle(PremiumButton.Style.SUCCESS);
        } else {
            btn.setTextColor(isDarkTheme ? ThemeManager.TEXT_PRIMARY_DARK : textColor);
        }
        return btn;
    }

    // ==========================================
    // 6C. CARD 8: UNIVERSAL MEDIA DOWNLOADER & HARVESTER
    // ==========================================
    private View createMediaDownloaderContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // 1. Storage & Space Overview Card
        LinearLayout storageCard = createCardLayout(Color.parseColor("#F5F3FF"), Color.parseColor("#DDD6FE"));
        storageCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        storageCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout sHeaderRow = new LinearLayout(this);
        sHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        sHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView sTitle = new TextView(this);
        sTitle.setText("📁 MEDIASTORE: Movies/Media • Music/Media");
        sTitle.setTextSize(10);
        sTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sTitle.setTextColor(Color.parseColor("#7C3AED"));
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        sTitle.setLayoutParams(stLp);
        sHeaderRow.addView(sTitle);

        TextView storageSpaceBadge = new TextView(this);
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long freeBytes = stat.getAvailableBytes();
            storageSpaceBadge.setText("💾 " + DownloadJob.formatBytes(freeBytes) + " Free");
        } catch (Exception e) {
            storageSpaceBadge.setText("💾 Ready");
        }
        storageSpaceBadge.setTextSize(9);
        storageSpaceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        storageSpaceBadge.setTextColor(isDarkTheme ? 0xFFC4B5FD : Color.parseColor("#7C3AED"));
        storageSpaceBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        storageSpaceBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF3B1D76 : Color.WHITE, dp(4)));
        sHeaderRow.addView(storageSpaceBadge);
        storageCard.addView(sHeaderRow);

        TextView sSub = new TextView(this);
        sSub.setText("Universal media downloader with zero restrictions. YouTube HD, Instagram, X/Twitter, TikTok, Reddit, Facebook, Pinterest & Web HTML5 streams.");
        sSub.setTextSize(10);
        sSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#6B7280"));
        sSub.setPadding(0, dp(4), 0, 0);
        storageCard.addView(sSub);
        content.addView(storageCard);

        // 2. URL Input & Extraction Card
        LinearLayout inputCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        inputCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        icLp.setMargins(0, dp(12), 0, 0);
        inputCard.setLayoutParams(icLp);

        TextView inTitle = new TextView(this);
        inTitle.setText("🔗 UNIVERSAL MEDIA EXTRACTOR");
        inTitle.setTextSize(11);
        inTitle.setTypeface(Typeface.DEFAULT_BOLD);
        inTitle.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        inputCard.addView(inTitle);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        irLp.setMargins(0, dp(8), 0, 0);
        inputRow.setLayoutParams(irLp);

        downloaderUrlInput = new EditText(this);
        downloaderUrlInput.setHint("Paste any link (YouTube, X, TikTok, Reddit, FB, Web)...");
        downloaderUrlInput.setTextSize(11);
        downloaderUrlInput.setSingleLine(true);
        downloaderUrlInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        downloaderUrlInput.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        downloaderUrlInput.setHintTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#94A3B8"));
        downloaderUrlInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(isDarkTheme ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F8FAFC"));
        inBg.setCornerRadius(dp(8));
        inBg.setStroke(dp(1), isDarkTheme ? DesignTokens.getBorder(true) : Color.parseColor("#CBD5E1"));
        downloaderUrlInput.setBackground(inBg);
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        downloaderUrlInput.setLayoutParams(editLp);
        inputRow.addView(downloaderUrlInput);

        PremiumButton pasteBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
        pasteBtn.setText("📋 Paste");
        pasteBtn.setTextSize(11);
        pasteBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_INDIGO_DARK : DesignTokens.ACCENT_INDIGO_PRIMARY);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        pbLp.setMargins(dp(6), 0, 0, 0);
        pasteBtn.setLayoutParams(pbLp);
        pasteBtn.setOnClickListener(v -> pasteFromClipboard());
        inputRow.addView(pasteBtn);

        PremiumIconButton clearBtn = new PremiumIconButton(this, "✕", isDarkTheme);
        clearBtn.setTextSize(14);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        cbLp.setMargins(dp(4), 0, 0, 0);
        clearBtn.setLayoutParams(cbLp);
        clearBtn.setOnClickListener(v -> {
            if (downloaderUrlInput != null) downloaderUrlInput.setText("");
            if (downloaderSourceBadge != null) downloaderSourceBadge.setVisibility(View.GONE);
            if (downloaderPreviewCard != null) downloaderPreviewCard.setVisibility(View.GONE);
        });
        inputRow.addView(clearBtn);
        inputCard.addView(inputRow);

        // Row for auto-detect source badge and Instagram session cookie button
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brLp.setMargins(0, dp(8), 0, 0);
        badgeRow.setLayoutParams(brLp);

        downloaderSourceBadge = new TextView(this);
        downloaderSourceBadge.setVisibility(View.GONE);
        downloaderSourceBadge.setTextSize(9);
        downloaderSourceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        downloaderSourceBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badgeRow.addView(downloaderSourceBadge);

        View badgeSpacer = new View(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1, 1.0f);
        badgeSpacer.setLayoutParams(spLp);
        badgeRow.addView(badgeSpacer);

        igSessionBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
        updateIgSessionButtonLabel();
        igSessionBtn.setTextSize(9);
        LinearLayout.LayoutParams igLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        igSessionBtn.setLayoutParams(igLp);
        igSessionBtn.setOnClickListener(v -> showInstagramCookieDialog());
        badgeRow.addView(igSessionBtn);

        inputCard.addView(badgeRow);

        // Listen for typing to show source badge
        downloaderUrlInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString().trim();
                if (text.length() > 5) {
                    MediaSource src = MediaSource.detectFromUrl(text);
                    downloaderSourceBadge.setVisibility(View.VISIBLE);
                    downloaderSourceBadge.setText(src.icon + " " + src.displayName);
                    downloaderSourceBadge.setTextColor(Color.parseColor(src.colorHex));
                    downloaderSourceBadge.setBackground(createPillDrawable(isDarkTheme ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F8FAFC"), dp(6)));
                } else {
                    downloaderSourceBadge.setVisibility(View.GONE);
                }
            }
        });

        // Action Buttons Row: Analyze & Extract + In-App Web Sniffer
        LinearLayout actionBtnRow = new LinearLayout(this);
        actionBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams abrLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        abrLp.setMargins(0, dp(12), 0, 0);
        actionBtnRow.setLayoutParams(abrLp);

        downloaderAnalyzeBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        downloaderAnalyzeBtn.setText("⚡ ANALYZE & EXTRACT");
        downloaderAnalyzeBtn.setTextSize(11);
        downloaderAnalyzeBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_INDIGO_DARK : DesignTokens.ACCENT_INDIGO_PRIMARY);
        LinearLayout.LayoutParams abLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        downloaderAnalyzeBtn.setLayoutParams(abLp);
        downloaderAnalyzeBtn.setOnClickListener(v -> {
            if (downloaderUrlInput != null) {
                String raw = downloaderUrlInput.getText().toString().trim();
                triggerAnalyzeUrl(raw);
            }
        });
        actionBtnRow.addView(downloaderAnalyzeBtn);

        PremiumButton snifferBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
        snifferBtn.setText("🌐 Sniff Web");
        snifferBtn.setTextSize(11);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        sbLp.setMargins(dp(6), 0, 0, 0);
        snifferBtn.setLayoutParams(sbLp);
        snifferBtn.setOnClickListener(v -> {
            String raw = downloaderUrlInput != null ? downloaderUrlInput.getText().toString().trim() : "";
            openInAppMediaSniffer(raw);
        });
        actionBtnRow.addView(snifferBtn);

        inputCard.addView(actionBtnRow);

        // Loading indicator & status
        downloaderLoadingBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        downloaderLoadingBar.setIndeterminate(true);
        downloaderLoadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        lbLp.setMargins(0, dp(8), 0, 0);
        downloaderLoadingBar.setLayoutParams(lbLp);
        inputCard.addView(downloaderLoadingBar);

        downloaderStatusView = new TextView(this);
        downloaderStatusView.setTextSize(10);
        downloaderStatusView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        downloaderStatusView.setVisibility(View.GONE);
        downloaderStatusView.setPadding(0, dp(4), 0, 0);
        inputCard.addView(downloaderStatusView);

        content.addView(inputCard);

        // 3. Media Preview & Resolution Picker Card (Initially Hidden)
        downloaderPreviewCard = createCardLayout(Color.WHITE, Color.parseColor("#DDD6FE"));
        downloaderPreviewCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        downloaderPreviewCard.setVisibility(View.GONE);
        LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpLp.setMargins(0, dp(12), 0, 0);
        downloaderPreviewCard.setLayoutParams(dpLp);

        downloaderTitleView = new TextView(this);
        downloaderTitleView.setTextSize(12);
        downloaderTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        downloaderTitleView.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        downloaderPreviewCard.addView(downloaderTitleView);

        downloaderMetaBadgeView = new TextView(this);
        downloaderMetaBadgeView.setTextSize(10);
        downloaderMetaBadgeView.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        downloaderMetaBadgeView.setPadding(0, dp(2), 0, dp(8));
        downloaderPreviewCard.addView(downloaderMetaBadgeView);

        downloaderThumbView = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170));
        ivLp.setMargins(0, 0, 0, dp(10));
        downloaderThumbView.setLayoutParams(ivLp);
        downloaderThumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setColor(Color.parseColor("#F1F5F9"));
        thumbBg.setCornerRadius(dp(10));
        downloaderThumbView.setBackground(thumbBg);
        if (Build.VERSION.SDK_INT >= 21) {
            downloaderThumbView.setClipToOutline(true);
        }
        downloaderPreviewCard.addView(downloaderThumbView);

        downloaderFormatsContainer = new LinearLayout(this);
        downloaderFormatsContainer.setOrientation(LinearLayout.VERTICAL);
        downloaderPreviewCard.addView(downloaderFormatsContainer);

        content.addView(downloaderPreviewCard);

        // 4. WhatsApp Status Harvester Card
        LinearLayout waCard = createCardLayout(Color.parseColor("#F0FDF4"), Color.parseColor("#BBF7D0"));
        waCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams waLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        waLp.setMargins(0, dp(12), 0, 0);
        waCard.setLayoutParams(waLp);

        LinearLayout waHeaderRow = new LinearLayout(this);
        waHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        waHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView waTitle = new TextView(this);
        waTitle.setText("💬 WHATSAPP STATUS HARVESTER");
        waTitle.setTextSize(11);
        waTitle.setTypeface(Typeface.DEFAULT_BOLD);
        waTitle.setTextColor(Color.parseColor("#15803D"));
        LinearLayout.LayoutParams wtLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        waTitle.setLayoutParams(wtLp);
        waHeaderRow.addView(waTitle);

        whatsappStatusCountBadge = new TextView(this);
        whatsappStatusCountBadge.setText("0 Cached");
        whatsappStatusCountBadge.setTextSize(9);
        whatsappStatusCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        whatsappStatusCountBadge.setTextColor(isDarkTheme ? 0xFF6EE7B7 : Color.parseColor("#15803D"));
        whatsappStatusCountBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        whatsappStatusCountBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF064E3B : Color.WHITE, dp(4)));
        waHeaderRow.addView(whatsappStatusCountBadge);
        waCard.addView(waHeaderRow);

        TextView waSub = new TextView(this);
        waSub.setText("Instant offline status viewer. Save any viewed friend's video or photo status straight to your public gallery.");
        waSub.setTextSize(10);
        waSub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#4B5563"));
        waSub.setPadding(0, dp(4), 0, dp(8));
        waCard.addView(waSub);

        PremiumButton scanWaBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
        scanWaBtn.setText("🔄 Scan Cached Statuses");
        scanWaBtn.setTextSize(11);
        LinearLayout.LayoutParams swLp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        scanWaBtn.setLayoutParams(swLp2);
        scanWaBtn.setOnClickListener(v -> refreshWhatsAppStatusesUI());
        waCard.addView(scanWaBtn);

        whatsappStatusContainer = new LinearLayout(this);
        whatsappStatusContainer.setOrientation(LinearLayout.HORIZONTAL);
        whatsappStatusContainer.setPadding(0, dp(10), 0, 0);

        android.widget.HorizontalScrollView waScroll = new android.widget.HorizontalScrollView(this);
        waScroll.setHorizontalScrollBarEnabled(false);
        waScroll.addView(whatsappStatusContainer);
        waCard.addView(waScroll);

        content.addView(waCard);

        // 5. Active Downloads & Queue Manager Card
        LinearLayout queueCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        queueCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams qcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qcLp.setMargins(0, dp(12), 0, 0);
        queueCard.setLayoutParams(qcLp);

        LinearLayout qHeaderRow = new LinearLayout(this);
        qHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        qHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView qTitle = new TextView(this);
        qTitle.setText("📥 DOWNLOAD MANAGER");
        qTitle.setTextSize(11);
        qTitle.setTypeface(Typeface.DEFAULT_BOLD);
        qTitle.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        LinearLayout.LayoutParams qtLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        qTitle.setLayoutParams(qtLp);
        qHeaderRow.addView(qTitle);

        downloaderQueueBadge = new TextView(this);
        downloaderQueueBadge.setText("0 Active");
        downloaderQueueBadge.setTextSize(9);
        downloaderQueueBadge.setTypeface(Typeface.DEFAULT_BOLD);
        downloaderQueueBadge.setTextColor(isDarkTheme ? 0xFFC4B5FD : Color.parseColor("#7C3AED"));
        downloaderQueueBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        downloaderQueueBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF3B1D76 : Color.parseColor("#F5F3FF"), dp(4)));
        qHeaderRow.addView(downloaderQueueBadge);
        queueCard.addView(qHeaderRow);

        downloaderQueueContainer = new LinearLayout(this);
        downloaderQueueContainer.setOrientation(LinearLayout.VERTICAL);
        queueCard.addView(downloaderQueueContainer);

        content.addView(queueCard);

        renderDownloadQueue();
        refreshWhatsAppStatusesUI();

        return content;
    }

    private void refreshMediaDownloaderUI() {
        renderDownloadQueue();
        refreshWhatsAppStatusesUI();
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null) {
                    String clean = item.getText().toString().trim();
                    if (downloaderUrlInput != null) {
                        downloaderUrlInput.setText(clean);
                        downloaderUrlInput.setSelection(clean.length());
                    }
                    triggerAnalyzeUrl(clean);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not paste from clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateIgSessionButtonLabel() {
        if (igSessionBtn == null) return;
        String cookie = getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                .getString("instagram_session_cookie", "");
        if (cookie != null && !cookie.trim().isEmpty()) {
            igSessionBtn.setText("🔑 IG: Active");
            igSessionBtn.setStyle(PremiumButton.Style.SUCCESS);
        } else {
            igSessionBtn.setText("🔑 IG Session");
            igSessionBtn.setStyle(PremiumButton.Style.SECONDARY);
        }
    }

    private void showInstagramCookieDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔑 Instagram Session Cookie");
        builder.setMessage("Paste your Instagram 'sessionid' cookie to download from accounts you follow and private stories (personal use only):");

        final EditText input = new EditText(this);
        String current = getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                .getString("instagram_session_cookie", "");
        input.setText(current);
        input.setHint("e.g. 12345678%3Aabc...");
        input.setSingleLine(true);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String val = input.getText().toString().trim();
            getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("instagram_session_cookie", val)
                    .apply();
            InstagramExtractorEngine.setSessionCookie(val);
            Toast.makeText(this, val.isEmpty() ? "Instagram cookie cleared" : "Instagram session cookie saved!", Toast.LENGTH_SHORT).show();
            updateIgSessionButtonLabel();
        });

        builder.setNeutralButton("Clear", (dialog, which) -> {
            getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                    .edit()
                    .remove("instagram_session_cookie")
                    .apply();
            InstagramExtractorEngine.setSessionCookie(null);
            Toast.makeText(this, "Instagram cookie cleared", Toast.LENGTH_SHORT).show();
            updateIgSessionButtonLabel();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void triggerAnalyzeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            Toast.makeText(this, "Please enter or paste a valid link", Toast.LENGTH_SHORT).show();
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && downloaderUrlInput != null) {
            imm.hideSoftInputFromWindow(downloaderUrlInput.getWindowToken(), 0);
        }

        downloaderPreviewCard.setVisibility(View.GONE);
        downloaderLoadingBar.setVisibility(View.VISIBLE);
        downloaderStatusView.setVisibility(View.VISIBLE);
        downloaderStatusView.setText("Probing remote streams & resolving media metadata...");
        downloaderAnalyzeBtn.setEnabled(false);

        String igCookie = getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                .getString("instagram_session_cookie", null);

        new Thread(() -> {
            try {
                ExtractedMedia media = MediaResolverRegistry.resolve(rawUrl, igCookie);
                runOnUiThread(() -> {
                    downloaderLoadingBar.setVisibility(View.GONE);
                    downloaderStatusView.setVisibility(View.GONE);
                    downloaderAnalyzeBtn.setEnabled(true);
                    renderMediaPreview(media);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    downloaderLoadingBar.setVisibility(View.GONE);
                    downloaderStatusView.setVisibility(View.VISIBLE);
                    downloaderStatusView.setText("❌ " + (e.getMessage() != null ? e.getMessage() : "Analysis error") + "\n💡 Tip: Tap '🌐 Sniff Web' above to load page in in-app browser sniffer.");
                    downloaderAnalyzeBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void openInAppMediaSniffer(String targetUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(isDarkTheme ? DesignTokens.getSurfaceRoot(true) : Color.WHITE);

        // Top Navigation Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        topBar.setBackgroundColor(isDarkTheme ? DesignTokens.getSurfaceCard(true) : Color.parseColor("#F1F5F9"));

        EditText urlBar = new EditText(this);
        String startUrl = (targetUrl != null && !targetUrl.trim().isEmpty() && targetUrl.startsWith("http")) ? targetUrl.trim() : "https://google.com";
        urlBar.setText(startUrl);
        urlBar.setTextSize(11);
        urlBar.setSingleLine(true);
        urlBar.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        urlBar.setPadding(dp(8), dp(6), dp(8), dp(6));
        GradientDrawable ubBg = new GradientDrawable();
        ubBg.setColor(isDarkTheme ? DesignTokens.getSurfaceInput(true) : Color.WHITE);
        ubBg.setCornerRadius(dp(6));
        ubBg.setStroke(dp(1), isDarkTheme ? DesignTokens.getBorder(true) : Color.parseColor("#CBD5E1"));
        urlBar.setBackground(ubBg);
        LinearLayout.LayoutParams ubLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        urlBar.setLayoutParams(ubLp);
        topBar.addView(urlBar);

        PremiumButton goBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        goBtn.setText("Go");
        goBtn.setTextSize(11);
        LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        gbLp.setMargins(dp(6), 0, 0, 0);
        goBtn.setLayoutParams(gbLp);
        topBar.addView(goBtn);

        PremiumIconButton closeBtn = new PremiumIconButton(this, "✕", isDarkTheme);
        closeBtn.setTextSize(14);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        cbLp.setMargins(dp(6), 0, 0, 0);
        closeBtn.setLayoutParams(cbLp);
        topBar.addView(closeBtn);

        root.addView(topBar);

        // Sniffer Detected Banner
        LinearLayout snifferBanner = new LinearLayout(this);
        snifferBanner.setOrientation(LinearLayout.HORIZONTAL);
        snifferBanner.setGravity(Gravity.CENTER_VERTICAL);
        snifferBanner.setPadding(dp(12), dp(8), dp(12), dp(8));
        snifferBanner.setBackgroundColor(isDarkTheme ? 0xFF064E3B : Color.parseColor("#DCFCE7"));
        snifferBanner.setVisibility(View.GONE);

        TextView snifferText = new TextView(this);
        snifferText.setText("🎯 Media Stream Detected!");
        snifferText.setTextSize(11);
        snifferText.setTypeface(Typeface.DEFAULT_BOLD);
        snifferText.setTextColor(isDarkTheme ? 0xFF6EE7B7 : Color.parseColor("#15803D"));
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        snifferText.setLayoutParams(stLp);
        snifferBanner.addView(snifferText);

        PremiumButton downloadSniffedBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
        downloadSniffedBtn.setText("📥 Download Video");
        downloadSniffedBtn.setTextSize(10);
        snifferBanner.addView(downloadSniffedBtn);

        root.addView(snifferBanner);

        // WebView
        android.webkit.WebView webView = new android.webkit.WebView(this);
        LinearLayout.LayoutParams wvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        webView.setLayoutParams(wvLp);

        android.webkit.WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        final String[] detectedMediaUrl = new String[1];
        final String[] detectedMediaMime = new String[1];

        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String u = request.getUrl().toString();
                    String lower = u.toLowerCase();
                    if (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("googlevideo.com/videoplayback") || (lower.contains("cdninstagram.com") && lower.contains(".mp4")) || (lower.contains("fbcdn.net") && (lower.contains(".mp4") || lower.contains("video")))) {
                        runOnUiThread(() -> {
                            detectedMediaUrl[0] = u;
                            detectedMediaMime[0] = lower.contains(".m3u8") ? "application/x-mpegurl" : "video/mp4";
                            snifferBanner.setVisibility(View.VISIBLE);
                            snifferText.setText("🎯 Stream Detected (" + (lower.contains(".m3u8") ? "M3U8" : "MP4") + ")");
                        });
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                if (urlBar != null) urlBar.setText(url);
            }
        });

        builder.setView(root);
        final AlertDialog dialog = builder.create();

        goBtn.setOnClickListener(v -> {
            String u = urlBar.getText().toString().trim();
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "https://" + u;
            }
            webView.loadUrl(u);
        });

        closeBtn.setOnClickListener(v -> {
            webView.stopLoading();
            webView.destroy();
            dialog.dismiss();
        });

        downloadSniffedBtn.setOnClickListener(v -> {
            String mUrl = detectedMediaUrl[0];
            if (mUrl != null && !mUrl.isEmpty()) {
                String title = (webView.getTitle() != null && !webView.getTitle().isEmpty()) ? webView.getTitle() : ("Sniffed_Media_" + System.currentTimeMillis());
                StreamFormat opt = new StreamFormat(
                        "sniffed_" + System.currentTimeMillis(),
                        "mp4",
                        "Web Stream HD",
                        0,
                        false,
                        mUrl,
                        null,
                        false,
                        0,
                        detectedMediaMime[0] != null ? detectedMediaMime[0] : "video/mp4"
                );
                DownloadJob job = new DownloadJob(
                        "dl_sniff_" + System.currentTimeMillis(),
                        title,
                        MediaSource.GENERIC_WEB,
                        opt,
                        "Movies/Media"
                );
                MediaDownloadManager.getInstance(this).enqueueJob(job);
                Toast.makeText(this, "🚀 Download Started: " + title, Toast.LENGTH_SHORT).show();
                webView.stopLoading();
                webView.destroy();
                dialog.dismiss();
                renderDownloadQueue();
            }
        });

        dialog.show();
        webView.loadUrl(startUrl);
    }

    private void renderMediaPreview(ExtractedMedia media) {
        if (media == null) return;
        downloaderPreviewCard.setVisibility(View.VISIBLE);
        downloaderTitleView.setText(media.title);

        String meta = media.source.icon + " " + media.source.displayName;
        if (media.author != null && !media.author.isEmpty()) {
            meta += " • " + media.author;
        }
        if (media.durationSeconds > 0) {
            meta += " • " + media.getFormattedDuration();
        }
        downloaderMetaBadgeView.setText(meta);

        if (media.thumbnailUrl != null && !media.thumbnailUrl.isEmpty()) {
            downloaderThumbView.setVisibility(View.VISIBLE);
            AsyncThumbnailLoader.load(media.thumbnailUrl, downloaderThumbView);
        } else {
            downloaderThumbView.setVisibility(View.GONE);
        }

        downloaderFormatsContainer.removeAllViews();

        TextView fHeader = new TextView(this);
        fHeader.setText("SELECT DOWNLOAD FORMAT & QUALITY:");
        fHeader.setTextSize(10);
        fHeader.setTypeface(Typeface.DEFAULT_BOLD);
        fHeader.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#475569"));
        fHeader.setPadding(0, dp(4), 0, dp(6));
        downloaderFormatsContainer.addView(fHeader);

        for (StreamFormat opt : media.streamOptions) {
            downloaderFormatsContainer.addView(createFormatOptionRow(media, opt));
        }
    }

    private View createFormatOptionRow(ExtractedMedia media, StreamFormat opt) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(4), 0, 0);
        row.setLayoutParams(rlp);

        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(isDarkTheme ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F8FAFC"));
        rBg.setCornerRadius(dp(8));
        rBg.setStroke(dp(1), isDarkTheme ? DesignTokens.getBorderSubtle(true) : Color.parseColor("#E2E8F0"));
        row.setBackground(rBg);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(tcLp);

        TextView label = new TextView(this);
        label.setText(opt.resolution + " (" + opt.extension.toUpperCase() + ")");
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        textCol.addView(label);

        TextView sub = new TextView(this);
        String subText = opt.requiresMuxing ? "Native Hardware Muxed • Zero Quality Loss" : (opt.isAudioOnly ? "Direct Audio Stream" : "Direct Stream");
        if (opt.approximateBytes > 0) {
            subText += " • " + opt.getFormattedSize();
        }
        sub.setText(subText);
        sub.setTextSize(9);
        sub.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
        textCol.addView(sub);
        row.addView(textCol);

        PremiumButton dlBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        dlBtn.setText("📥 Download");
        dlBtn.setTextSize(11);
        dlBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_INDIGO_DARK : DesignTokens.ACCENT_INDIGO_PRIMARY);
        LinearLayout.LayoutParams dlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        dlBtn.setLayoutParams(dlLp);
        dlBtn.setOnClickListener(v -> startDownloadFromOption(media, opt));
        row.addView(dlBtn);

        return row;
    }

    private void startDownloadFromOption(ExtractedMedia media, StreamFormat opt) {
        String subDir = "Movies/Media";
        if (opt.isAudioOnly) {
            subDir = "Music/Media";
        } else if ("jpg".equalsIgnoreCase(opt.extension) || "png".equalsIgnoreCase(opt.extension)) {
            subDir = "Pictures/Media";
        }

        String jobId = "dl_" + System.currentTimeMillis();
        DownloadJob job = new DownloadJob(
                jobId,
                media.title,
                media.source,
                opt,
                subDir
        );

        MediaDownloadManager.getInstance(this).enqueueJob(job);
        Toast.makeText(this, "🚀 Download started: " + media.title, Toast.LENGTH_SHORT).show();
        renderDownloadQueue();
    }

    private void refreshWhatsAppStatusesUI() {
        if (whatsappStatusContainer == null) return;
        whatsappStatusContainer.removeAllViews();

        String persistedUriStr = getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                .getString("saf_whatsapp_uri", null);
        Uri treeUri = (persistedUriStr != null) ? Uri.parse(persistedUriStr) : null;

        List<WhatsAppStatusHarvester.StatusItem> items = WhatsAppStatusHarvester.scanStatuses(this, treeUri);

        if (whatsappStatusCountBadge != null) {
            whatsappStatusCountBadge.setText(items.size() + " Cached");
        }

        if (items.isEmpty()) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setPadding(dp(12), dp(8), dp(12), dp(8));

            TextView msg = new TextView(this);
            msg.setText("No cached WhatsApp statuses found in standard paths. If using Android 11–16, grant SAF access to the .Statuses folder.");
            msg.setTextSize(10);
            msg.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            emptyBox.addView(msg);

            PremiumButton grantBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
            grantBtn.setText("📂 Grant WhatsApp SAF Access");
            grantBtn.setTextSize(11);
            LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            gbLp.setMargins(0, dp(6), 0, 0);
            grantBtn.setLayoutParams(gbLp);
            grantBtn.setOnClickListener(v -> {
                Intent picker = WhatsAppStatusHarvester.createSafFolderPickerIntent();
                startActivityForResult(picker, 707);
            });
            emptyBox.addView(grantBtn);

            whatsappStatusContainer.addView(emptyBox);
            return;
        }

        for (WhatsAppStatusHarvester.StatusItem item : items) {
            whatsappStatusContainer.addView(createWhatsAppStatusCard(item));
        }
    }

    private View createWhatsAppStatusCard(WhatsAppStatusHarvester.StatusItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, dp(8), 0);
        card.setLayoutParams(clp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDarkTheme ? DesignTokens.getSurfaceCard(true) : Color.WHITE);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), isDarkTheme ? 0xFF2E4035 : Color.parseColor("#DCFCE7"));
        card.setBackground(bg);

        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100));
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(isDarkTheme ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F1F5F9"));
        if (Build.VERSION.SDK_INT >= 21) iv.setClipToOutline(true);

        if (item.file != null) {
            iv.setImageURI(Uri.fromFile(item.file));
        } else if (item.uri != null) {
            iv.setImageURI(item.uri);
        }
        card.addView(iv);

        TextView tag = new TextView(this);
        tag.setText(item.isVideo ? "🎥 Video" : "📸 Photo");
        tag.setTextSize(9);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        tag.setTextColor(isDarkTheme ? 0xFF4ADE80 : Color.parseColor("#15803D"));
        tag.setPadding(0, dp(4), 0, 0);
        card.addView(tag);

        TextView size = new TextView(this);
        size.setText(item.getFormattedSize());
        size.setTextSize(8);
        size.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
        card.addView(size);

        PremiumButton saveBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
        saveBtn.setText("💾 Save");
        saveBtn.setTextSize(10);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        sbLp.setMargins(0, dp(4), 0, 0);
        saveBtn.setLayoutParams(sbLp);
        saveBtn.setOnClickListener(v -> {
            try {
                Uri saved = WhatsAppStatusHarvester.saveStatusToGallery(this, item);
                Toast.makeText(this, "🎉 Saved to Gallery: " + item.name, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(saveBtn);

        return card;
    }

    private void renderDownloadQueue() {
        if (downloaderQueueContainer == null) return;
        downloaderQueueContainer.removeAllViews();

        List<DownloadJob> jobs = MediaDownloadManager.getInstance(this).getJobs();

        int activeCount = 0;
        for (DownloadJob j : jobs) {
            if (j.state == DownloadJob.State.DOWNLOADING || j.state == DownloadJob.State.MUXING || j.state == DownloadJob.State.QUEUED) {
                activeCount++;
            }
        }
        if (downloaderQueueBadge != null) {
            downloaderQueueBadge.setText(activeCount + " Active • " + jobs.size() + " Total");
        }

        if (jobs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No active or recent downloads. Paste a link above to begin.");
            empty.setTextSize(10);
            empty.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#94A3B8"));
            empty.setPadding(0, dp(8), 0, 0);
            downloaderQueueContainer.addView(empty);
            return;
        }

        for (DownloadJob job : jobs) {
            downloaderQueueContainer.addView(createTaskQueueRow(job));
        }
    }

    private View createTaskQueueRow(DownloadJob job) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(6), 0, 0);
        row.setLayoutParams(rlp);

        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(isDarkTheme ? DesignTokens.getSurfaceCard(true) : Color.parseColor("#F8FAFC"));
        rBg.setCornerRadius(dp(8));
        rBg.setStroke(dp(1), isDarkTheme ? DesignTokens.getBorderSubtle(true) : Color.parseColor("#E2E8F0"));
        row.setBackground(rBg);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(job.source.icon + " " + job.title);
        title.setTextSize(11);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        title.setSingleLine(true);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        title.setLayoutParams(tLp);
        topRow.addView(title);

        TextView stateBadge = new TextView(this);
        stateBadge.setTextSize(9);
        stateBadge.setTypeface(Typeface.DEFAULT_BOLD);
        stateBadge.setPadding(dp(6), dp(2), dp(6), dp(2));

        if (job.state == DownloadJob.State.COMPLETED) {
            stateBadge.setText("✅ COMPLETED");
            stateBadge.setTextColor(isDarkTheme ? 0xFF4ADE80 : Color.parseColor("#15803D"));
            stateBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF064E3B : Color.parseColor("#DCFCE7"), dp(4)));
        } else if (job.state == DownloadJob.State.MUXING) {
            stateBadge.setText("⚙️ MUXING MP4");
            stateBadge.setTextColor(isDarkTheme ? 0xFFC4B5FD : Color.parseColor("#7C3AED"));
            stateBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF3B1D76 : Color.parseColor("#F5F3FF"), dp(4)));
        } else if (job.state == DownloadJob.State.DOWNLOADING) {
            stateBadge.setText("⬇️ " + job.getProgressPercent() + "%");
            stateBadge.setTextColor(isDarkTheme ? 0xFF60A5FA : Color.parseColor("#2563EB"));
            stateBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF1E3A8A : Color.parseColor("#DBEAFE"), dp(4)));
        } else if (job.state == DownloadJob.State.PAUSED) {
            stateBadge.setText("⏸ PAUSED");
            stateBadge.setTextColor(isDarkTheme ? 0xFFFCD34D : Color.parseColor("#B45309"));
            stateBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF78350F : Color.parseColor("#FEF3C7"), dp(4)));
        } else if (job.state == DownloadJob.State.ERROR) {
            stateBadge.setText("❌ ERROR");
            stateBadge.setTextColor(isDarkTheme ? 0xFFF87171 : Color.parseColor("#B91C1C"));
            stateBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF7F1D1D : Color.parseColor("#FEE2E2"), dp(4)));
        } else {
            stateBadge.setText("⏳ " + job.state.name());
            stateBadge.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            stateBadge.setBackground(createPillDrawable(isDarkTheme ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9"), dp(4)));
        }
        topRow.addView(stateBadge);
        row.addView(topRow);

        if (job.state == DownloadJob.State.DOWNLOADING || job.state == DownloadJob.State.MUXING) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(job.getProgressPercent());
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
            pbLp.setMargins(0, dp(6), 0, 0);
            pb.setLayoutParams(pbLp);
            row.addView(pb);

            TextView speed = new TextView(this);
            speed.setText(job.getFormattedProgressBytes() + (job.getFormattedSpeed().isEmpty() ? "" : (" • " + job.getFormattedSpeed())));
            speed.setTextSize(9);
            speed.setTextColor(isDarkTheme ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            speed.setPadding(0, dp(2), 0, 0);
            row.addView(speed);
        }

        if (job.state == DownloadJob.State.ERROR && job.errorMessage != null) {
            TextView err = new TextView(this);
            err.setText(job.errorMessage);
            err.setTextSize(9);
            err.setTextColor(Color.parseColor("#DC2626"));
            err.setPadding(0, dp(2), 0, 0);
            row.addView(err);
        }

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.RIGHT);
        actionsRow.setPadding(0, dp(6), 0, 0);

        if (job.state == DownloadJob.State.DOWNLOADING) {
            PremiumButton pauseBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            pauseBtn.setText("⏸ Pause");
            pauseBtn.setTextSize(10);
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            pauseBtn.setLayoutParams(pbLp);
            pauseBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).pauseJob(job.id));
            actionsRow.addView(pauseBtn);

            PremiumButton cancelBtn = new PremiumButton(this, PremiumButton.Style.DESTRUCTIVE, isDarkTheme);
            cancelBtn.setText("✖ Cancel");
            cancelBtn.setTextSize(10);
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            cbLp.setMargins(dp(6), 0, 0, 0);
            cancelBtn.setLayoutParams(cbLp);
            cancelBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).cancelJob(job.id));
            actionsRow.addView(cancelBtn);
        } else if (job.state == DownloadJob.State.PAUSED) {
            PremiumButton resumeBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
            resumeBtn.setText("▶ Resume");
            resumeBtn.setTextSize(10);
            LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            resumeBtn.setLayoutParams(rbLp);
            resumeBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).resumeJob(job.id));
            actionsRow.addView(resumeBtn);

            PremiumButton dismissBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            dismissBtn.setText("✕ Dismiss");
            dismissBtn.setTextSize(10);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            dbLp.setMargins(dp(6), 0, 0, 0);
            dismissBtn.setLayoutParams(dbLp);
            dismissBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).dismissJob(job.id));
            actionsRow.addView(dismissBtn);
        } else if (job.state == DownloadJob.State.COMPLETED) {
            PremiumButton openBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
            openBtn.setText("📂 Open Media");
            openBtn.setTextSize(10);
            LinearLayout.LayoutParams obLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            openBtn.setLayoutParams(obLp);
            openBtn.setOnClickListener(v -> openDownloadedMedia(job));
            actionsRow.addView(openBtn);

            PremiumButton dismissBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            dismissBtn.setText("✕ Dismiss");
            dismissBtn.setTextSize(10);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            dbLp.setMargins(dp(6), 0, 0, 0);
            dismissBtn.setLayoutParams(dbLp);
            dismissBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).dismissJob(job.id));
            actionsRow.addView(dismissBtn);
        } else {
            PremiumButton dismissBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            dismissBtn.setText("✕ Dismiss");
            dismissBtn.setTextSize(10);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            dismissBtn.setLayoutParams(dbLp);
            dismissBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).dismissJob(job.id));
            actionsRow.addView(dismissBtn);
        }

        row.addView(actionsRow);
        return row;
    }

    private void openDownloadedMedia(DownloadJob job) {
        if (job.savedMediaUri != null) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                String mime = job.format.isAudioOnly ? "audio/*" : "video/*";
                if ("jpg".equalsIgnoreCase(job.format.extension) || "png".equalsIgnoreCase(job.format.extension)) {
                    mime = "image/*";
                }
                viewIntent.setDataAndType(job.savedMediaUri, mime);
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(viewIntent);
                return;
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, "Media saved to System Gallery (" + job.destinationSubdir + ")", Toast.LENGTH_SHORT).show();
    }

    private void handleIncomingShareIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                String extractedUrl = MediaResolverRegistry.extractUrl(sharedText);
                if (extractedUrl != null && !extractedUrl.isEmpty()) {
                    openFeature(FeatureRegistry.ID_MEDIA);
                    if (downloaderUrlInput != null) {
                        downloaderUrlInput.setText(extractedUrl);
                    }
                    triggerAnalyzeUrl(extractedUrl);
                }
            }
        }
    }

    // ==========================================
    // 7. UI STYLING HELPERS
    // ==========================================
    private LinearLayout createCardLayout(int bgColor, int strokeColor) {
        PremiumCard layout = new PremiumCard(this, isDarkTheme);
        layout.setOrientation(LinearLayout.VERTICAL);

        int finalBg;
        int finalStroke;
        if (isDarkTheme) {
            if (bgColor == Color.WHITE || bgColor == 0xFFFFFFFF) {
                finalBg = Color.argb(235, 20, 25, 35);
            } else {
                int r = (bgColor >> 16) & 0xFF;
                int g = (bgColor >> 8) & 0xFF;
                int b = bgColor & 0xFF;
                finalBg = Color.argb(235, Math.min(255, 18 + r / 8), Math.min(255, 22 + g / 8), Math.min(255, 32 + b / 8));
            }
            if (strokeColor != Color.WHITE && strokeColor != 0xFFFFFFFF && strokeColor != Color.parseColor("#E2E8F0") && strokeColor != ThemeManager.BORDER_DARK) {
                int sr = (strokeColor >> 16) & 0xFF;
                int sg = (strokeColor >> 8) & 0xFF;
                int sb = strokeColor & 0xFF;
                finalStroke = Color.argb(90, sr, sg, sb);
            } else {
                finalStroke = Color.argb(38, 255, 255, 255);
            }
        } else {
            if (bgColor == Color.WHITE || bgColor == 0xFFFFFFFF) {
                finalBg = Color.argb(218, 255, 255, 255);
            } else {
                int r = (bgColor >> 16) & 0xFF;
                int g = (bgColor >> 8) & 0xFF;
                int b = bgColor & 0xFF;
                finalBg = Color.argb(220, r, g, b);
            }
            finalStroke = Color.argb(70, 0, 0, 0);
        }

        layout.setCustomColors(finalBg, finalStroke);
        layout.setCornerRadiusDp(DesignTokens.RADIUS_LG);
        return layout;
    }

    private GradientDrawable createPillDrawable(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private Button createIosGlossyButton(String text, int topColor, int midColor, int bottomColor, int strokeColor) {
        PremiumButton btn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        btn.setText(text);
        btn.setCustomAccent(midColor);
        return btn;
    }

    private Button createStyledButton(String text, int color) {
        PremiumButton btn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        btn.setText(text);
        btn.setCustomAccent(color);
        return btn;
    }

    private View createCalculatorContent() {
        if (calculatorCardController == null) {
            calculatorCardController = new com.devendra.healthtracker.calculator.CalculatorCardController(this);
        }
        return calculatorCardController.createView();
    }

    private void updateHomeCalendarNote() {
        if (homeCalendarNoteView == null) return;
        String note = CalendarManager.getTodayNote(this);
        if (note != null && !note.trim().isEmpty()) {
            homeCalendarNoteView.setText("📌 " + note.trim());
        } else {
            homeCalendarNoteView.setText("📅 Tap to write today's note");
        }
    }

    // ==========================================
    // CALENDAR & SPECIAL NOTES SUBSYSTEM
    // ==========================================
    private View createCalendarContent() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, dp(32));

        boolean isDark = isDarkTheme;

        // 1. Month Navigation Card
        LinearLayout monthCard = createCardLayout(
            isDark ? ThemeManager.CARD_BG_DARK : Color.WHITE,
            isDark ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
        );
        monthCard.setOrientation(LinearLayout.VERTICAL);
        monthCard.setPadding(dp(14), dp(12), dp(14), dp(14));
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mcLp.setMargins(0, 0, 0, dp(14));
        monthCard.setLayoutParams(mcLp);

        // Header: Prev, Month YYYY, Next, Today
        LinearLayout monthNavRow = new LinearLayout(this);
        monthNavRow.setOrientation(LinearLayout.HORIZONTAL);
        monthNavRow.setGravity(Gravity.CENTER_VERTICAL);

        PremiumIconButton prevBtn = new PremiumIconButton(this, "◀", isDark);
        prevBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        prevBtn.setOnClickListener(v -> {
            calDisplayMonth.add(Calendar.MONTH, -1);
            refreshCalendarUI();
        });
        monthNavRow.addView(prevBtn);

        calMonthHeaderView = new TextView(this);
        calMonthHeaderView.setTextSize(15);
        calMonthHeaderView.setTypeface(Typeface.DEFAULT_BOLD);
        calMonthHeaderView.setTextColor(ThemeManager.getTextBrightColor(isDark));
        calMonthHeaderView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mhLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        calMonthHeaderView.setLayoutParams(mhLp);
        monthNavRow.addView(calMonthHeaderView);

        PremiumIconButton nextBtn = new PremiumIconButton(this, "▶", isDark);
        nextBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        nextBtn.setOnClickListener(v -> {
            calDisplayMonth.add(Calendar.MONTH, 1);
            refreshCalendarUI();
        });
        monthNavRow.addView(nextBtn);

        PremiumButton todayBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDark);
        todayBtn.setText("Today");
        todayBtn.setTextSize(11);
        todayBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        tbLp.setMargins(dp(8), 0, 0, 0);
        todayBtn.setLayoutParams(tbLp);
        todayBtn.setOnClickListener(v -> {
            calDisplayMonth = Calendar.getInstance();
            calSelectedDateKey = CalendarManager.getTodayKey();
            refreshCalendarUI();
        });
        monthNavRow.addView(todayBtn);

        monthCard.addView(monthNavRow);

        // Day of week labels: S M T W T F S
        LinearLayout dowRow = new LinearLayout(this);
        dowRow.setOrientation(LinearLayout.HORIZONTAL);
        dowRow.setPadding(0, dp(12), 0, dp(6));
        String[] dowLabels = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        for (String dow : dowLabels) {
            TextView dowTv = new TextView(this);
            dowTv.setText(dow);
            dowTv.setTextSize(10);
            dowTv.setTypeface(Typeface.DEFAULT_BOLD);
            dowTv.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
            dowTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            dowTv.setLayoutParams(dLp);
            dowRow.addView(dowTv);
        }
        monthCard.addView(dowRow);

        // Grid container for days
        calGridContainer = new LinearLayout(this);
        calGridContainer.setOrientation(LinearLayout.VERTICAL);
        monthCard.addView(calGridContainer);

        layout.addView(monthCard);

        // 2. Note Editor Card for Selected Date
        LinearLayout noteEditorCard = createCardLayout(
            isDark ? ThemeManager.CARD_BG_DARK : Color.WHITE,
            isDark ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
        );
        noteEditorCard.setOrientation(LinearLayout.VERTICAL);
        noteEditorCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams necLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        necLp.setMargins(0, 0, 0, dp(14));
        noteEditorCard.setLayoutParams(necLp);

        calDateHeaderView = new TextView(this);
        calDateHeaderView.setTextSize(14);
        calDateHeaderView.setTypeface(Typeface.DEFAULT_BOLD);
        calDateHeaderView.setTextColor(ThemeManager.getTextBrightColor(isDark));
        calDateHeaderView.setPadding(0, 0, 0, dp(6));
        noteEditorCard.addView(calDateHeaderView);

        calCurrentNoteSnippet = new TextView(this);
        calCurrentNoteSnippet.setTextSize(12);
        calCurrentNoteSnippet.setTextColor(isDark ? Color.parseColor("#38BDF8") : Color.parseColor("#0284C7"));
        calCurrentNoteSnippet.setPadding(0, 0, 0, dp(10));
        noteEditorCard.addView(calCurrentNoteSnippet);

        calNoteInput = new EditText(this);
        calNoteInput.setHint("Write something special for this date (stored permanently across reboots)...");
        calNoteInput.setHintTextColor(isDark ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8"));
        calNoteInput.setTextColor(ThemeManager.getTextBrightColor(isDark));
        calNoteInput.setTextSize(13);
        calNoteInput.setBackground(createOutlinedDrawable(
            isDark ? Color.parseColor("#0F172A") : Color.parseColor("#F8FAFC"),
            isDark ? ThemeManager.BORDER_DARK : Color.parseColor("#CBD5E1"),
            dp(12)
        ));
        calNoteInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        calNoteInput.setMinLines(3);
        calNoteInput.setGravity(Gravity.TOP | Gravity.START);
        LinearLayout.LayoutParams cniLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cniLp.setMargins(0, 0, 0, dp(12));
        calNoteInput.setLayoutParams(cniLp);
        noteEditorCard.addView(calNoteInput);

        LinearLayout noteBtnsRow = new LinearLayout(this);
        noteBtnsRow.setOrientation(LinearLayout.HORIZONTAL);
        noteBtnsRow.setGravity(Gravity.CENTER_VERTICAL);

        PremiumButton saveNoteBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDark);
        saveNoteBtn.setText("💾 Save Note");
        saveNoteBtn.setTextSize(12);
        saveNoteBtn.setPadding(dp(16), dp(8), dp(16), dp(8));
        saveNoteBtn.setOnClickListener(v -> {
            String noteText = calNoteInput.getText().toString();
            CalendarManager.saveNote(this, calSelectedDateKey, noteText);
            Toast.makeText(this, "✅ Saved note for " + CalendarManager.formatDisplayDate(calSelectedDateKey), Toast.LENGTH_SHORT).show();
            refreshCalendarUI();
            updateHomeCalendarNote();
        });
        noteBtnsRow.addView(saveNoteBtn);

        PremiumButton clearNoteBtn = new PremiumButton(this, PremiumButton.Style.DESTRUCTIVE, isDark);
        clearNoteBtn.setText("🗑️ Clear");
        clearNoteBtn.setTextSize(12);
        clearNoteBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams clrLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clrLp.setMargins(dp(10), 0, 0, 0);
        clearNoteBtn.setLayoutParams(clrLp);
        clearNoteBtn.setOnClickListener(v -> {
            CalendarManager.deleteNote(this, calSelectedDateKey);
            calNoteInput.setText("");
            Toast.makeText(this, "Cleared note for " + CalendarManager.formatDisplayDate(calSelectedDateKey), Toast.LENGTH_SHORT).show();
            refreshCalendarUI();
            updateHomeCalendarNote();
        });
        noteBtnsRow.addView(clearNoteBtn);

        noteEditorCard.addView(noteBtnsRow);

        layout.addView(noteEditorCard);

        // 3. Saved Special Notes Section
        TextView savedHeader = new TextView(this);
        savedHeader.setText("ALL SPECIAL NOTES & MEMORANDA");
        savedHeader.setTextSize(11);
        savedHeader.setTypeface(Typeface.DEFAULT_BOLD);
        savedHeader.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        savedHeader.setPadding(dp(4), dp(4), dp(4), dp(8));
        layout.addView(savedHeader);

        calNotesListContainer = new LinearLayout(this);
        calNotesListContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(calNotesListContainer);

        refreshCalendarUI();

        return layout;
    }

    private void refreshCalendarUI() {
        if (calMonthHeaderView == null || calGridContainer == null) return;

        boolean isDark = isDarkTheme;

        // Month Header text
        String monthTitle = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calDisplayMonth.getTime());
        calMonthHeaderView.setText(monthTitle);

        // Selected Date Header & Input Text
        if (calDateHeaderView != null) {
            calDateHeaderView.setText("📅 " + CalendarManager.formatDisplayDate(calSelectedDateKey));
        }
        String currentNote = CalendarManager.getNote(this, calSelectedDateKey);
        if (calNoteInput != null && !calNoteInput.hasFocus()) {
            calNoteInput.setText(currentNote);
        }
        if (calCurrentNoteSnippet != null) {
            if (!currentNote.isEmpty()) {
                calCurrentNoteSnippet.setText("Active note: \"" + currentNote + "\"");
                calCurrentNoteSnippet.setVisibility(View.VISIBLE);
            } else {
                calCurrentNoteSnippet.setText("No special note saved for this date yet.");
                calCurrentNoteSnippet.setVisibility(View.VISIBLE);
            }
        }

        // Rebuild Calendar Days Grid
        calGridContainer.removeAllViews();

        Calendar monthCal = (Calendar) calDisplayMonth.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int year = monthCal.get(Calendar.YEAR);
        int month = monthCal.get(Calendar.MONTH) + 1; // 1-indexed

        Set<String> datesWithNotes = CalendarManager.getDatesWithNotes(this);
        String todayKey = CalendarManager.getTodayKey();

        int currentDay = 1;
        int totalCells = firstDayOfWeek + daysInMonth;
        int numRows = (int) Math.ceil(totalCells / 7.0);

        for (int r = 0; r < numRows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(3), 0, dp(3));

            for (int c = 0; c < 7; c++) {
                int cellIndex = r * 7 + c;
                if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                    // Empty cell
                    View empty = new View(this);
                    LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
                    eLp.setMargins(dp(2), dp(2), dp(2), dp(2));
                    empty.setLayoutParams(eLp);
                    row.addView(empty);
                } else {
                    int dayNum = currentDay;
                    String cellKey = CalendarManager.formatDateKey(year, month, dayNum);
                    boolean isSelected = cellKey.equals(calSelectedDateKey);
                    boolean isToday = cellKey.equals(todayKey);
                    boolean hasNote = datesWithNotes.contains(cellKey);

                    LinearLayout cell = new LinearLayout(this);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
                    cLp.setMargins(dp(2), dp(2), dp(2), dp(2));
                    cell.setLayoutParams(cLp);

                    // Cell Background
                    GradientDrawable bg = new GradientDrawable();
                    if (isSelected) {
                        bg.setColor(isDark ? Color.parseColor("#7C3AED") : Color.parseColor("#8B5CF6"));
                        bg.setCornerRadius(dp(12));
                    } else if (hasNote) {
                        bg.setColor(isDark ? Color.argb(160, 244, 63, 94) : Color.argb(60, 244, 63, 94));
                        bg.setCornerRadius(dp(12));
                        bg.setStroke(dp(1), Color.parseColor("#F43F5E"));
                    } else if (isToday) {
                        bg.setColor(isDark ? Color.argb(100, 30, 41, 59) : Color.argb(80, 226, 232, 240));
                        bg.setCornerRadius(dp(12));
                        bg.setStroke(dp(1), isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
                    } else {
                        bg.setColor(Color.TRANSPARENT);
                        bg.setCornerRadius(dp(12));
                    }
                    cell.setBackground(bg);

                    TextView dayTv = new TextView(this);
                    dayTv.setText(String.valueOf(dayNum));
                    dayTv.setTextSize(13);
                    dayTv.setTypeface(isToday || isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    if (isSelected) {
                        dayTv.setTextColor(Color.WHITE);
                    } else if (hasNote) {
                        dayTv.setTextColor(isDark ? Color.parseColor("#FDA4AF") : Color.parseColor("#BE123C"));
                        dayTv.setTypeface(Typeface.DEFAULT_BOLD);
                    } else if (isToday) {
                        dayTv.setTextColor(isDark ? Color.parseColor("#38BDF8") : Color.parseColor("#0284C7"));
                    } else {
                        dayTv.setTextColor(isDark ? ThemeManager.getTextBrightColor(true) : ThemeManager.getTextBrightColor(false));
                    }
                    cell.addView(dayTv);

                    if (hasNote) {
                        TextView dot = new TextView(this);
                        dot.setText("📌");
                        dot.setTextSize(8);
                        cell.addView(dot);
                    }

                    cell.setOnClickListener(v -> {
                        calSelectedDateKey = cellKey;
                        if (calNoteInput != null) calNoteInput.clearFocus();
                        refreshCalendarUI();
                    });

                    row.addView(cell);
                    currentDay++;
                }
            }
            calGridContainer.addView(row);
        }

        // Rebuild Saved Notes List
        if (calNotesListContainer != null) {
            calNotesListContainer.removeAllViews();
            Map<String, String> allNotes = CalendarManager.getAllNotes(this);
            if (allNotes.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No saved special notes yet. Tap any date above to add one!");
                empty.setTextSize(12);
                empty.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
                empty.setPadding(dp(8), dp(10), dp(8), dp(10));
                calNotesListContainer.addView(empty);
            } else {
                List<String> sortedKeys = new ArrayList<>(allNotes.keySet());
                Collections.sort(sortedKeys);
                for (String dateKey : sortedKeys) {
                    String noteText = allNotes.get(dateKey);

                    LinearLayout noteRow = createCardLayout(
                        isDark ? ThemeManager.CARD_BG_DARK : Color.WHITE,
                        isDark ? ThemeManager.BORDER_DARK : Color.parseColor("#E2E8F0")
                    );
                    noteRow.setOrientation(LinearLayout.HORIZONTAL);
                    noteRow.setGravity(Gravity.CENTER_VERTICAL);
                    noteRow.setPadding(dp(14), dp(12), dp(14), dp(12));
                    LinearLayout.LayoutParams nrlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    nrlp.setMargins(0, 0, 0, dp(8));
                    noteRow.setLayoutParams(nrlp);

                    LinearLayout textCol = new LinearLayout(this);
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    textCol.setLayoutParams(tcLp);

                    TextView dateLabel = new TextView(this);
                    dateLabel.setText("📅 " + CalendarManager.formatDisplayDate(dateKey));
                    dateLabel.setTextSize(11);
                    dateLabel.setTypeface(Typeface.DEFAULT_BOLD);
                    dateLabel.setTextColor(isDark ? Color.parseColor("#A78BFA") : Color.parseColor("#7C3AED"));
                    textCol.addView(dateLabel);

                    TextView memoText = new TextView(this);
                    memoText.setText(noteText);
                    memoText.setTextSize(13);
                    memoText.setTextColor(ThemeManager.getTextBrightColor(isDark));
                    memoText.setPadding(0, dp(3), 0, 0);
                    textCol.addView(memoText);

                    noteRow.addView(textCol);

                    PremiumIconButton delBtn = new PremiumIconButton(this, "✕", isDark);
                    delBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
                    delBtn.setOnClickListener(v -> {
                        CalendarManager.deleteNote(this, dateKey);
                        refreshCalendarUI();
                        updateHomeCalendarNote();
                    });
                    noteRow.addView(delBtn);

                    TouchFeedback.attach(noteRow, v -> {
                        calSelectedDateKey = dateKey;
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            Date d = sdf.parse(dateKey);
                            if (d != null) {
                                calDisplayMonth.setTime(d);
                            }
                        } catch (Exception ignored) {}
                        refreshCalendarUI();
                    });

                    calNotesListContainer.addView(noteRow);
                }
            }
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
