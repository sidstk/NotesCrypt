package com.example.sid.NotesCrypt.fingerprint;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.sid.NotesCrypt.CipherEngine;
import com.example.sid.NotesCrypt.MainActivity;
import com.example.sid.NotesCrypt.NoteListActivity;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.SettingsActivity;
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
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String AES_KEY = "secret_key";
    private Activity mActivity;
    static List<Note> notes = new ArrayList<>();

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        //mActivity = (Activity)context;
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
                        NoteListActivity.db = null;                  // before deleting database unreferenced all database connections
                        context.deleteDatabase("notes_db");
                        new Engine(new WeakReference<Context>(context), new WeakReference<ChangePasswordFragment>(this)).execute(rePassword.getText().toString());
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


    private static final class Engine extends AsyncTask<String,Void,Void>{
        private SecretKey secret;
        private  final String AES_KEY = "secret_key";
        private  final String AES_KEY_TEMP = "secret_key_temp";
        private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
        private WeakReference<Context> contextWeakReference;
        private WeakReference<ChangePasswordFragment> changePasswordFragmentWeakReference;

        Engine(WeakReference<Context> contextWeakReference,
               WeakReference<ChangePasswordFragment> changePasswordFragmentWeakReference){
            this.contextWeakReference = contextWeakReference;
            this.changePasswordFragmentWeakReference = changePasswordFragmentWeakReference;
        }

        private PublicKey extractkey(Context context) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return  keyStore.getCertificate(context.getString(R.string.privalias)).getPublicKey();


        }
        private void generateKey(final String pass) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, InvalidKeySpecException, InvalidAlgorithmParameterException, UnrecoverableKeyException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidParameterSpecException, DestroyFailedException {


            byte[] salt = new byte[32];         // salt must be atleast 32 bytes long
            final int iterationCount = 4000;

            final SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            final PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, iterationCount, 256);

            final SecretKey secretKey = factory.generateSecret(keySpec);

            keySpec.clearPassword();

            secret = new SecretKeySpec(secretKey.getEncoded(), "AES");


            final SharedPreferences shp = contextWeakReference.get().getSharedPreferences("dataa", Context.MODE_PRIVATE);
            Cipher ecipher = Cipher.getInstance(RSA_ALGORITHM);
            ecipher.init(Cipher.ENCRYPT_MODE,extractkey(contextWeakReference.get()));
            shp.edit().putString("salt",Base64.encodeToString(ecipher.doFinal(salt), Base64.NO_WRAP)).apply();


            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if(keyStore.containsAlias(AES_KEY_TEMP))
                keyStore.deleteEntry(AES_KEY_TEMP);

            keyStore.setEntry(
                    AES_KEY_TEMP,
                    new KeyStore.SecretKeyEntry(secret),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(true)
                            .build());

            ChangeEncryptedData();
        }

        private void ChangeEncryptedData() throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException, KeyStoreException, InvalidKeySpecException, IllegalBlockSizeException, InvalidParameterSpecException, DestroyFailedException {
            DatabaseHelper db = new DatabaseHelper(contextWeakReference.get());


            for(int i=0; i<notes.size(); i++){

                long id = db.insertNote(CipherEngine.encryptWithKey(AES_KEY_TEMP, CipherEngine.decrypt(notes.get(i).getNote())),
                        CipherEngine.encryptWithKey(AES_KEY_TEMP, CipherEngine.decrypt(notes.get(i).getTitle())),notes.get(i).getTimestamp());
                NoteListActivity.notesList.add(db.getNote(id));
            }

            ((Activity) contextWeakReference.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NoteListActivity.mAdapter.notifyDataSetChanged();
                }
            });


            db = null;
            RevertKeys();
        }

        private void RevertKeys() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, DestroyFailedException {


            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if(keyStore.containsAlias(AES_KEY_TEMP))
                keyStore.deleteEntry(AES_KEY_TEMP);

            if(keyStore.containsAlias(AES_KEY))
                keyStore.deleteEntry(AES_KEY);

            keyStore.setEntry(
                    AES_KEY,
                    new KeyStore.SecretKeyEntry(secret),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(true)
                            .build());

        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                generateKey(strings[0]);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
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
            changePasswordFragmentWeakReference.get().dismiss();
            super.onPostExecute(aVoid);
        }
    }
}
