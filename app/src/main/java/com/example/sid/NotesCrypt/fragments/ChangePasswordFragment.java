package com.example.sid.NotesCrypt.fragments;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.example.sid.NotesCrypt.utils.CipherEngine;
import com.example.sid.NotesCrypt.activity.NoteListActivity;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.database.DatabaseHelper;
import com.example.sid.NotesCrypt.database.model.Note;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

public class ChangePasswordFragment extends DialogFragment {

    private Button mCancelButton;
    private Button mSecondDialogButton;
    private EditText oldPassword;
    private EditText newPassword;
    private EditText rePassword;
    private ProgressBar pg;
    private Context context;
    private static List<Note> notes = new ArrayList<>();

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_change_password, container, false);
        getDialog().setTitle("Change Password");
        mCancelButton = v.findViewById(R.id.cancel_button);
        mSecondDialogButton = v.findViewById(R.id.second_dialog_button);
        oldPassword = v.findViewById(R.id.oldpassword);
        newPassword = v.findViewById(R.id.newpassword);
        rePassword = v.findViewById(R.id.repassword);
        pg = v.findViewById(R.id.pg);
        mCancelButton.setText(R.string.cancel);
        mSecondDialogButton.setText(R.string.ok);

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });


        mSecondDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPassword();
            }
        });
        return v;
    }

    private void resetPassword(){
        if(!TextUtils.isEmpty(oldPassword.getText())){

            try {
                if(CipherEngine.check_password(oldPassword.getText().toString(), new WeakReference<Context>(context))){
                    if(TextUtils.isEmpty(newPassword.getText())){
                        newPassword.setError(getString(R.string.password_empty));
                        if(TextUtils.isEmpty(rePassword.getText())){
                            rePassword.setError(getString(R.string.password_empty));
                        }
                    }
                    else if(TextUtils.isEmpty(rePassword.getText())){
                        rePassword.setError(getString(R.string.password_empty));
                    }

                    if(TextUtils.equals(newPassword.getText(),rePassword.getText())){
                        /*for(int i=0; i<NoteListActivity.notesList.size(); i++){
                            NoteListActivity.mAdapter.notifyItemRemoved(0);
                        }
                        NoteListActivity.notesList.clear();
                        NoteListActivity.toggleEmptyNotes();*/
                        mCancelButton.setEnabled(false);
                        mSecondDialogButton.setEnabled(false);
                        NoteListActivity.notesList.clear();
                        notes.addAll(NoteListActivity.db.getAllNotes());
                        NoteListActivity.db.close();                  // before deleting database unreferenced all database connections
                        context.getApplicationContext().deleteDatabase("notes_db");
                        new RevertData(new WeakReference<Context>(context),
                                new WeakReference<ChangePasswordFragment>(this)).execute(rePassword.getText().toString());
                        //generateKey(rePassword.getText().toString());
                        //dismiss();
                    }
                    else{
                        rePassword.setError(getString(R.string.password_no_match));
                    }
                }
                else{
                    oldPassword.setError(getString(R.string.password_no_match));
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        }
        else{
            oldPassword.setError(getString(R.string.password_empty));
        }
    }


    private static final class RevertData extends AsyncTask<String,Void,Void>{
        private SecretKey secret;
        private WeakReference<Context> contextWeakReference;
        private WeakReference<ChangePasswordFragment> changePasswordFragmentWeakReference;

        RevertData(WeakReference<Context> contextWeakReference,
                   WeakReference<ChangePasswordFragment> changePasswordFragmentWeakReference){
            this.contextWeakReference = contextWeakReference;
            this.changePasswordFragmentWeakReference = changePasswordFragmentWeakReference;
        }

        private PublicKey extractkey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return  keyStore.getCertificate(CipherEngine.privalias).getPublicKey();


        }

        private void generateKey(final String pass) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, InvalidKeySpecException, InvalidAlgorithmParameterException, UnrecoverableKeyException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidParameterSpecException, DestroyFailedException {


            byte[] salt = new byte[32];         // salt must be atleast 32 bytes long
            final int iterationCount = 4000;

            final SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(CipherEngine.PBE_ALGORITHM);
            final PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, iterationCount, 256);

            final SecretKey secretKey = factory.generateSecret(keySpec);

            keySpec.clearPassword();

            secret = secretKey;

            final Context context = contextWeakReference.get();
            SecretKey key = new SecretKeySpec(secretKey.getEncoded(), "AES");
            final SharedPreferences shp = context.getApplicationContext().getSharedPreferences(context.getApplicationContext().getString(R.string.shred_preference),
                    Context.MODE_PRIVATE);
            Cipher ecipher = Cipher.getInstance(CipherEngine.RSA_ALGORITHM);
            ecipher.init(Cipher.ENCRYPT_MODE,extractkey());
            shp.edit().putString("salt",Base64.encodeToString(ecipher.doFinal(salt), Base64.NO_WRAP)).apply();


            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if(keyStore.containsAlias(CipherEngine.AES_KEY_TEMP))
                keyStore.deleteEntry(CipherEngine.AES_KEY_TEMP);

            keyStore.setEntry(
                    CipherEngine.AES_KEY_TEMP,
                    new KeyStore.SecretKeyEntry(key),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build());

            ChangeEncryptedData();
        }

        private void ChangeEncryptedData() throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException, KeyStoreException, InvalidKeySpecException, IllegalBlockSizeException, InvalidParameterSpecException, DestroyFailedException {
            DatabaseHelper db = new DatabaseHelper(contextWeakReference.get().getApplicationContext());


            /*for(int i=0; i<notes.size(); i++){
                Log.i("Enote", notes.get(i).getNote());
                Log.i("Etitle", notes.get(i).getTitle());
            }*/

            for(int i=0; i<notes.size(); i++){

                long id = db.insertNote(CipherEngine.encryptWithKey(CipherEngine.AES_KEY_TEMP, CipherEngine.decrypt(notes.get(i).getNote())),
                        CipherEngine.encryptWithKey(CipherEngine.AES_KEY_TEMP, CipherEngine.decrypt(notes.get(i).getTitle())),
                        notes.get(i).getTimestamp());
                NoteListActivity.notesList.add(db.getNote(id));

            }

            RevertKeys();

            ((Activity) contextWeakReference.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NoteListActivity.mAdapter.notifyDataSetChanged();
                }
            });

            notes.clear();
            db.close();
            db = null;
        }


        private void RevertKeys() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, DestroyFailedException, InvalidKeySpecException, NoSuchPaddingException, UnrecoverableKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {


            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);


            if(keyStore.containsAlias(CipherEngine.AES_KEY_TEMP))
                keyStore.deleteEntry(CipherEngine.AES_KEY_TEMP);


            if(keyStore.containsAlias(CipherEngine.AES_KEY))
                keyStore.deleteEntry(CipherEngine.AES_KEY);

            final SecretKey key = new SecretKeySpec(secret.getEncoded(), "AES");

            keyStore.setEntry(
                    CipherEngine.AES_KEY,
                    new KeyStore.SecretKeyEntry(key),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build());
            secret = null;
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                generateKey(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidParameterSpecException e) {
                e.printStackTrace();
            } catch (DestroyFailedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            changePasswordFragmentWeakReference.get().pg.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            changePasswordFragmentWeakReference.get().pg.setVisibility(View.GONE);
            //NoteListActivity.db = new DatabaseHelper(contextWeakReference.get().getApplicationContext());
            changePasswordFragmentWeakReference.get().dismiss();
            super.onPostExecute(aVoid);
        }


    }

}



