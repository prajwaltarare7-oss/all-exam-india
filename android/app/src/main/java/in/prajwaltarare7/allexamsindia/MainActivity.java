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
import android.text.InputFilter;
import android.text.InputType;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String HOME_URL =
            "https://prajwaltarare7-oss.github.io/all-exam-india/";
    private static final double MINIMUM_AGE = 17.5;
    private static final String PREFS = "all_exam_india_profile";
    private static final int NAVY = Color.rgb(16, 27, 53);
    private static final int BLUE = Color.rgb(49, 94, 232);
    private static final int INK = Color.rgb(23, 35, 61);
    private static final int MUTED = Color.rgb(112, 124, 145);

    private FrameLayout root;
    private FrameLayout webContainer;
    private WebView webView;
    private ProgressBar progressBar;
    private View errorView;
    private SharedPreferences preferences;
    private LinearLayout bottomNav;
    private final TextView[] navItems = new TextView[4];
    private final String[] navPages = {"home", "pyq", "mock", "currentaffairs"};
    private boolean showingError = false;
    private String activePage = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(0);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(245, 247, 252));
        setContentView(root);

        if (preferences.getBoolean("profile_complete", false)) {
            showMainApp();
        } else {
            showRegistrationScreen();
        }
    }

    private void showRegistrationScreen() {
        root.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(NAVY);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setPadding(dp(22), dp(28), dp(22), dp(28));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(24));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(22));
        card.setBackground(cardBg);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.website_hero_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(172), dp(185));
        logoParams.bottomMargin = dp(8);
        card.addView(logo, logoParams);

        TextView heading = new TextView(this);
        heading.setText("Welcome to SSC EXAM.IN");
        heading.setTextColor(INK);
        heading.setTextSize(22);
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(null, Typeface.BOLD);
        card.addView(heading, matchWrap());

        TextView intro = new TextView(this);
        intro.setText("App सुरू करण्यासाठी तुमची माहिती भरा");
        intro.setTextColor(MUTED);
        intro.setTextSize(14);
        intro.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams introParams = matchWrap();
        introParams.topMargin = dp(7);
        introParams.bottomMargin = dp(18);
        card.addView(intro, introParams);

        addFieldLabel(card, "नाव / Nickname");
        EditText nameInput = makeInput("तुमचे नाव लिहा",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        card.addView(nameInput, fieldParams());

        addFieldLabel(card, "वय (वर्षांत)");
        EditText ageInput = makeInput("उदा. 17.5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        card.addView(ageInput, fieldParams());

        TextView ageNote = new TextView(this);
        ageNote.setText("किमान वय: 17.5 वर्षे (17 वर्षे 6 महिने)");
        ageNote.setTextColor(BLUE);
        ageNote.setTextSize(13);
        LinearLayout.LayoutParams ageNoteParams = matchWrap();
        ageNoteParams.topMargin = dp(4);
        card.addView(ageNote, ageNoteParams);

        Button continueButton = new Button(this);
        continueButton.setText("Continue");
        continueButton.setTextColor(Color.WHITE);
        continueButton.setTextSize(16);
        continueButton.setAllCaps(false);
        continueButton.setBackgroundTintList(ColorStateList.valueOf(BLUE));
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        continueParams.topMargin = dp(20);
        card.addView(continueButton, continueParams);

        TextView privacyNote = new TextView(this);
        privacyNote.setText("नाव आणि वय फक्त या फोनमध्ये सेव्ह राहील.");
        privacyNote.setTextColor(MUTED);
        privacyNote.setTextSize(12);
        privacyNote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = matchWrap();
        privacyParams.topMargin = dp(12);
        card.addView(privacyNote, privacyParams);

        continueButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String ageText = ageInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("नाव लिहा");
                nameInput.requestFocus();
                return;
            }
            double age;
            try {
                age = Double.parseDouble(ageText);
            } catch (Exception e) {
                ageInput.setError("वय 17.5 अशा स्वरूपात लिहा");
                ageInput.requestFocus();
                return;
            }
            if (age < MINIMUM_AGE || age > 120) {
                ageInput.setError("किमान वय 17.5 वर्षे असणे आवश्यक आहे");
                ageInput.requestFocus();
                Toast.makeText(this, "या अॅपसाठी किमान वय 17.5 वर्षे आहे.", Toast.LENGTH_LONG).show();
                return;
            }

            preferences.edit()
                    .putString("name", name)
                    .putString("age", ageText)
                    .putBoolean("profile_complete", true)
                    .apply();
            showMainApp();
        });

        outer.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(outer, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showMainApp() {
        root.removeAllViews();

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.rgb(245, 247, 252));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(15), dp(5), dp(13), dp(5));
        GradientDrawable toolbarBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{NAVY, Color.rgb(25, 55, 102)});
        toolbar.setBackground(toolbarBg);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.website_hero_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        toolbar.addView(logo, new LinearLayout.LayoutParams(dp(43), dp(48)));

        LinearLayout brandStack = new LinearLayout(this);
        brandStack.setOrientation(LinearLayout.VERTICAL);
        brandStack.setGravity(Gravity.CENTER_VERTICAL);
        brandStack.setPadding(dp(10), 0, 0, 0);

        TextView brand = new TextView(this);
        brand.setText("SSC EXAM.IN");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(16);
        brand.setTypeface(null, Typeface.BOLD);
        brandStack.addView(brand, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("ALL EXAM INDIA  •  FREE PRACTICE");
        subtitle.setTextColor(Color.rgb(194, 210, 239));
        subtitle.setTextSize(9);
        subtitle.setLetterSpacing(0.06f);
        brandStack.addView(subtitle, matchWrap());

        toolbar.addView(brandStack, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView profileButton = new TextView(this);
        profileButton.setText("👤");
        profileButton.setTextSize(21);
        profileButton.setGravity(Gravity.CENTER);
        profileButton.setContentDescription("Profile");
        GradientDrawable profileBg = new GradientDrawable();
        profileBg.setColor(Color.rgb(49, 94, 232));
        profileBg.setCornerRadius(dp(15));
        profileButton.setBackground(profileBg);
        profileButton.setOnClickListener(v -> showProfileDialog());
        toolbar.addView(profileButton, new LinearLayout.LayoutParams(dp(44), dp(44)));

        shell.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

        webContainer = new FrameLayout(this);
        shell.addView(webContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        bottomNav = buildBottomNavigation();
        shell.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));

        root.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        initializeWebView();
        if (webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }
        webContainer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(ColorStateList.valueOf(BLUE));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP);
        webContainer.addView(progressBar, progressParams);

        updateNavigation(activePage);
        if (webView.getUrl() == null) {
            webView.loadUrl(HOME_URL);
        } else {
            applyNativeAppAppearance();
        }
    }

    private LinearLayout buildBottomNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(7), dp(5), dp(7), dp(5));

        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.WHITE);
        barBg.setStroke(dp(1), Color.rgb(229, 234, 243));
        bar.setBackground(barBg);
        bar.setElevation(dp(12));

        String[] icons = {"⌂", "▤", "◎", "▣"};
        String[] labels = {"Home", "PYQ", "Mock", "Current"};
        for (int i = 0; i < navPages.length; i++) {
            final String page = navPages[i];
            TextView item = new TextView(this);
            item.setText(icons[i] + "\n" + labels[i]);
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
        if (webView == null) return;
        String safePage = JSONObject.quote(page);
        webView.evaluateJavascript(
                "if(typeof showPage==='function'){showPage(" + safePage + ");}", null);
    }

    private void updateNavigation(String page) {
        if (page == null) return;
        activePage = page;
        String selectedPage = page.equals("testroom") ? "mock" : page;
        for (int i = 0; i < navItems.length; i++) {
            TextView item = navItems[i];
            if (item == null) continue;
            boolean selected = navPages[i].equals(selectedPage);
            item.setTextColor(selected ? BLUE : Color.rgb(112, 124, 145));
            item.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(13));
            bg.setColor(selected ? Color.rgb(235, 241, 255) : Color.TRANSPARENT);
            item.setBackground(bg);
        }
    }

    private void initializeWebView() {
        if (webView != null) return;

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(245, 247, 252));
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
                if (errorView != null && webContainer != null) {
                    webContainer.removeView(errorView);
                    errorView = null;
                }
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                applyNativeAppAppearance();
                applyProfileToWebsite();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    showConnectionError();
                }
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
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this,
                        "हा डाउनलोड उघडण्यासाठी अॅप उपलब्ध नाही.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private class AndroidAppBridge {
        @JavascriptInterface
        public void onPageChanged(String page) {
            runOnUiThread(() -> updateNavigation(page));
        }
    }

    private void applyNativeAppAppearance() {
        if (webView == null) return;

        String css =
                "body{background:#f5f7fc!important;overscroll-behavior-y:contain}" +
                "body:after,body>header,body>nav,body>footer,.hero{display:none!important}" +
                "main.container{max-width:none!important;width:100%!important;margin:0!important;" +
                "padding:15px 13px 22px!important;min-height:100%!important}" +
                ".page.active{animation:rise .18s ease}" +
                ".container h2{font-size:22px;margin:5px 0 9px}" +
                ".container h2:after{width:34px;height:3px;margin-top:7px}" +
                ".home-actions{gap:11px;margin:7px 0 15px}" +
                ".home-action-card{min-height:185px;padding:18px;border-radius:17px}" +
                ".home-action-card h3{font-size:19px}" +
                ".home-action-icon{font-size:32px}" +
                ".card,.question{border-radius:15px;box-shadow:0 5px 18px #18294b0a}" +
                ".search,select{border-radius:11px}" +
                ".question-sidebar{top:8px}" +
                "body.dark{background:#0c1425!important}" +
                "body.dark main.container{background:#0c1425!important}";

        String script = "(function(){try{" +
                "var style=document.getElementById('aei-native-app-style');" +
                "if(!style){style=document.createElement('style');style.id='aei-native-app-style';" +
                "style.textContent=" + JSONObject.quote(css) + ";document.head.appendChild(style);}" +
                "if(!window.__aeiNativePageHook&&typeof window.showPage==='function'){" +
                "var originalShowPage=window.showPage;" +
                "window.showPage=function(id,fromHistory){var result=originalShowPage.apply(this,arguments);" +
                "try{var active=document.querySelector('.page.active');" +
                "if(active&&window.AndroidApp)AndroidApp.onPageChanged(active.id);}catch(e){}" +
                "return result;};window.__aeiNativePageHook=true;}" +
                "var active=document.querySelector('.page.active');" +
                "if(active&&window.AndroidApp)AndroidApp.onPageChanged(active.id);" +
                "}catch(e){}})();";
        webView.evaluateJavascript(script, null);
    }

    private void applyProfileToWebsite() {
        if (webView == null) return;
        String name = preferences.getString("name", "");
        if (name.isEmpty()) return;
        String script = "(function(){try{var n=" + JSONObject.quote(name) + ";"
                + "localStorage.setItem('aei-student',JSON.stringify({name:n}));"
                + "var input=document.getElementById('regName');if(input)input.value=n;"
                + "var modal=document.getElementById('registerModal');if(modal)modal.style.display='none';"
                + "if(typeof renderStudentStats==='function')renderStudentStats();"
                + "}catch(e){}})();";
        webView.evaluateJavascript(script, null);
    }

    private void showProfileDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));

        addFieldLabel(form, "नाव / Nickname");
        EditText nameInput = makeInput("तुमचे नाव लिहा",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(preferences.getString("name", ""));
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        form.addView(nameInput, fieldParams());

        addFieldLabel(form, "वय (वर्षांत)");
        EditText ageInput = makeInput("उदा. 17.5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ageInput.setText(preferences.getString("age", ""));
        form.addView(ageInput, fieldParams());

        TextView note = new TextView(this);
        note.setText("किमान वय: 17.5 वर्षे (17 वर्षे 6 महिने)");
        note.setTextColor(BLUE);
        note.setTextSize(13);
        form.addView(note, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("तुमचे Profile")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String ageText = ageInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("नाव लिहा");
                return;
            }
            double age;
            try {
                age = Double.parseDouble(ageText);
            } catch (Exception e) {
                ageInput.setError("वय 17.5 अशा स्वरूपात लिहा");
                return;
            }
            if (age < MINIMUM_AGE || age > 120) {
                ageInput.setError("किमान वय 17.5 वर्षे असणे आवश्यक आहे");
                Toast.makeText(this, "किमान वय 17.5 वर्षे आहे.", Toast.LENGTH_LONG).show();
                return;
            }

            preferences.edit().putString("name", name).putString("age", ageText).apply();
            applyProfileToWebsite();
            dialog.dismiss();
            Toast.makeText(this, "Profile सेव्ह झाले.", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void addFieldLabel(LinearLayout parent, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(Color.rgb(35, 48, 72));
        label.setTextSize(14);
        label.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(9);
        params.bottomMargin = dp(4);
        parent.addView(label, params);
    }

    private EditText makeInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextSize(15);
        input.setTextColor(INK);
        input.setHintTextColor(Color.rgb(145, 153, 168));
        input.setPadding(dp(13), dp(10), dp(13), dp(10));
        input.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(190, 201, 219)));
        return input;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        params.bottomMargin = dp(5);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private boolean handleUrl(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();

        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.equals("prajwaltarare7-oss.github.io")) {
                return false;
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "ही लिंक उघडता आली नाही.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void showConnectionError() {
        if (showingError || webContainer == null) return;
        showingError = true;

        TextView message = new TextView(this);
        message.setText("इंटरनेट कनेक्शन तपासा आणि पुन्हा प्रयत्न करा.\n\nAll Exam India");
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(28), dp(24), dp(28), dp(24));

        Button retry = new Button(this);
        retry.setText("पुन्हा प्रयत्न करा");
        retry.setTextColor(Color.WHITE);
        retry.setAllCaps(false);
        retry.setBackgroundTintList(ColorStateList.valueOf(BLUE));
        retry.setOnClickListener(v -> {
            showingError = false;
            if (errorView != null) webContainer.removeView(errorView);
            errorView = null;
            webView.loadUrl(HOME_URL);
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        content.setBackgroundColor(NAVY);
        content.addView(message, matchWrap());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryParams.topMargin = dp(12);
        content.addView(retry, retryParams);

        errorView = content;
        webContainer.addView(errorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (!"home".equals(activePage)) {
            navigateToPage("home");
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
