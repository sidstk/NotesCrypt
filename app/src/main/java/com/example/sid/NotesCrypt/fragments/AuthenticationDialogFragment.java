package com.example.sid.NotesCrypt.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.sid.NotesCrypt.fingerprint.FingerprintUiHelper;
import com.example.sid.NotesCrypt.utils.AuthenticationHelper;
import com.example.sid.NotesCrypt.utils.CipherEngine;
import com.example.sid.NotesCrypt.activity.NoteActivity;
import com.example.sid.NotesCrypt.activity.NoteListActivity;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.activity.SettingsActivity;

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


/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class AuthenticationDialogFragment extends DialogFragment
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
    private WeakReference<Activity> activity;
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
        mCancelButton = v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dismiss();
            }
        });

        mSecondDialogButton =  v.findViewById(R.id.second_dialog_button);
        mSecondDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
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
        mPassword =  v.findViewById(R.id.oldpassword);
        mPassword.setOnEditorActionListener(this);
        mPasswordDescriptionTextView =  v.findViewById(R.id.password_description);
        mUseFingerprintFutureCheckBox = v.findViewById(R.id.use_fingerprint_in_future_check);
        mNewFingerprintEnrolledTextView = v.findViewById(R.id.new_fingerprint_enrolled_description);



        mFingerprintUiHelper = new FingerprintUiHelper(activity.get().getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);

        updateStage();

        // If fingerprint authentication is not available, switch immediately to the backup
        // (password) screen.
        if (!mFingerprintUiHelper.isFingerprintAuthAvailable()) {
            //Log.i("info","calling backup");
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



    @Override
    public void onPause() {
        super.onPause();
        //Log.i("info","onpause");
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        activity = new WeakReference<>(getActivity());
        contextWeakReference = new WeakReference<>(context);
        mInputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mSharedPreferences = context.getApplicationContext().
                getSharedPreferences(context.getApplicationContext().getString(R.string.shred_preference),
                Context.MODE_PRIVATE);
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


    private void verifyPassword() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, UnrecoverableKeyException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, CertificateException, IOException, InvalidAlgorithmParameterException {

        if(!TextUtils.isEmpty(mPassword.getText())){
            if (!CipherEngine.check_password(mPassword.getText().toString(), contextWeakReference)) {

                //Log.i("pwd","password false");
                mPassword.setText("");
                mPassword.setError("wrong credential");
                return;
            }

            if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
                Context context = contextWeakReference.get();
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(context.getApplicationContext().getString(R.string.use_fingerprint_future), mUseFingerprintFutureCheckBox.isChecked());

                editor.apply();

                if (mUseFingerprintFutureCheckBox.isChecked()) {
                    // Re-create the key so that fingerprints including new ones are validated.
                    AuthenticationHelper.createKey(CipherEngine.DEFAULT_KEY_NAME,
                            true,true);
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


    private final Runnable mShowKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            //mInputMethodManager.showSoftInput(mPassword, 0);
            mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
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
                mPassword.postDelayed(mShowKeyboardRunnable, 200);
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

        authenticated = true;
        Context context = contextWeakReference.get();


        if(context instanceof NoteListActivity){

            NoteListActivity noteList = (NoteListActivity) context;
            NoteListActivity.foregroundSessionTimeout = false;
            if(mCause.equals(context.getApplicationContext().getString(R.string.edit)))
                noteList.startNoteActivity(NoteListActivity.notesList.get(listPosition).getId(),listPosition);

            else if(mCause.equals(context.getApplicationContext().getString(R.string.delete)))
                NoteListActivity.deleteNote(listPosition);
        }

        else if(context instanceof NoteActivity){
            NoteActivity noteActivity = (NoteActivity) context;

            if(mCause.equals(context.getApplicationContext().getString(R.string.save))){

                noteActivity.save();
            }
            else if(mCause.equals(context.getApplicationContext().getString(R.string.delete))){
                noteActivity.delete();
            }
            else if(mCause.equals(context.getApplicationContext().getString(R.string.update))){
                noteActivity.update();
            }
        }

        else if(context instanceof SettingsActivity){
            if(mCause.equals(contextWeakReference.get().getApplicationContext().getString(R.string.fp_true))){
                //Log.i("info","fp_true");
                mSharedPreferences.edit().putBoolean(getString(R.string.use_fingerprint_future), true).apply();
                AuthenticationHelper.createKey(CipherEngine.DEFAULT_KEY_NAME, true, true);
            }

            else if(mCause.equals(contextWeakReference.get().getApplicationContext().getString(R.string.fp_false))){
                //Log.i("info","fp_false");
                mSharedPreferences.edit().putBoolean(getString(R.string.use_fingerprint_future), false).apply();
            }
        }

        dismiss();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {

        if(!authenticated){
            Context context = contextWeakReference.get();
            if(context instanceof SettingsActivity){
                SettingsActivity settingsActivity = (SettingsActivity) context;
                if(mCause.equals(contextWeakReference.get().getApplicationContext().getString(R.string.fp_true)))
                    settingsActivity.aSwitch.setChecked(false);
                if(mCause.equals(contextWeakReference.get().getApplicationContext().getString(R.string.fp_false)))
                    settingsActivity.aSwitch.setChecked(true);
            }

        }

        super.onDismiss(dialog);
    }


    @Override
    public void onError(final int errId) {

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

