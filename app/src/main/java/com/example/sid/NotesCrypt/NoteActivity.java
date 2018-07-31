package com.example.sid.NotesCrypt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sid.NotesCrypt.database.DatabaseHelper;
import com.example.sid.NotesCrypt.database.model.Note;
import com.example.sid.NotesCrypt.fingerprint.FingerprintAuthenticationDialogFragment;
import com.squareup.leakcanary.LeakCanary;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.example.sid.NotesCrypt.NoteListActivity.mAdapter;
import static com.example.sid.NotesCrypt.NoteListActivity.noNotesView;
import static com.example.sid.NotesCrypt.NoteListActivity.notesList;


public class NoteActivity extends AppCompatActivity {

    TextInputEditText noteTitle;
    TextInputEditText noteText;
    private int id;
    private int position;
    private Menu menu;
    private boolean titleChange;
    private boolean noteChnage;
    private Cipher mCipher;
    //public static NoteActivity instance = null;
    private static final String SAVE = "save";
    private static final String UPDATE = "update";
    private static final  String DELETE = "delete";
    public static final String DEFAULT_KEY_NAME = "default_key";
    private boolean use_fingerprint;
    private SharedPreferences mSharedPreferences;



    private static class SaveData extends AsyncTask<String,Void,Void>{

        private ProgressDialog progress;
        private DatabaseHelper db;
        private WeakReference<Context> contextWeakReference;

        SaveData(WeakReference<Context> contextWeakReference){
            this.contextWeakReference = contextWeakReference;
            db = new DatabaseHelper(this.contextWeakReference.get());

        }

        private void createNote(final String note,final String title)  {
            // inserting note in db and getting
            // newly inserted note id
                    long id = 0;
                    try {
                        id = db.insertNote(CipherEngine.encrypt(note), CipherEngine.encrypt(title),null);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (UnrecoverableKeyException e) {
                        e.printStackTrace();
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (InvalidParameterSpecException e) {
                        e.printStackTrace();
                    }

            // get the newly inserted note from db
                    final Note n = db.getNote(id);
                    Context context = contextWeakReference.get();
                    Activity activity = (Activity) context;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (n != null) {
                                // adding new note to array list at 0 position
                                notesList.add(0, n);

                                // refreshing the list
                                mAdapter.notifyDataSetChanged();
                            }

                            NoteListActivity.toggleEmptyNotes();
                        }
                    });

        }

        private void updateNote(String note, String title, final int position) {
            Note n = notesList.get(position);
            // updating note text


            try {
                n.setNote(CipherEngine.encrypt(note));
                n.setTitle(CipherEngine.encrypt(title));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (InvalidParameterSpecException e) {
                e.printStackTrace();
            }

            // updating note in db
            db.updateNote(n);

            // refreshing the list
            notesList.set(position, n);
            ((Activity)contextWeakReference.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyItemChanged(position);
                }
            });


            NoteListActivity.toggleEmptyNotes();
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progress = new ProgressDialog(contextWeakReference.get());
            progress.setMessage("Saving data");
            progress.setCancelable(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            if(strings[0] == SAVE)
            createNote(strings[1],strings[2]);

            else if(strings[0] == UPDATE)
                updateNote(strings[1],strings[2],Integer.parseInt(strings[3]));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progress.dismiss();
            ((Activity)contextWeakReference.get()).finish();
        }
    }




    private void toggleEmptyNotes() {
        // you can check notesList.size() > 0

        if (NoteListActivity.db.getNotesCount() > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }




    private void deleteNote(int id,int position) {

        if(position != -1 && id != 0){
            // deleting the note from db
            NoteListActivity.db.deleteNote(notesList.get(position));

            // removing the note from the list
            notesList.remove(position);
            mAdapter.notifyItemRemoved(position);
        }

        toggleEmptyNotes();
    }

    private boolean checkEmpty(String note, String title){

        if(TextUtils.isEmpty(title)){
            noteTitle.setError("title can't be empty");

            if(TextUtils.isEmpty(note)){
                noteText.setError("can't save empty data");
                return true;
                //noteText.requestFocus();
            }
            noteTitle.requestFocus();
            return true;
        }

        if(TextUtils.isEmpty(note)){
            noteText.setError("can't save empty data");
            noteText.requestFocus();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.delete:

                new AlertDialog.Builder(this)
                        .setTitle("Delete note")
                        .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if(use_fingerprint){
                                    AuthenticationHelper ah = new AuthenticationHelper(NoteActivity.this);
                                    //instance = NoteActivity.this;
                                    ah.listener(mCipher,DEFAULT_KEY_NAME,DELETE, position);
                                }

                                else{
                                    AuthenticationHelper ah = new AuthenticationHelper(NoteActivity.this);
                                    //instance = NoteActivity.this;
                                    ah.listener(DELETE, position);
                                }

                            }
                        })
                        .setNegativeButton("no", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;

            case R.id.edit:
                Log.i("info","edit");
                item.setVisible(false);
                menu.findItem(R.id.done).setVisible(true);
                noteTitle.setFocusableInTouchMode(true);
                noteTitle.setClickable(true);
                noteText.setFocusableInTouchMode(true);
                noteText.setClickable(true);
                return true;

            case R.id.done:
                if(!checkEmpty(noteText.getText().toString(),noteTitle.getText().toString())){
                    if(id==0){
                        new AlertDialog.Builder(NoteActivity.this)
                                .setTitle("Save note")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //createNote(noteText.getText().toString(),noteTitle.getText().toString());
                                        //new SaveData().execute("addNote",noteText.getText().toString(),noteTitle.getText().toString());
                                        //finish();
                                        if(use_fingerprint){
                                            AuthenticationHelper ah = new AuthenticationHelper(NoteActivity.this);
                                            //instance = NoteActivity.this;
                                            ah.listener(mCipher,DEFAULT_KEY_NAME,SAVE, position);
                                        }

                                        else{
                                            AuthenticationHelper ah = new AuthenticationHelper(NoteActivity.this);
                                            //instance = NoteActivity.this;
                                            ah.listener(SAVE, position);
                                        }


                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setCancelable(false)
                                .show();
                    }
                    else{

                        new AlertDialog.Builder(NoteActivity.this)
                                .setTitle("Update note")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //updateNote(noteText.getText().toString(),noteTitle.getText().toString(),position);
                                        //finish();
                                        //new SaveData().execute("updateNote",noteText.getText().toString(),noteTitle.getText().toString(),String.valueOf(position));
                                        if(use_fingerprint){
                                            AuthenticationHelper ah = new AuthenticationHelper(NoteActivity.this);
                                            //instance = NoteActivity.this;
                                            ah.listener(mCipher,DEFAULT_KEY_NAME,UPDATE, position);
                                        }

                                        else{
                                            AuthenticationHelper ah = new AuthenticationHelper(NoteActivity.this);
                                            //instance = NoteActivity.this;
                                            ah.listener(UPDATE, position);
                                        }

                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .show();
                    }
                    return true;
                }
            default: return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem editItem = menu.findItem(R.id.edit);
        MenuItem doneItem = menu.findItem(R.id.done);
        if(id==0)
            doneItem.setVisible(true);
        else
            editItem.setVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    public void save(){
    new SaveData(new WeakReference<Context>(this)).execute(SAVE,noteText.getText().toString(),noteTitle.getText().toString());

    }


    public void delete(){
    deleteNote(id,position);
    finish();
    }

    public void update(){

    new SaveData(new WeakReference<Context>(this)).execute(UPDATE,noteText.getText().toString(),noteTitle.getText().toString(),String.valueOf(position));

    }

    private String getData(String text){
    String result = null;
    try {
        result = CipherEngine.decrypt(text);
    } catch(AEADBadTagException e){
        Toast.makeText(this, "It looks like your message has been changed outside scope", Toast.LENGTH_SHORT).show();
    }
    catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    } catch (InvalidKeySpecException e) {
        e.printStackTrace();
    } catch (NoSuchPaddingException e) {
        e.printStackTrace();
    } catch (InvalidKeyException e) {
        e.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    } catch (BadPaddingException e) {
        e.printStackTrace();
    } catch (IllegalBlockSizeException e) {
        e.printStackTrace();
    } catch (KeyStoreException e) {
        e.printStackTrace();
    } catch (CertificateException e) {
        e.printStackTrace();
    } catch (UnrecoverableKeyException e) {
        e.printStackTrace();
    }
    return result;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        //LeakCanary.install(getApplication());
        mSharedPreferences = getApplicationContext().getSharedPreferences("dataa", Context.MODE_PRIVATE);

        noteTitle = findViewById(R.id.noteTitle);
        noteText = findViewById(R.id.noteText);
        titleChange = false;
        noteChnage = false;
        use_fingerprint = mSharedPreferences.getBoolean("fingerprint",true) && mSharedPreferences.getBoolean("use_fingerprint_future",true);

        final TextView dateView = findViewById(R.id.dateView);


        try {
            mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }



        Intent it = getIntent();
        id = it.getIntExtra("id",0);
        position = it.getIntExtra("position",-1);
        Note note = new Note();

        if(id==0){
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("MMM d");
            String formattedDate = df.format(c);
            dateView.setText(formattedDate);
        }

        else{
            note = NoteListActivity.db.getNote(id);
            noteTitle.setText(getData(note.getTitle()));

            noteText.setText(getData(note.getNote()));

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //fmt.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false,TimeZone.SHORT)));
            try {
                Date date = fmt.parse(note.getTimestamp());
                fmt = new SimpleDateFormat("EEEE MMM d, yyyy HH:mm:ss");
                dateView.setText(fmt.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }


            noteTitle.setFocusable(false);
            noteTitle.setClickable(false);
            noteText.setFocusable(false);
            noteText.setClickable(false);
        }


        noteTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                titleChange = true;
                noteChnage = true;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    @Override
    public void onBackPressed() {

        if(titleChange || noteChnage) {
            new AlertDialog.Builder(NoteActivity.this)
                    .setTitle("Leave current activity")
                    .setMessage("All unsaved data will be lost")
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("Stay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        else
            super.onBackPressed();
    }



}