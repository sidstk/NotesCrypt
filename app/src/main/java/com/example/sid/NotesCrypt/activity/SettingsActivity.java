package com.example.sid.NotesCrypt.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import com.example.sid.NotesCrypt.AuthenticationHelper;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.fingerprint.ChangePasswordFragment;

public class SettingsActivity extends AppCompatActivity {

    public  Switch aSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences("dataa", Context.MODE_PRIVATE);
        aSwitch = findViewById(R.id.fingerprintToggle);


        if(!mSharedPreferences.getBoolean(getString(R.string.fingerprint),false)){
            Log.i("info","disabling switch");
            aSwitch.setEnabled(false);
            findViewById(R.id.hardwareMissing).setVisibility(View.VISIBLE);
        }

        if(mSharedPreferences.getBoolean(getString(R.string.use_fingerprint_future),false)){
            aSwitch.setChecked(true);
        }

        else
            aSwitch.setChecked(false);

        aSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(aSwitch.isChecked()){
                    AuthenticationHelper authenticationHelper = new AuthenticationHelper(SettingsActivity.this);
                    authenticationHelper.listener("fp_true", -1);
                }
                else{
                    AuthenticationHelper authenticationHelper = new AuthenticationHelper(SettingsActivity.this);
                    authenticationHelper.listener("fp_false", -1);
                }
            }
        });

        findViewById(R.id.changePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();
                changePasswordFragment.setCancelable(false);
                changePasswordFragment.show(SettingsActivity.this.getFragmentManager(), changePasswordFragment.getTag());

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default: return false;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}