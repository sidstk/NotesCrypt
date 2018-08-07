package com.example.sid.NotesCrypt.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.fingerprint.AuthenticationDialogFragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AuthenticationHelper {

    private static KeyStore mKeyStore;
    private static KeyGenerator mKeyGenerator;

    private Context mContext;

    public AuthenticationHelper(Context context){
        this.mContext = context;
    }
    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     *
     */


    public static void createKey(String keyName, boolean invalidatedByBiometricEnrollment,boolean fingerprint) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }

        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;

        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    /** Triggering for passwordless authentication, i.e fingerprint **/
    public void listener(final Cipher cipher, final String keyName, final String cause, final int position){
        AuthenticationListener al = new AuthenticationListener(cipher, keyName, cause, position, new WeakReference<Context>(mContext));
        al.trigger();
    }

    /** Triggering for authentication with password **/
    public void listener(final String cause, final int position){
        AuthenticationListener al = new AuthenticationListener(null, null, cause, position, new WeakReference<Context>(mContext));
        al.passwordTrigger();
    }

    private static class AuthenticationListener {

        private Cipher mCipher;
        private String mKeyName;
        private String mCause;
        private int listPosition;
        private AppCompatActivity activity;
        private final String DIALOG_FRAGMENT_TAG = "authenticationFragment";
        private SharedPreferences mSharedPreferences;


        AuthenticationListener(Cipher cipher, String keyName,String cause, int position, WeakReference<Context> contextWeakReference) {
            mCipher = cipher;
            mKeyName = keyName;
            mCause = cause;
            listPosition = position;
            activity = (AppCompatActivity) contextWeakReference.get();
            mSharedPreferences = contextWeakReference.get().getApplicationContext().getSharedPreferences("dataa", Context.MODE_PRIVATE);

        }


        private void trigger() {


            // Set up the crypto object for later. The object will be authenticated by use
            // of the fingerprint.
            if (AuthenticationHelper.initCipher(mCipher, mKeyName)) {


                Log.i("info","key valid");
                AuthenticationDialogFragment fragment
                        = new AuthenticationDialogFragment();
                fragment.setCause(mCause);
                fragment.setListPosition(listPosition);
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));

                boolean useFingerprintPreference = mSharedPreferences.getBoolean(activity.getApplicationContext().getString(R.string.use_fingerprint_future), true);

                if (useFingerprintPreference) {
                    fragment.setStage(
                            AuthenticationDialogFragment.Stage.FINGERPRINT);
                } else {
                    fragment.setStage(
                            AuthenticationDialogFragment.Stage.PASSWORD);
                }

                fragment.show(activity.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }

            else {
                // This happens if the lock screen has been disabled or or a fingerprint got
                // enrolled. Thus show the dialog to authenticate with their password first
                // and ask the user if they want to authenticate with fingerprints in the
                // future
                Log.i("info","key invalid");
                AuthenticationDialogFragment fragment
                        = new AuthenticationDialogFragment();
                fragment.setCause(mCause);
                fragment.setListPosition(listPosition);
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                fragment.setStage(
                        AuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                fragment.show(activity.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        }

        private void passwordTrigger(){
            AuthenticationDialogFragment fragment
                    = new AuthenticationDialogFragment();
            fragment.setCause(mCause);
            fragment.setListPosition(listPosition);
            fragment.setStage(
                    AuthenticationDialogFragment.Stage.PASSWORD);
            fragment.show(activity.getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
    }



}
