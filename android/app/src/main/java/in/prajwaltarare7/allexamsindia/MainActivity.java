package in.prajwaltarare7.allexamsindia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final String HOME_URL =
            "https://prajwaltarare7-oss.github.io/all-exam-india/";
    private static final String PREFS = "all_exam_india_native";
    private static final int NAVY = Color.rgb(16, 27, 53);
    private static final int BLUE = Color.rgb(49, 94, 232);
    private static final int LIGHT_BG = Color.rgb(245, 247, 252);
    private static final int INK = Color.rgb(23, 35, 61);
    private static final int MUTED = Color.rgb(112, 124, 145);

    private final String[] navPages = {"home", "pyq", "mock", "currentaffairs", "settings"};
    private final String[] navLabels = {"Home", "PYQ", "Mock", "Current", "More"};
    private final String[] navIcons = {"⌂", "▤", "◎", "▣", "☰"};
    private final List<String> sscExams = Arrays.asList(
            "SSC CGL", "SSC CHSL", "SSC MTS", "SSC GD", "SSC CPO",
            "SSC Stenographer", "SSC JE", "SSC JHT", "SSC Selection Post",
            "SSC Scientific Assistant");

    private SharedPreferences preferences;
    private FrameLayout root;
    private FrameLayout contentContainer;
    private WebView webView;
    private ProgressBar progressBar;
    private View errorView;
    private LinearLayout bottomNav;
    private final TextView[] navItems = new TextView[5];
    private String activePage = "home";
    private String pendingWebPage = "mock";
    private String pendingExam = "";
    private boolean webLoaded = false;
    private boolean showingError = false;
    private boolean darkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        darkMode = preferences.getBoolean("dark_mode", false);
        buildAppShell();
        showNativePage("home");
    }

    private void buildAppShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(pageBackground());
        setContentView(root);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(pageBackground());

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(16), dp(6), dp(14), dp(6));
        GradientDrawable toolbarBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{NAVY, Color.rgb(25, 55, 102)});
        toolbarBg.setCornerRadius(0);
        toolbar.setBackground(toolbarBg);

        LinearLayout brandStack = new LinearLayout(this);
        brandStack.setOrientation(LinearLayout.VERTICAL);
        brandStack.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = new TextView(this);
        brand.setText("ALL EXAM INDIA");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(17);
        brand.setTypeface(null, Typeface.BOLD);
        brandStack.addView(brand, wrapParams());
        TextView subtitle = new TextView(this);
        subtitle.setText("SMART PREPARATION • BETTER RESULTS");
        subtitle.setTextColor(Color.rgb(194, 210, 239));
        subtitle.setTextSize(9);
        subtitle.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams subParams = wrapParams();
        subParams.topMargin = dp(2);
        brandStack.addView(subtitle, subParams);
        toolbar.addView(brandStack, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView profile = new TextView(this);
        profile.setText("👤");
        profile.setTextSize(20);
        profile.setGravity(Gravity.CENTER);
        profile.setTextColor(Color.WHITE);
        profile.setBackground(roundDrawable(Color.rgb(42, 70, 119), dp(18)));
        toolbar.addView(profile, new LinearLayout.LayoutParams(dp(42), dp(42)));
        profile.setOnClickListener(v -> showProfileDialog());

        shell.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        contentContainer = new FrameLayout(this);
        shell.addView(contentContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        bottomNav = buildBottomNavigation();
        shell.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));

        root.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateSystemBars();
    }

    private LinearLayout buildBottomNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(6), dp(5), dp(6), dp(5));
        bar.setBackgroundColor(darkMode ? Color.rgb(17, 27, 46) : Color.WHITE);
        bar.setElevation(dp(12));

        for (int i = 0; i < navPages.length; i++) {
            final String page = navPages[i];
            TextView item = new TextView(this);
            item.setText(navIcons[i] + "\n" + navLabels[i]);
            item.setGravity(Gravity.CENTER);
            item.setTextSize(10);
            item.setLineSpacing(0, 0.92f);
            item.setMaxLines(2);
            item.setPadding(dp(2), dp(3), dp(2), dp(3));
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(v -> navigateToPage(page));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(dp(3), 0, dp(3), 0);
            bar.addView(item, params);
            navItems[i] = item;
        }
        return bar;
    }

    private void navigateToPage(String page) {
        if ("home".equals(page) || "settings".equals(page)) {
            if ("testroom".equals(activePage) && webView != null) {
                webView.evaluateJavascript(
                        "!(document.querySelector('.page.active')&&" +
                        "document.querySelector('.page.active').id==='testroom'&&" +
                        "typeof activePractice!=='undefined'&&activePractice&&!practiceSubmitted)",
                        result -> {
                            if ("true".equals(result)) showNativePage(page);
                            else Toast.makeText(this,
                                    "चालू टेस्ट आधी Submit करा.", Toast.LENGTH_LONG).show();
                        });
            } else {
                showNativePage(page);
            }
        } else {
            showWebsiteSection(page);
        }
    }

    private void showNativePage(String page) {
        activePage = page;
        contentContainer.removeAllViews();
        if ("settings".equals(page)) {
            contentContainer.addView(buildSettingsPage(),
                    new FrameLayout.LayoutParams(-1, -1));
        } else {
            contentContainer.addView(buildHomePage(),
                    new FrameLayout.LayoutParams(-1, -1));
        }
        updateNavigation(page);
    }

    private View buildHomePage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(pageBackground());

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(17), dp(18), dp(17), dp(24));

        String name = preferences.getString("name", "").trim();
        TextView greeting = makeText(name.isEmpty() ? "नमस्कार! 👋" : "नमस्कार, " + name + " 👋",
                20, textPrimary(), true);
        body.addView(greeting, wrapParams());

        TextView intro = makeText("तुमच्या परीक्षेची तयारी आता एका ठिकाणी.", 13,
                textMuted(), false);
        LinearLayout.LayoutParams introParams = wrapParams();
        introParams.topMargin = dp(5);
        body.addView(intro, introParams);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(roundDrawable(
                darkMode ? Color.rgb(30, 53, 91) : Color.rgb(231, 239, 255), dp(20)));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.topMargin = dp(18);
        body.addView(hero, heroParams);

        TextView heroTitle = makeText("तयारीला द्या नवी दिशा!", 21,
                darkMode ? Color.WHITE : NAVY, true);
        hero.addView(heroTitle, wrapParams());
        TextView heroDesc = makeText(
                "Mock Test सोडवा, PYQ सराव करा आणि Current Affairs वाचा.",
                13, darkMode ? Color.rgb(218, 228, 247) : Color.rgb(69, 87, 119), false);
        LinearLayout.LayoutParams descParams = wrapParams();
        descParams.topMargin = dp(7);
        hero.addView(heroDesc, descParams);

        Button start = primaryButton("आता सराव सुरू करा");
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(47));
        startParams.topMargin = dp(14);
        hero.addView(start, startParams);
        start.setOnClickListener(v -> showWebsiteSection("mock"));

        TextView section = makeText("तुमच्यासाठी अभ्यास विभाग", 17, textPrimary(), true);
        LinearLayout.LayoutParams sectionParams = wrapParams();
        sectionParams.topMargin = dp(22);
        sectionParams.bottomMargin = dp(10);
        body.addView(section, sectionParams);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(actionCard("📝", "Mock Tests", "परीक्षेच्या पद्धतीने टेस्ट",
                Color.rgb(49, 94, 232), () -> showWebsiteSection("mock")),
                new LinearLayout.LayoutParams(0, dp(132), 1));
        View gap1 = new View(this);
        row1.addView(gap1, new LinearLayout.LayoutParams(dp(10), 1));
        row1.addView(actionCard("📚", "PYQ Practice", "मागील प्रश्नांचा सराव",
                Color.rgb(12, 139, 112), () -> showWebsiteSection("pyq")),
                new LinearLayout.LayoutParams(0, dp(132), 1));
        body.addView(row1, wrapParams());

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Params = wrapParams();
        row2Params.topMargin = dp(10);
        row2.addView(actionCard("📰", "Current Affairs", "चालू घडामोडी व MCQ",
                Color.rgb(193, 112, 24), () -> showWebsiteSection("currentaffairs")),
                new LinearLayout.LayoutParams(0, dp(132), 1));
        View gap2 = new View(this);
        row2.addView(gap2, new LinearLayout.LayoutParams(dp(10), 1));
        row2.addView(actionCard("⚙️", "App Settings", "Theme आणि Profile",
                Color.rgb(103, 77, 170), () -> showNativePage("settings")),
                new LinearLayout.LayoutParams(0, dp(132), 1));
        body.addView(row2, row2Params);

        TextView examHeading = makeText("SSC परीक्षांसाठी थेट जा", 17, textPrimary(), true);
        LinearLayout.LayoutParams examHeadingParams = wrapParams();
        examHeadingParams.topMargin = dp(23);
        examHeadingParams.bottomMargin = dp(9);
        body.addView(examHeading, examHeadingParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("परीक्षेचे नाव शोधा...");
        search.setTextColor(textPrimary());
        search.setHintTextColor(textMuted());
        search.setPadding(dp(13), dp(8), dp(13), dp(8));
        search.setBackground(roundDrawable(
                darkMode ? Color.rgb(27, 40, 63) : Color.WHITE, dp(12)));
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout examList = new LinearLayout(this);
        examList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = wrapParams();
        listParams.topMargin = dp(7);
        body.addView(examList, listParams);
        renderExamShortcuts(examList, "");

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                renderExamShortcuts(examList, s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });

        TextView sourceNote = makeText(
                "Mock Test, PYQ आणि Current Affairs चा मजकूर तुझ्या Live Website वरून घेतला जातो. नवीन अपडेटसाठी इंटरनेट आवश्यक आहे.",
                11, textMuted(), false);
        sourceNote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = wrapParams();
        noteParams.topMargin = dp(19);
        body.addView(sourceNote, noteParams);

        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private void renderExamShortcuts(LinearLayout host, String query) {
        host.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase();
        int count = 0;
        for (String exam : sscExams) {
            if (!exam.toLowerCase().contains(q)) continue;
            Button item = new Button(this);
            item.setText("🎯  " + exam + "     ›");
            item.setAllCaps(false);
            item.setTextSize(13);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setTextColor(textPrimary());
            item.setBackgroundTintList(ColorStateList.valueOf(
                    darkMode ? Color.rgb(27, 40, 63) : Color.WHITE));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(47));
            params.topMargin = dp(5);
            host.addView(item, params);
            item.setOnClickListener(v -> {
                pendingExam = exam;
                showWebsiteSection("mock");
            });
            count++;
        }
        if (count == 0) {
            TextView empty = makeText("या नावाची परीक्षा सापडली नाही.", 13, textMuted(), false);
            host.addView(empty, wrapParams());
        }
    }

    private View actionCard(String icon, String title, String subtitle,
                            int accent, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(12), dp(11), dp(12));
        card.setBackground(roundDrawable(
                darkMode ? Color.rgb(24, 37, 59) : Color.WHITE, dp(16)));
        card.setElevation(dp(2));
        TextView symbol = makeText(icon, 25, accent, true);
        card.addView(symbol, wrapParams());
        TextView heading = makeText(title, 14, textPrimary(), true);
        LinearLayout.LayoutParams headingParams = wrapParams();
        headingParams.topMargin = dp(5);
        card.addView(heading, headingParams);
        TextView detail = makeText(subtitle, 11, textMuted(), false);
        LinearLayout.LayoutParams detailParams = wrapParams();
        detailParams.topMargin = dp(2);
        card.addView(detail, detailParams);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> action.run());
        return card;
    }

    private View buildSettingsPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(pageBackground());
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(20), dp(18), dp(25));

        body.addView(makeText("App Settings", 22, textPrimary(), true), wrapParams());
        TextView sub = makeText("अॅप तुमच्या सोयीनुसार सेट करा.", 13, textMuted(), false);
        LinearLayout.LayoutParams subParams = wrapParams();
        subParams.topMargin = dp(4);
        body.addView(sub, subParams);

        LinearLayout themeCard = settingsCard();
        TextView themeTitle = makeText("🌙  Dark Mode", 16, textPrimary(), true);
        themeCard.addView(themeTitle, wrapParams());
        TextView themeDesc = makeText("अॅपचा Light किंवा Dark look निवडा.", 12, textMuted(), false);
        LinearLayout.LayoutParams td = wrapParams();
        td.topMargin = dp(4);
        themeCard.addView(themeDesc, td);
        Switch themeSwitch = new Switch(this);
        themeSwitch.setText("Dark theme");
        themeSwitch.setTextColor(textPrimary());
        themeSwitch.setChecked(darkMode);
        LinearLayout.LayoutParams sw = wrapParams();
        sw.topMargin = dp(8);
        themeCard.addView(themeSwitch, sw);
        themeSwitch.setOnCheckedChangeListener((button, checked) -> {
            darkMode = checked;
            preferences.edit().putBoolean("dark_mode", checked).apply();
            updateSystemBars();
            showNativePage("settings");
        });
        addSettingsCard(body, themeCard);

        LinearLayout profileCard = settingsCard();
        profileCard.addView(makeText("👤  Student Profile", 16, textPrimary(), true), wrapParams());
        TextView profileDesc = makeText(
                preferences.getString("name", "").isEmpty()
                        ? "तुमचे नाव सेट करा."
                        : "नाव: " + preferences.getString("name", ""),
                12, textMuted(), false);
        LinearLayout.LayoutParams pd = wrapParams();
        pd.topMargin = dp(4);
        profileCard.addView(profileDesc, pd);
        Button editProfile = secondaryButton("Edit Profile");
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(44));
        ep.topMargin = dp(10);
        profileCard.addView(editProfile, ep);
        editProfile.setOnClickListener(v -> showProfileDialog());
        addSettingsCard(body, profileCard);

        LinearLayout dataCard = settingsCard();
        dataCard.addView(makeText("🔄  Website Content Sync", 16, textPrimary(), true), wrapParams());
        TextView dataDesc = makeText(
                "Mock Test, PYQ आणि Current Affairs Live Website मधून उघडतात. नवीन मजकूर पाहण्यासाठी इंटरनेट कनेक्शन ठेवा.",
                12, textMuted(), false);
        LinearLayout.LayoutParams dd = wrapParams();
        dd.topMargin = dp(5);
        dataCard.addView(dataDesc, dd);
        Button refresh = secondaryButton("Refresh Website Content");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(44));
        rp.topMargin = dp(10);
        dataCard.addView(refresh, rp);
        refresh.setOnClickListener(v -> {
            if (webView != null) {
                webLoaded = false;
                webView.reload();
                Toast.makeText(this, "Website content refreshing...", Toast.LENGTH_SHORT).show();
            } else {
                showWebsiteSection("mock");
            }
        });
        addSettingsCard(body, dataCard);

        LinearLayout aboutCard = settingsCard();
        aboutCard.addView(makeText("ℹ️  About All Exam India", 16, textPrimary(), true), wrapParams());
        TextView about = makeText(
                "Native Android interface with website-powered exam content. Website design and data remain separate from this app.",
                12, textMuted(), false);
        LinearLayout.LayoutParams ap = wrapParams();
        ap.topMargin = dp(5);
        aboutCard.addView(about, ap);
        addSettingsCard(body, aboutCard);

        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private LinearLayout settingsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(roundDrawable(
                darkMode ? Color.rgb(24, 37, 59) : Color.WHITE, dp(15)));
        return card;
    }

    private void addSettingsCard(LinearLayout parent, View card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(13);
        parent.addView(card, params);
    }

    private void showProfileDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView label = makeText("तुमचे नाव", 13, textPrimary(), true);
        form.addView(label, wrapParams());
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(preferences.getString("name", ""));
        name.setHint("तुमचे नाव लिहा");
        name.setTextSize(15);
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(50)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Student Profile")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = name.getText().toString().trim();
                    if (value.isEmpty()) {
                        name.setError("नाव लिहा");
                        return;
                    }
                    preferences.edit().putString("name", value).apply();
                    dialog.dismiss();
                    if ("home".equals(activePage)) showNativePage("home");
                    else if ("settings".equals(activePage)) showNativePage("settings");
                    Toast.makeText(this, "Profile saved.", Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    private void showWebsiteSection(String page) {
        pendingWebPage = page;
        activePage = page;
        contentContainer.removeAllViews();
        initializeWebView();
        if (webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }
        contentContainer.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        if (progressBar == null) {
            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setProgressTintList(ColorStateList.valueOf(BLUE));
        }
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP);
        contentContainer.addView(progressBar, pp);
        progressBar.setVisibility(webLoaded ? View.GONE : View.VISIBLE);
        updateNavigation(page);

        if (!webLoaded && webView.getUrl() == null) {
            webView.loadUrl(HOME_URL);
        } else if (webLoaded) {
            showWebsitePage(page);
        }
    }

    private void initializeWebView() {
        if (webView != null) return;
        webView = new WebView(this);
        webView.setBackgroundColor(pageBackground());
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.addJavascriptInterface(new AndroidAppBridge(), "AndroidApp");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                showingError = false;
                if (errorView != null && contentContainer != null) {
                    contentContainer.removeView(errorView);
                    errorView = null;
                }
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webLoaded = true;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                applyNativeWebStyle();
                showWebsitePage(pendingWebPage);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) showConnectionError();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(MainActivity.this,
                        "No app is available to open this download.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showWebsitePage(String page) {
        if (webView == null || !webLoaded) return;
        String safePage = JSONObject.quote(page);
        String exam = JSONObject.quote(pendingExam);
        String script = "(function(){try{" +
                "if(typeof window.showPage==='function')window.showPage(" + safePage + ");" +
                "if(" + exam + "&&typeof window.selectMockExam==='function'){" +
                "window.selectMockExam(" + exam + ");}" +
                "if(window.AndroidApp)window.AndroidApp.onPageChanged(" + safePage + ");" +
                "}catch(e){console.log(e);}})();";
        webView.evaluateJavascript(script, null);
        pendingExam = "";
    }

    private void applyNativeWebStyle() {
        if (webView == null) return;
        String css =
                "body{background:" + (darkMode ? "#0c1425" : "#f5f7fc") +
                "!important;overscroll-behavior-y:contain}" +
                "body>header,body>nav,body>footer,.hero{display:none!important}" +
                "main.container{max-width:none!important;width:100%!important;margin:0!important;" +
                "padding:15px 13px 22px!important;min-height:100%!important}" +
                ".page.active{animation:rise .18s ease}" +
                ".container h2{font-size:22px;margin:5px 0 9px}" +
                ".container h2:after{width:34px;height:3px;margin-top:7px}" +
                ".home-actions{gap:11px;margin:7px 0 15px}" +
                ".home-action-card{min-height:185px;padding:18px;border-radius:17px}" +
                ".card,.question{border-radius:15px;box-shadow:0 5px 18px #18294b0a}" +
                ".search,select{border-radius:11px}" +
                (darkMode ? "body.dark,body.dark main.container{background:#0c1425!important}" : "");
        String script = "(function(){try{" +
                "var style=document.getElementById('aei-native-app-style');" +
                "if(!style){style=document.createElement('style');style.id='aei-native-app-style';" +
                "document.head.appendChild(style);}" +
                "style.textContent=" + JSONObject.quote(css) + ";" +
                "document.body.classList.toggle('dark'," + darkMode + ");" +
                "if(!window.__aeiNativePageHook&&typeof window.showPage==='function'){" +
                "var originalShowPage=window.showPage;" +
                "window.showPage=function(id,fromHistory){var result=originalShowPage.apply(this,arguments);" +
                "try{var a=document.querySelector('.page.active');" +
                "if(a&&window.AndroidApp)window.AndroidApp.onPageChanged(a.id);}catch(e){}" +
                "return result;};window.__aeiNativePageHook=true;}" +
                "}catch(e){}})();";
        webView.evaluateJavascript(script, null);
    }

    private class AndroidAppBridge {
        @JavascriptInterface
        public void onPageChanged(String page) {
            runOnUiThread(() -> {
                if (page != null && !page.isEmpty()) {
                    activePage = page;
                    updateNavigation(page);
                }
            });
        }
    }

    private void updateNavigation(String page) {
        if (page == null) return;
        String selectedPage = page.equals("testroom") ? "mock" : page;
        for (int i = 0; i < navItems.length; i++) {
            TextView item = navItems[i];
            if (item == null) continue;
            boolean selected = navPages[i].equals(selectedPage);
            item.setTextColor(selected ? BLUE : (darkMode
                    ? Color.rgb(172, 185, 207) : MUTED));
            item.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            item.setBackground(roundDrawable(selected
                    ? (darkMode ? Color.rgb(34, 54, 88) : Color.rgb(235, 241, 255))
                    : Color.TRANSPARENT, dp(12)));
        }
    }

    private void showConnectionError() {
        if (showingError || contentContainer == null) return;
        showingError = true;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(25), dp(25), dp(25), dp(25));
        panel.setBackgroundColor(NAVY);
        TextView message = makeText(
                "इंटरनेट कनेक्शन तपासा आणि पुन्हा प्रयत्न करा.\n\nAll Exam India",
                17, Color.WHITE, true);
        message.setGravity(Gravity.CENTER);
        panel.addView(message, wrapParams());
        Button retry = primaryButton("Try Again");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, dp(46));
        rp.topMargin = dp(15);
        panel.addView(retry, rp);
        retry.setOnClickListener(v -> {
            showingError = false;
            if (errorView != null) contentContainer.removeView(errorView);
            errorView = null;
            webLoaded = false;
            webView.loadUrl(HOME_URL);
        });
        errorView = panel;
        contentContainer.addView(errorView, new FrameLayout.LayoutParams(-1, -1));
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private boolean handleUrl(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.equals("prajwaltarare7-oss.github.io")) return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open this link.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(ColorStateList.valueOf(BLUE));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(darkMode ? Color.WHITE : INK);
        button.setBackgroundTintList(ColorStateList.valueOf(
                darkMode ? Color.rgb(43, 62, 91) : Color.rgb(232, 237, 247)));
        return button;
    }

    private GradientDrawable roundDrawable(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private int pageBackground() {
        return darkMode ? Color.rgb(12, 20, 37) : LIGHT_BG;
    }

    private int textPrimary() {
        return darkMode ? Color.rgb(239, 243, 250) : INK;
    }

    private int textMuted() {
        return darkMode ? Color.rgb(166, 180, 204) : MUTED;
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void updateSystemBars() {
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(darkMode
                ? Color.rgb(17, 27, 46) : Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (bottomNav != null) {
            bottomNav.setBackgroundColor(darkMode
                    ? Color.rgb(17, 27, 46) : Color.WHITE);
        }
        if (webView != null) {
            webView.setBackgroundColor(pageBackground());
            applyNativeWebStyle();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.getParent() != null && webView.canGoBack()) {
            webView.goBack();
        } else if (!"home".equals(activePage)) {
            showNativePage("home");
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeJavascriptInterface("AndroidApp");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
