package com.example.sid.NotesCrypt.activity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        Element versionElement = new Element();
        versionElement.setTitle("Version 1.0");


        View aboutPage = new AboutPage(this)
                .setDescription("NotesCrypt is a simple secure application that uses Android Key Store API and Fingerprint API to keep your notes private and safe.")
                .addGitHub("sidstk/NotesCrypt")
                .addItem(versionElement)
                .create();
        setContentView(aboutPage);
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
