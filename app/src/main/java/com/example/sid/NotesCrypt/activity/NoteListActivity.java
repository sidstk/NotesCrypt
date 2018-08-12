package com.example.sid.NotesCrypt.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.sid.NotesCrypt.utils.AuthenticationHelper;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.database.DatabaseHelper;
import com.example.sid.NotesCrypt.database.model.Note;
import com.example.sid.NotesCrypt.utils.CipherEngine;
import com.example.sid.NotesCrypt.utils.MyDividerItemDecoration;
import com.example.sid.NotesCrypt.utils.RecyclerTouchListener;
import com.example.sid.NotesCrypt.view.NotesAdapter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;


public class NoteListActivity extends AppCompatActivity {
    public  static NotesAdapter mAdapter;
    public static List<Note> notesList = new ArrayList<>();
    private static TextView noNotesView;

    public  static DatabaseHelper db;

    private SharedPreferences mSharedPreferences;
    private Cipher mCipher;



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_settings,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notelist);
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        noNotesView = findViewById(R.id.empty_notes_view);



        db = new DatabaseHelper(getApplicationContext());

        notesList.addAll(db.getAllNotes());


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                startNoteActivity(0,-1);
            }
        });

        mAdapter = new NotesAdapter(this, notesList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyNotes();


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                showActionsDialog(position);
            }

        }));


        mSharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shred_preference),
                Context.MODE_PRIVATE);


    }

    /**
     * Deleting note from SQLite and removing the
     * item from the list by its position
     */
    public static void deleteNote(int position) {
        // deleting the note from db
        db.deleteNote(notesList.get(position));

        // removing the note from the list
        notesList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyNotes();
    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */
    private void showActionsDialog(final int position) {
        CharSequence colors[] = new CharSequence[]{"Edit", "Delete"};

        final boolean use_fingerprint = mSharedPreferences.getBoolean(getApplicationContext().getString(R.string.fingerprint),true) &&
                mSharedPreferences.getBoolean(getApplicationContext().getString(R.string.use_fingerprint_future),true);
        if(use_fingerprint) {
            AuthenticationHelper.createKey(CipherEngine.DEFAULT_KEY_NAME,true, true);

            try {
                mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                e.printStackTrace();
            }
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {

                    if(use_fingerprint){
                        AuthenticationHelper ah = new AuthenticationHelper(NoteListActivity.this);
                        ah.listener(mCipher,CipherEngine.DEFAULT_KEY_NAME,
                                getApplicationContext().getString(R.string.edit),
                                position);
                    }

                    else{
                        AuthenticationHelper ah = new AuthenticationHelper(NoteListActivity.this);
                        ah.listener(getApplicationContext().getString(R.string.edit), position);      // only password based authentication
                    }


                }
                else {

                    if(use_fingerprint) {
                        AuthenticationHelper ah = new AuthenticationHelper(NoteListActivity.this);
                        ah.listener(mCipher, CipherEngine.DEFAULT_KEY_NAME,
                                getApplicationContext().getString(R.string.delete),
                                position);
                    }
                    else{
                        AuthenticationHelper ah = new AuthenticationHelper(NoteListActivity.this);
                        ah.listener(getApplicationContext().getString(R.string.delete), position);
                    }
                }
            }
        });
        builder.show();
    }


    public void startNoteActivity(int id, int position){
        Intent it = new Intent(NoteListActivity.this,NoteActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.putExtra("id",id);
        it.putExtra("position",position);
        startActivity(it);
    }

    /**
     * Toggling list and empty notes view
     */
    public static void toggleEmptyNotes() {
        // you can check notesList.size() > 0

        Log.i("info",String.valueOf(db.getNotesCount()));
        if (db.getNotesCount() > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        new MaterialDialog.Builder(NoteListActivity.this)
                .title("Application Exit")
                .content("Are you sure you want to exit?")
                .positiveText("Yes")
                .negativeText("No")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                        System.exit(0);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();

    }



}


