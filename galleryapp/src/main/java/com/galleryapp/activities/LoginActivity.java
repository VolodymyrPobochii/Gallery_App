package com.galleryapp.activities;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.galleryapp.Config;
import com.galleryapp.R;
import com.galleryapp.services.LoginService;

public class LoginActivity extends BaseActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    private static final int TOKEN_LENGTH = 80;
    public static final int LOGIN_REQUEST = 108;
    private IntentFilter mFilter;
    private Button loginButton;
    private Intent mServiceLoginIntent;
    private String token = "";
    private ProgressDialog loginProgress;
    private boolean tokenReceived = false;
    private EditText login;
    private EditText pass;
    private CheckBox saveLogin;

    // Values for login and password at the time of the login attempt.
    private String mLogin;
    private String mPassword;
    private Intent mOnCreateIntent;
    private SharedPreferences mPreff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        mOnCreateIntent = getIntent();
        setContentView(R.layout.login_screen);
        setTitle(R.string.authenticate);
        mPreff = getApp().getPreff();
        initViews();
        initServices();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            switch (resultCode) {
                case LoginService.CONNECTION_START:
                    loginProgress = getApp().customProgressDialog(LoginActivity.this, getResources().getString(R.string.authentication_progress));
                    loginProgress.show();
                    break;
                case LoginService.CONNECTION_SUCCESS:
                    if (loginProgress != null && loginProgress.isShowing()) {
                        loginProgress.setMessage(getResources().getString(R.string.connection_established));
                    }
                    break;
                case LoginService.CONNECTION_ERROR:
                    if (loginProgress != null && loginProgress.isShowing()) {
                        loginProgress.setMessage(getResources().getString(R.string.server_connection_problem));
                        loginProgress.dismiss();
                        getApp().customAlertDialog(LoginActivity.this, getResources().getString(R.string.server_connection_problem),
                                getString(R.string.close), false, null, false, false).show();
                    }
                    break;
                case LoginService.AUTH_FINISHED:
                    checkAuthResult(data);
                    break;
            }
        }
        if (requestCode == GalleryActivity.REQUEST_SETTINGS) {
            Log.d("LoginActivity", "onActResult()::requestCode == GalleryActivity.REQUEST_SETTINGS");
            Log.d("LoginActivity", "onActResult()::resultCode == " + resultCode);
            if (resultCode == RESULT_OK) {
                Log.d("LoginActivity", "onActResult()::resultCode == RESULT_OK");
                getApp().setUpHost();
                initViews();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkAuthResult(Intent intent) {
        if (intent.hasExtra(getString(R.string.response_token)) && !tokenReceived) {
            String responseToken = intent.getStringExtra(getString(R.string.response_token));
            if (responseToken != null && responseToken.length() > TOKEN_LENGTH) {
                if (loginProgress != null && loginProgress.isShowing()) loginProgress.dismiss();
                getApp().customAlertDialog(LoginActivity.this, getResources().getString(R.string.login_pass_problem),
                        getString(R.string.close), false, null, false, false).show();
                tokenReceived = true;
                return;
            }
            getApp().setToken(responseToken);
            Log.d("PostLoginReceiver", "onReceive()" + "TOKEN = " + responseToken);
            tokenReceived = true;
            completeAuth();
        } else if (intent.hasExtra(Config.SERVER_CONNECTION)) {
            if (LoginService.AUTH_PROBLEM.equalsIgnoreCase(intent.getStringExtra(Config.SERVER_CONNECTION))) {
                if (loginProgress != null && loginProgress.isShowing()) loginProgress.dismiss();
                getApp().customAlertDialog(LoginActivity.this, getResources().getString(R.string.auth_problem),
                        getString(R.string.close), false, null, false, false).show();
            }
        }
    }

    public void initServices() {
        // Creating an intent service
        mServiceLoginIntent = new Intent(LoginActivity.this, LoginService.class);
    }

    private void initViews() {
        saveLogin = (CheckBox) findViewById(R.id.save_credentials);
        saveLogin.setChecked(mPreff.getBoolean(getString(R.string.save_login), false));

        login = (EditText) findViewById(R.id.login_txt);
        pass = (EditText) findViewById(R.id.password_txt);

        String username = mPreff.getString(getString(R.string.login), "");
        String password = mPreff.getString(getString(R.string.password), "");

        if (saveLogin.isChecked() || (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password))) {
            login.setText(username);
            pass.setText(password);
        }
        loginButton = (Button) findViewById(R.id.login);
        loginButton.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getApp().isNetworkConnected()) {
                    attemptLogin();
                } else {
                    getApp().noConnectionDialog();
                }
            }
        });
    }

    private void attemptLogin() {
        // Reset errors.
        login.setError(null);
        pass.setError(null);

        // Store values at the time of the login attempt.
        mLogin = login.getText().toString();
        mPassword = pass.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            pass.setError(getString(R.string.error_field_required));
            focusView = pass;
            cancel = true;
        }

        // Check for a valid login address.
        if (TextUtils.isEmpty(mLogin)) {
            login.setError(getString(R.string.error_field_required));
            focusView = login;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            /*mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);*/
            tokenReceived = false;
            // Starting the OAuthService to fetch the capital of the country
            PendingIntent loginProgressIntent = createPendingResult(LOGIN_REQUEST, new Intent(), 0);
            mServiceLoginIntent.putExtra("loginProgressIntent", loginProgressIntent);
            mServiceLoginIntent.putExtra("login", login.getText().toString());
            mServiceLoginIntent.putExtra("pass", pass.getText().toString());
            mServiceLoginIntent.putExtra("baseUrl", getApp().getLoginBaseUrl());
            mServiceLoginIntent.putExtra("hostName", getApp().getHostName());
            mServiceLoginIntent.putExtra("port", getApp().getPort());
            startService(mServiceLoginIntent);
        }
    }

    private void completeAuth() {
        if (tokenReceived) {
            startActivity(new Intent(LoginActivity.this, GalleryActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, PrefActivity.class), GalleryActivity.REQUEST_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        completeAuth(getIntent());
    }

    @Override
    public void finish() {
        if (saveLogin.isChecked()) {
            mPreff.edit()
                    .putBoolean("saveLogin", true)
                    .putString("username", login.getText().toString())
                    .putString("password", pass.getText().toString())
                    .apply();
        } else {
            mPreff.edit()
                    .putBoolean("saveLogin", false)
//                    .putString("username", "")
//                    .putString("password", "")
                    .apply();
        }
        super.finish();
    }
}
