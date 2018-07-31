package com.example.sid.NotesCrypt.fingerprint;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sid.NotesCrypt.AuthenticationHelper;
import com.example.sid.NotesCrypt.CipherEngine;
import com.example.sid.NotesCrypt.NoteActivity;
import com.example.sid.NotesCrypt.NoteListActivity;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.SettingsActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.example.sid.NotesCrypt.MainActivity.DEFAULT_KEY_NAME;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class FingerprintAuthenticationDialogFragment extends DialogFragment
        implements TextView.OnEditorActionListener, FingerprintUiHelper.Callback {

    private Button mCancelButton;
    private Button mSecondDialogButton;
    private View mFingerprintContent;
    private View mBackupContent;
    private EditText mPassword;
    private CheckBox mUseFingerprintFutureCheckBox;
    private TextView mPasswordDescriptionTextView;
    private TextView mNewFingerprintEnrolledTextView;

    private Stage mStage = Stage.FINGERPRINT;

    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private Activity activity;
    private WeakReference<Context> contextWeakReference;

    private InputMethodManager mInputMethodManager;
    private SharedPreferences mSharedPreferences;
    private String mCause;
    private int listPosition;
    private boolean authenticated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.sign_in));
        authenticated = false;
        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
        mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dismiss();
            }
        });

        mSecondDialogButton = (Button) v.findViewById(R.id.second_dialog_button);
        mSecondDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStage == Stage.FINGERPRINT) {
                    goToBackup();
                } else {
                    try {
                        verifyPassword();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (UnrecoverableKeyException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mFingerprintContent = v.findViewById(R.id.fingerprint_container);
        mBackupContent = v.findViewById(R.id.backup_container);
        mPassword = (EditText) v.findViewById(R.id.oldpassword);
        mPassword.setOnEditorActionListener(this);
        mPasswordDescriptionTextView = (TextView) v.findViewById(R.id.password_description);
        mUseFingerprintFutureCheckBox = (CheckBox)
                v.findViewById(R.id.use_fingerprint_in_future_check);
        mNewFingerprintEnrolledTextView = (TextView)
                v.findViewById(R.id.new_fingerprint_enrolled_description);



        mFingerprintUiHelper = new FingerprintUiHelper(contextWeakReference.get().getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);

        updateStage();

        // If fingerprint authentication is not available, switch immediately to the backup
        // (password) screen.
        if (!mFingerprintUiHelper.isFingerprintAuthAvailable()) {
            Log.i("info","calling backup");
            goToBackup();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStage == Stage.FINGERPRINT) {
            mFingerprintUiHelper.startListening(mCryptoObject);
        }
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }


    public void setCause(String cause){
        mCause = cause;
    }

    public void setListPosition(int position){
        listPosition = position;
    }

    public void setWeakReference(WeakReference<Context> contextWeakReference){
        this.contextWeakReference = contextWeakReference;
    }


    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);



        activity = (Activity) context;

        mInputMethodManager = context.getSystemService(InputMethodManager.class);
        mSharedPreferences = context.getSharedPreferences("dataa", Context.MODE_PRIVATE);
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    /**
     * Switches to backup (password) screen. This either can happen when fingerprint is not
     * available or the user chooses to use the password authentication method by pressing the
     * button. This can also happen when the user had too many fingerprint attempts.
     */
    private void goToBackup() {
        mStage = Stage.PASSWORD;
        updateStage();
        mPassword.requestFocus();

        // Show the keyboard.
        mPassword.postDelayed(mShowKeyboardRunnable, 500);

        // Fingerprint is not used anymore. Stop listening for it.
        mFingerprintUiHelper.stopListening();
    }

    /**
     * Checks whether the current entered password is correct, and dismisses the the dialog and
     * let's the activity know about the result.
     */
    private void verifyPassword() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, UnrecoverableKeyException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, CertificateException, IOException, InvalidAlgorithmParameterException {

        if(!TextUtils.isEmpty(mPassword.getText())){
            if (!CipherEngine.check_password(mPassword.getText().toString(), contextWeakReference)) {

                Log.i("pwd","password false");
                mPassword.setText("");
                mPassword.setError("wrong credential");
                return;
            }

            if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                //editor.putBoolean(getString(R.string.use_fingerprint_to_authenticate_key), mUseFingerprintFutureCheckBox.isChecked());
                editor.putBoolean("use_fingerprint_future", mUseFingerprintFutureCheckBox.isChecked());

                editor.apply();

                if (mUseFingerprintFutureCheckBox.isChecked()) {
                    // Re-create the key so that fingerprints including new ones are validated.
                    AuthenticationHelper.createKey(NoteListActivity.DEFAULT_KEY_NAME, true,true);
                    mStage = Stage.FINGERPRINT;
                }
            }

            onAuthenticated();
        }

        else{
            mPassword.setText("");
            mPassword.setError("empty password");
        }

    }

    /**
     * @return true if {@code password} is correct, false otherwise
     */


    private final Runnable mShowKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            mInputMethodManager.showSoftInput(mPassword, 0);
        }
    };

    private void updateStage() {
        switch (mStage) {
            case FINGERPRINT:
                mCancelButton.setText(R.string.cancel);
                mSecondDialogButton.setText(R.string.use_password);
                mFingerprintContent.setVisibility(View.VISIBLE);
                mBackupContent.setVisibility(View.GONE);
                break;
            case NEW_FINGERPRINT_ENROLLED:
                // Intentional fall through
            case PASSWORD:
                mCancelButton.setText(R.string.cancel);
                mSecondDialogButton.setText(R.string.ok);
                mFingerprintContent.setVisibility(View.GONE);
                mBackupContent.setVisibility(View.VISIBLE);
                if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
                    mPasswordDescriptionTextView.setVisibility(View.GONE);
                    mNewFingerprintEnrolledTextView.setVisibility(View.VISIBLE);
                    mUseFingerprintFutureCheckBox.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            try {
                verifyPassword();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onAuthenticated() {
        // Callback from FingerprintUiHelper. Let the activity know that authentication was
        // successful.
        //noteListActivity.onPurchased(true /* withFingerprint */, mCryptoObject);

        /*if(mCause == "edit")
        noteListActivity.startNoteActivity(noteListActivity.notesList.get(listPosition).getId(),listPosition);

        else if(mCause == "delete")
        noteListActivity.deleteNote(listPosition);
            dismiss();*/
        authenticated = true;
        Context context = contextWeakReference.get();

        if(context instanceof NoteListActivity){

            NoteListActivity noteList = (NoteListActivity) context;

            if(mCause == "edit")
                noteList.startNoteActivity(NoteListActivity.notesList.get(listPosition).getId(),listPosition);

            else if(mCause == "delete")
                noteList.deleteNote(listPosition);
        }

        else if(context instanceof NoteActivity){
            NoteActivity noteActivity = (NoteActivity) context;
            if(mCause == "save"){
                Log.i("info","save");
                noteActivity.save();
            }
            else if(mCause == "delete"){
                noteActivity.delete();
            }
            else if(mCause == "update"){
                noteActivity.update();
            }
        }

        else if(context instanceof SettingsActivity){
            if(mCause == "fp_true"){
                Log.i("info","fp_true");
                mSharedPreferences.edit().putBoolean(getString(R.string.use_fingerprint_future), true).apply();
                AuthenticationHelper.createKey(DEFAULT_KEY_NAME, true, true);
            }

            else if(mCause == "fp_false"){
                Log.i("info","fp_false");
                mSharedPreferences.edit().putBoolean(getString(R.string.use_fingerprint_future), false).apply();
            }
        }


        /*if(activity.getLocalClassName().equals("SettingsActivity")){

           if(mCause == "fp_true"){
               Log.i("info","fp_true");
               mSharedPreferences.edit().putBoolean(getString(R.string.use_fingerprint_future), true).apply();
               AuthenticationHelper authenticationHelper = new AuthenticationHelper(activity);
               authenticationHelper.createKey(DEFAULT_KEY_NAME, true, true);
           }

           else if(mCause == "fp_false"){
               Log.i("info","fp_false");
               mSharedPreferences.edit().putBoolean(getString(R.string.use_fingerprint_future), false).apply();
           }
        }

        if(NoteListActivity.instance != null){
            Log.i("info","notelist instance not null");

            NoteListActivity noteList = (NoteListActivity) NoteListActivity.instance;

            if(mCause == "edit")
                noteList.startNoteActivity(noteListActivity.notesList.get(listPosition).getId(),listPosition);

            else if(mCause == "delete")
                noteList.deleteNote(listPosition);
            NoteListActivity.instance = null;
        }


        if(NoteActivity.instance!=null) {

            NoteActivity noteActivity = NoteActivity.instance;
            if(mCause == "save"){
            Log.i("info","save");
                    noteActivity.save();
            }
            else if(mCause == "delete"){
                noteActivity.delete();
            }
            else if(mCause == "update"){
                noteActivity.update();
            }
            NoteActivity.instance = null;
        }*/

        dismiss();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.i("info","ondismiss");

        if(!authenticated){
            Context context = contextWeakReference.get();
            if(context instanceof SettingsActivity){
                SettingsActivity settingsActivity = (SettingsActivity) context;
                if(mCause == "fp_true")
                    settingsActivity.aSwitch.setChecked(false);
                if(mCause == "fp_false")
                    settingsActivity.aSwitch.setChecked(true);
            }

        }

        super.onDismiss(dialog);
    }

    @Override
    public void onError() {
        goToBackup();
    }

    /**
     * Enumeration to indicate which authentication method the user is trying to authenticate with.
     */

    public enum Stage {
        FINGERPRINT,
        NEW_FINGERPRINT_ENROLLED,
        PASSWORD
    }
}

