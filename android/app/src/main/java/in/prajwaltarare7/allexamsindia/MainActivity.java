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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final String HOME_URL =
            "https://prajwaltarare7-oss.github.io/all-exam-india/";
    private static final double MINIMUM_AGE = 17.5;
    private static final String PREFS = "all_exam_india_profile";
    private static final String SUPABASE_URL = "https://qutfsjeczixgnadpyrls.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_KIYCIMCasEY6_9QE5VS1Fg_uXL8_pPf";
    private static final String AUTH_PREFS = "all_exam_india_auth";
    private static final String AUTH_TOKEN_KEY = "refresh_token_encrypted";
    private static final String KEY_ALIAS = "all_exam_india_auth_key";
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
    private final TextView[] navItems = new TextView[5];
    private final String[] navPages = {"home", "pyq", "mock", "currentaffairs", "settings"};
    private boolean showingError = false;
    private String activePage = "home";
    private boolean authEmailMode = true;
    private boolean otpPending = false;
    private String pendingContact = "";

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

        String savedRefreshToken = getSavedRefreshToken();
        if (savedRefreshToken == null) {
            showAuthScreen("");
        } else {
            refreshSavedSession(savedRefreshToken);
        }
    }


    private void continueAfterAuthentication() {
        otpPending = false;
        pendingContact = "";
        if (preferences.getBoolean("profile_complete", false)) {
            showMainApp();
        } else {
            showRegistrationScreen();
        }
    }

    private void showAuthScreen(String statusMessage) {
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
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(132), dp(142));
        logoParams.bottomMargin = dp(8);
        card.addView(logo, logoParams);

        TextView heading = new TextView(this);
        heading.setText("All Exam India Login");
        heading.setTextColor(INK);
        heading.setTextSize(22);
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(null, Typeface.BOLD);
        card.addView(heading, matchWrap());

        TextView intro = new TextView(this);
        intro.setText(otpPending
                ? "Enter the one-time password sent to your contact."
                : "Continue with your email address or mobile number.");
        intro.setTextColor(MUTED);
        intro.setTextSize(14);
        intro.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams introParams = matchWrap();
        introParams.topMargin = dp(7);
        introParams.bottomMargin = dp(18);
        card.addView(intro, introParams);

        if (!otpPending) {
            LinearLayout modes = new LinearLayout(this);
            modes.setOrientation(LinearLayout.HORIZONTAL);
            modes.setGravity(Gravity.CENTER);
            Button emailMode = new Button(this);
            emailMode.setText("Email OTP");
            emailMode.setAllCaps(false);
            emailMode.setTextSize(14);
            emailMode.setTextColor(authEmailMode ? Color.WHITE : INK);
            emailMode.setBackgroundTintList(ColorStateList.valueOf(
                    authEmailMode ? BLUE : Color.rgb(232, 237, 247)));
            Button phoneMode = new Button(this);
            phoneMode.setText("Mobile OTP");
            phoneMode.setAllCaps(false);
            phoneMode.setTextSize(14);
            phoneMode.setTextColor(authEmailMode ? INK : Color.WHITE);
            phoneMode.setBackgroundTintList(ColorStateList.valueOf(
                    authEmailMode ? Color.rgb(232, 237, 247) : BLUE));
            LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            modeParams.setMargins(dp(3), 0, dp(3), 0);
            modes.addView(emailMode, modeParams);
            modes.addView(phoneMode, modeParams);
            card.addView(modes, matchWrap());
            emailMode.setOnClickListener(v -> {
                authEmailMode = true;
                showAuthScreen("");
            });
            phoneMode.setOnClickListener(v -> {
                authEmailMode = false;
                showAuthScreen("");
            });

            addFieldLabel(card, authEmailMode ? "Email address" : "Mobile number");
            EditText contactInput = makeInput(
                    authEmailMode ? "Enter your email address" : "10-digit mobile number",
                    authEmailMode
                            ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            : InputType.TYPE_CLASS_PHONE);
            card.addView(contactInput, fieldParams());

            if (authEmailMode) {
                TextView emailHint = new TextView(this);
                emailHint.setText("We will send a one-time code to your email.");
                emailHint.setTextColor(MUTED);
                emailHint.setTextSize(12);
                card.addView(emailHint, matchWrap());
            } else {
                TextView phoneHint = new TextView(this);
                phoneHint.setText("For an Indian number, enter your 10-digit mobile number.");
                phoneHint.setTextColor(MUTED);
                phoneHint.setTextSize(12);
                card.addView(phoneHint, matchWrap());
            }

            Button sendButton = new Button(this);
            sendButton.setText("Send OTP");
            sendButton.setTextColor(Color.WHITE);
            sendButton.setTextSize(16);
            sendButton.setAllCaps(false);
            sendButton.setBackgroundTintList(ColorStateList.valueOf(BLUE));
            LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
            sendParams.topMargin = dp(20);
            card.addView(sendButton, sendParams);

            sendButton.setOnClickListener(v -> {
                String contact = contactInput.getText().toString().trim();
                if (authEmailMode) {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contact).matches()) {
                        contactInput.setError("Enter a valid email address");
                        contactInput.requestFocus();
                        return;
                    }
                } else {
                    contact = normalizePhoneNumber(contact);
                    if (contact == null) {
                        contactInput.setError("Enter a valid mobile number with country code");
                        contactInput.requestFocus();
                        return;
                    }
                }
                pendingContact = contact;
                sendButton.setEnabled(false);
                sendButton.setText("Sending...");
                final boolean useEmail = authEmailMode;
                final String destination = contact;
                new Thread(() -> {
                    try {
                        JSONObject body = new JSONObject();
                        if (useEmail) body.put("email", destination);
                        else body.put("phone", destination);
                        body.put("create_user", true);
                        postAuth("/otp", body);

                        runOnUiThread(() -> {
                            otpPending = true;
                            pendingContact = destination;
                            showAuthScreen("OTP sent to " + destination + ". Check your inbox or messages.");
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            sendButton.setEnabled(true);
                            sendButton.setText("Send OTP");
                            Toast.makeText(MainActivity.this,
                                    friendlyAuthError(e), Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            });
        } else {
            TextView destination = new TextView(this);
            destination.setText((authEmailMode ? "Email: " : "Mobile: ") + pendingContact);
            destination.setTextColor(MUTED);
            destination.setTextSize(13);
            destination.setGravity(Gravity.CENTER);
            card.addView(destination, matchWrap());

            addFieldLabel(card, "One-time password");
            EditText otpInput = makeInput("Enter the OTP", InputType.TYPE_CLASS_NUMBER);
            otpInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
            card.addView(otpInput, fieldParams());

            TextView changeContact = new TextView(this);
            changeContact.setText("Change email / mobile number");
            changeContact.setTextColor(BLUE);
            changeContact.setTextSize(13);
            changeContact.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams changeParams = matchWrap();
            changeParams.topMargin = dp(8);
            card.addView(changeContact, changeParams);
            changeContact.setOnClickListener(v -> {
                otpPending = false;
                pendingContact = "";
                showAuthScreen("");
            });

            Button verifyButton = new Button(this);
            verifyButton.setText("Verify OTP");
            verifyButton.setTextColor(Color.WHITE);
            verifyButton.setTextSize(16);
            verifyButton.setAllCaps(false);
            verifyButton.setBackgroundTintList(ColorStateList.valueOf(BLUE));
            LinearLayout.LayoutParams verifyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
            verifyParams.topMargin = dp(20);
            card.addView(verifyButton, verifyParams);

            verifyButton.setOnClickListener(v -> {
                String token = otpInput.getText().toString().trim();
                if (token.length() < 4) {
                    otpInput.setError("Enter the OTP you received");
                    otpInput.requestFocus();
                    return;
                }
                verifyButton.setEnabled(false);
                verifyButton.setText("Verifying...");
                new Thread(() -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("type", authEmailMode ? "email" : "sms");
                        body.put(authEmailMode ? "email" : "phone", pendingContact);
                        body.put("token", token);
                        JSONObject response = postAuth("/verify", body);
                        String refreshToken = response.optString("refresh_token", "");
                        if (refreshToken.isEmpty()) {
                            throw new Exception("Supabase did not return a login session.");
                        }
                        saveRefreshToken(refreshToken);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                            continueAfterAuthentication();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify OTP");
                            Toast.makeText(MainActivity.this,
                                    friendlyAuthError(e), Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            });
        }

        if (statusMessage != null && !statusMessage.isEmpty()) {
            TextView status = new TextView(this);
            status.setText(statusMessage);
            status.setTextColor(BLUE);
            status.setTextSize(12);
            status.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams statusParams = matchWrap();
            statusParams.topMargin = dp(14);
            card.addView(status, statusParams);
        }

        TextView privacyNote = new TextView(this);
        privacyNote.setText("A one-time password keeps your account secure. Never share your OTP.");
        privacyNote.setTextColor(MUTED);
        privacyNote.setTextSize(11);
        privacyNote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = matchWrap();
        privacyParams.topMargin = dp(16);
        card.addView(privacyNote, privacyParams);

        outer.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(outer, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private String normalizePhoneNumber(String raw) {
        if (raw == null) return null;
        String phone = raw.replaceAll("[\\s()-]", "");
        if (phone.matches("\\d{10}")) phone = "+91" + phone;
        else if (phone.matches("91\\d{10}")) phone = "+" + phone;
        if (!phone.matches("\\+[1-9]\\d{7,14}")) return null;
        return phone;
    }

    private void showAuthLoadingScreen(String message) {
        root.removeAllViews();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(28), dp(28), dp(28), dp(28));
        panel.setBackgroundColor(NAVY);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.website_hero_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        panel.addView(logo, new LinearLayout.LayoutParams(dp(132), dp(142)));

        TextView label = new TextView(this);
        label.setText(message);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = matchWrap();
        labelParams.topMargin = dp(12);
        panel.addView(label, labelParams);

        Button retry = new Button(this);
        retry.setText("Try Again");
        retry.setAllCaps(false);
        retry.setTextColor(Color.WHITE);
        retry.setBackgroundTintList(ColorStateList.valueOf(BLUE));
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        retryParams.topMargin = dp(18);
        panel.addView(retry, retryParams);
        retry.setOnClickListener(v -> {
            String token = getSavedRefreshToken();
            if (token == null) showAuthScreen("");
            else refreshSavedSession(token);
        });

        root.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void refreshSavedSession(String refreshToken) {
        showAuthLoadingScreen("Checking your secure login...");
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("refresh_token", refreshToken);
                JSONObject response = postAuth("/token?grant_type=refresh_token", body);
                String newRefreshToken = response.optString("refresh_token", refreshToken);
                saveRefreshToken(newRefreshToken);
                runOnUiThread(this::continueAfterAuthentication);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (e instanceof AuthRequestException
                            && (((AuthRequestException) e).statusCode == 400
                            || ((AuthRequestException) e).statusCode == 401
                            || ((AuthRequestException) e).statusCode == 403)) {
                        clearSavedAuth();
                        showAuthScreen("Your previous login session expired. Please sign in again.");
                    } else {
                        showAuthLoadingScreen("Could not check your login. Check your internet connection and try again.");
                    }
                });
            }
        }).start();
    }

    private JSONObject postAuth(String path, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(SUPABASE_URL + "/auth/v1" + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            byte[] requestBytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBytes);
            }

            int statusCode = connection.getResponseCode();
            InputStream input = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            if (input != null) {
                try (InputStream stream = input) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = stream.read(buffer)) != -1) result.write(buffer, 0, count);
                }
            }
            String responseText = result.toString("UTF-8");
            JSONObject response = responseText.isEmpty() ? new JSONObject() : new JSONObject(responseText);
            if (statusCode < 200 || statusCode >= 300) {
                String message = response.optString("msg", "");
                if (message.isEmpty()) message = response.optString("message", "");
                if (message.isEmpty()) message = response.optString("error_description", "");
                if (message.isEmpty()) message = response.optString("error", "");
                if (message.isEmpty()) message = "Authentication request failed (HTTP " + statusCode + ").";
                throw new AuthRequestException(statusCode, message);
            }
            return response;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String friendlyAuthError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return "Something went wrong. Please try again.";
        if (message.toLowerCase().contains("email address not authorized")) {
            return "Supabase email delivery is not configured for this address yet. Set up custom SMTP in Supabase.";
        }
        if (message.toLowerCase().contains("phone provider")
                || message.toLowerCase().contains("sms provider")) {
            return "Mobile OTP needs an SMS provider configured in Supabase Auth settings.";
        }
        return message;
    }

    private static class AuthRequestException extends Exception {
        final int statusCode;
        AuthRequestException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    private void saveRefreshToken(String refreshToken) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            generator.generateKey();
        }
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit()
                .putString(AUTH_TOKEN_KEY, Base64.encodeToString(combined, Base64.NO_WRAP))
                .apply();
    }

    private String getSavedRefreshToken() {
        String stored = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .getString(AUTH_TOKEN_KEY, null);
        if (stored == null || stored.isEmpty()) return null;
        try {
            byte[] combined = Base64.decode(stored, Base64.NO_WRAP);
            if (combined.length <= 12) return null;
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            if (secretKey == null) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            clearSavedAuth();
            return null;
        }
    }

    private void clearSavedAuth() {
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit().clear().apply();
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
        intro.setText("Enter your details to start using the app");
        intro.setTextColor(MUTED);
        intro.setTextSize(14);
        intro.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams introParams = matchWrap();
        introParams.topMargin = dp(7);
        introParams.bottomMargin = dp(18);
        card.addView(intro, introParams);

        addFieldLabel(card, "Full Name");
        EditText nameInput = makeInput("Enter your name",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        card.addView(nameInput, fieldParams());

        addFieldLabel(card, "Age (in years)");
        EditText ageInput = makeInput("e.g. 17.5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        card.addView(ageInput, fieldParams());

        TextView ageNote = new TextView(this);
        ageNote.setText("Minimum age: 17.5 years (17 years and 6 months)");
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
        privacyNote.setText("Your name and age will be saved only on this phone.");
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
                nameInput.setError("Enter your name");
                nameInput.requestFocus();
                return;
            }
            double age;
            try {
                age = Double.parseDouble(ageText);
            } catch (Exception e) {
                ageInput.setError("Enter age in a format such as 17.5");
                ageInput.requestFocus();
                return;
            }
            if (age < MINIMUM_AGE || age > 120) {
                ageInput.setError("Minimum age must be 17.5 years");
                ageInput.requestFocus();
                Toast.makeText(this, "The minimum age for this app is 17.5 years.", Toast.LENGTH_LONG).show();
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

        String[] icons = {"⌂", "▤", "◎", "▣", "⚙"};
        String[] labels = {"Home", "PYQ", "Mock", "Current", "Settings"};
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
                        "No app is available to open this download.", Toast.LENGTH_LONG).show();
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

        addFieldLabel(form, "Full Name");
        EditText nameInput = makeInput("Enter your name",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(preferences.getString("name", ""));
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        form.addView(nameInput, fieldParams());

        addFieldLabel(form, "Age (in years)");
        EditText ageInput = makeInput("e.g. 17.5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ageInput.setText(preferences.getString("age", ""));
        form.addView(ageInput, fieldParams());

        TextView note = new TextView(this);
        note.setText("Minimum age: 17.5 years (17 years and 6 months)");
        note.setTextColor(BLUE);
        note.setTextSize(13);
        form.addView(note, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Your Profile")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String ageText = ageInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Enter your name");
                return;
            }
            double age;
            try {
                age = Double.parseDouble(ageText);
            } catch (Exception e) {
                ageInput.setError("Enter age in a format such as 17.5");
                return;
            }
            if (age < MINIMUM_AGE || age > 120) {
                ageInput.setError("Minimum age must be 17.5 years");
                Toast.makeText(this, "Minimum age is 17.5 years.", Toast.LENGTH_LONG).show();
                return;
            }

            preferences.edit().putString("name", name).putString("age", ageText).apply();
            applyProfileToWebsite();
            dialog.dismiss();
            Toast.makeText(this, "Profile saved.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Could not open this link.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void showConnectionError() {
        if (showingError || webContainer == null) return;
        showingError = true;

        TextView message = new TextView(this);
        message.setText("Check your internet connection and try again.\n\nAll Exam India");
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(28), dp(24), dp(28), dp(24));

        Button retry = new Button(this);
        retry.setText("Try Again");
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
