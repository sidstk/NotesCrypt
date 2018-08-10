package com.example.sid.NotesCrypt.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.sid.NotesCrypt.utils.AuthenticationHelper;
import com.example.sid.NotesCrypt.utils.CipherEngine;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.fragments.BottomSheetFragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity{




    private TextInputEditText pwd;
    private TextInputEditText repwd;
    private TextInputLayout pwdInputLayout;
    private TextInputLayout repwdInputLayout;
    private CheckBox checkBox;

    private InputMethodManager inputMethodManager;
    private SharedPreferences shp;

    Runnable r1,r2;
    Handler h1;
    KeyguardManager keyguardManager;
    FingerprintManager fingerprintManager;

    public static class CreateKeys extends AsyncTask<String,Void,Void>{
        private WeakReference<Context> contextWeakReference;
        private ProgressDialog progress;

        private CreateKeys(WeakReference<Context> contextWeakReference){
            this.contextWeakReference = contextWeakReference;
        }

        private void generatePrivateKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            CipherEngine.privalias,
                            KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            .setKeySize(2048)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                            .build());

            keyPairGenerator.generateKeyPair();
        }

        private void generateSecretKey(final String pass) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, InvalidParameterSpecException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, IllegalBlockSizeException, NoSuchProviderException, BadPaddingException {
            byte[] salt = new byte[32];         // salt must be atleast 32 bytes long
            final int iterationCount = 4000;

            final Context context = contextWeakReference.get();
            final SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(CipherEngine.PBE_ALGORITHM);
            final PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, iterationCount,
                    256);

            final SecretKey secretKey = factory.generateSecret(keySpec);

            keySpec.clearPassword();

            final SecretKey secret = new SecretKeySpec(secretKey.getEncoded(), "AES");


            final SharedPreferences shp = context.getApplicationContext().getSharedPreferences(context.getApplicationContext().getString(R.string.shred_preference),
                    Context.MODE_PRIVATE);

            Cipher ecipher = Cipher.getInstance(CipherEngine.RSA_ALGORITHM);
            ecipher.init(Cipher.ENCRYPT_MODE,extractkey());
            shp.edit().putString("salt",Base64.encodeToString(ecipher.doFinal(salt), Base64.NO_WRAP)).apply();


            //shp.edit().putString("secretkey",Base64.encodeToString(ecipher.wrap(secret), Base64.NO_WRAP)).apply();

            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            keyStore.setEntry(
                    CipherEngine.AES_KEY,
                    new KeyStore.SecretKeyEntry(secret),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build());
        }

        private PublicKey extractkey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return keyStore.getCertificate(CipherEngine.privalias).getPublicKey();
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                generatePrivateKey();
                generateSecretKey(strings[0]);
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidParameterSpecException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


            progress = new ProgressDialog(contextWeakReference.get());
            progress.setMessage("Generating secure keys...");
            progress.setCancelable(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progress.dismiss();
            Intent it = new Intent(contextWeakReference.get(),NoteListActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            contextWeakReference.get().startActivity(it);

        }
    }



    boolean isKeyguardSecure(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        }
        else{
            return isPassOrPinSet() || isPatternSet();
        }

    }

    private boolean isPatternSet() {
        ContentResolver cr = getContentResolver();
        try {
            int lockPatternEnable = Settings.Secure.getInt(cr, Settings.Secure.LOCK_PATTERN_ENABLED);
            return lockPatternEnable == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean isPassOrPinSet() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    boolean hasEnrolledFingerprints(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return fingerprintManager.hasEnrolledFingerprints();
        }
        else{
            FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(this);
            return fingerprintManagerCompat.hasEnrolledFingerprints();
        }
    }

    boolean isHardwareDetected(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return  fingerprintManager.isHardwareDetected();
        }
        else{
            FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(this);
            return fingerprintManagerCompat.isHardwareDetected();
        }
    }



    private boolean doubleCheck(){

        if(TextUtils.isEmpty(pwd.getText())){
            pwdInputLayout.setError(getString(R.string.password_empty));
            if(TextUtils.isEmpty(repwd.getText())){
                repwdInputLayout.setError(getString(R.string.password_empty));
            }
            return false;
        }
        else if(TextUtils.isEmpty(repwd.getText())){
            repwdInputLayout.setError(getString(R.string.password_empty));
            return false;
        }

        if(TextUtils.equals(pwd.getText(),repwd.getText()))
            return true;

        else {
            repwdInputLayout.setError(getString(R.string.password_no_match));
            return false;
        }
    }

    public void clickF(View v) {


        SharedPreferences shp = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shred_preference),
                Context.MODE_PRIVATE);


        if (shp.getBoolean(getString(R.string.first_run), true)) {       //handle user first time registration
            //Log.i("first_run", "true");
            if(doubleCheck()){
                shp.edit().putBoolean(getString(R.string.first_run), false).apply();
                if (checkBox.isChecked())
                    shp.edit().putBoolean(getString(R.string.use_fingerprint_future), true).apply();
                else
                    shp.edit().putBoolean(getString(R.string.use_fingerprint_future), false).apply();

                doubleCheck();
                if(shp.getBoolean(getString(R.string.fingerprint),true) && shp.getBoolean(getString(R.string.use_fingerprint_future),true)){
                    AuthenticationHelper.createKey(CipherEngine.DEFAULT_KEY_NAME,true,true);
                }

                new CreateKeys(new WeakReference<Context>(this)).execute(pwd.getText().toString());
            }
        } else {
            //handle user login
            //Log.i("first_run", "false");
            if(shp.getBoolean(getString(R.string.fingerprint),true) && shp.getBoolean(getString(R.string.use_fingerprint_future),true)){
                AuthenticationHelper.createKey(CipherEngine.DEFAULT_KEY_NAME,true,true);
            }
            checkPassword();

        }
    }

    private void checkPassword(){
        if (!TextUtils.isEmpty(pwd.getText())) {

            try {
                if (CipherEngine.check_password(pwd.getText().toString(), new WeakReference<Context>(this))) {
                    pwd.setText("");
                    Intent it = new Intent(MainActivity.this, NoteListActivity.class);
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                } else {

                    pwd.setText("");
                    pwdInputLayout.setError(getString(R.string.password_wrong));
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
        } else {
            pwdInputLayout.setError(getString(R.string.password_empty));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {


            if(isKeyguardSecure()){
                //Log.i("intent","secure");

                h1.removeCallbacks(r2);
                ChangeLayouts();
            }
            else{
                //Log.i("intent","not secure");
                h1.postDelayed(r2,0);
            }
        }
        else if(requestCode == 2){
            if(!hasEnrolledFingerprints()){
                h1.postDelayed(r2,0);
            }
            else{
                ChangeLayouts();
                h1.removeCallbacks(r2);
            }

        }

    }


    public void onFailed(){
        pwd.requestFocus();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }
        },500);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        shp = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shred_preference),
                Context.MODE_PRIVATE);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        handlers();



        /*if (!fingerprintManager.isHardwareDetected()) {

            //textView.setText("Your device doesn't support fingerprint authentication");
            shp.edit().putBoolean("fingerprint", false).apply();

        }
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            //textView.setText("No fingerprint configured. Please register at least one fingerprint in your device's Settings");

        }

        if (!keyguardManager.isDeviceSecure()) {

            //textView.setText("Please enable lockscreen security in your device's Settings");
        }*/


    }

    private void ChangeLayouts(){
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if(shp.getBoolean(getApplicationContext().getString(R.string.first_run),true)){
            setContentView(R.layout.activity_register);
            pwdInputLayout = findViewById(R.id.pwdInputLayout);
            repwdInputLayout = findViewById(R.id.repwdInputLayout);
            pwd  = findViewById(R.id.pwd);
            repwd = findViewById(R.id.repwd);
            checkBox = findViewById(R.id.checkBox);
            if(!shp.getBoolean(getString(R.string.fingerprint),false)){
                checkBox.setVisibility(View.GONE);
            }

            pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if(i == EditorInfo.IME_ACTION_GO){
                        if(TextUtils.isEmpty(pwd.getText())){
                            pwdInputLayout.setError(getString(R.string.password_empty));
                        }
                        else{
                            pwdInputLayout.setError(null);
                            repwd.requestFocus();
                        }
                        return true;
                    }
                    return false;
                }
            });

            repwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if(i == EditorInfo.IME_ACTION_GO){
                        if(TextUtils.isEmpty(repwd.getText())){
                            repwdInputLayout.setError(getString(R.string.password_empty));
                        }
                        else{
                            findViewById(R.id.button).setEnabled(false);
                            clickF(textView.getRootView());
                        }
                        return true;
                    }
                    return false;
                }
            });

            pwd.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    pwdInputLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }


            });

            repwd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    repwdInputLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });

            pwd.requestFocus();
            pwd.postDelayed(new Runnable() {
                @Override
                public void run() {
                    inputMethodManager.showSoftInput(pwd,0);

                }
            },500);

        }

        else
        {
            setContentView(R.layout.activity_login);
            pwdInputLayout = findViewById(R.id.pwdInputLayout);
            pwd  = findViewById(R.id.pwd);


            /*pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                    if (actionId == EditorInfo.IME_ACTION_GO){
                        checkPassword();
                        return true;
                    }
                    return false;
                }
            });*/

            if( !shp.getBoolean(getString(R.string.fingerprint),true) )
                findViewById(R.id.fpToggle).setVisibility(View.GONE);

            if( !shp.getBoolean(getString(R.string.use_fingerprint_future),true)){
                findViewById(R.id.fpToggle).setVisibility(View.GONE);
                pwd.requestFocus();
                pwd.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inputMethodManager.showSoftInput(pwd,0);
                    }
                },500);

            }
            else{

                AuthenticationHelper.createKey(CipherEngine.DEFAULT_KEY_NAME,true,true);

                pwd.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();
                        bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
                    }
                },200);
            }


            findViewById(R.id.fpToggle).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();
                    bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());

                }
            });

            pwd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    pwdInputLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });



        }
    }

    private void handlers(){

        h1 = new Handler();


        r1 = new Runnable() {
            @Override
            public void run() {
                if(!isHardwareDetected()){
                    shp.edit().putBoolean(getString(R.string.fingerprint), false).apply();
                    h1.removeCallbacks(r1);

                    h1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final Runnable r = this;
                            if(!isKeyguardSecure()){

                                //Log.i("info","showing dialog");
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Device is not secure!!")
                                        .content("Please enable lockscreen security in your device's Settings")
                                        .positiveText("Setup")
                                        .negativeText("Exit")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                                                r2 = r;
                                                startActivityForResult(intent,1);
                                            }
                                        })
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                //h2.removeCallbacks(r2);
                                                h1.removeCallbacks(r);
                                                finish();
                                                System.exit(0);
                                            }
                                        })
                                        .cancelable(false)
                                        .show();
                            }
                            else{
                                //Log.i("info","exiting handler");
                                //h2.removeCallbacks(this);
                                h1.removeCallbacks(r);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ChangeLayouts();
                                    }
                                });
                            }
                        }
                    }, 0);



                }
                else
                {
                    //Fingerprint hardware detected
                    shp.edit().putBoolean(getString(R.string.fingerprint), true).apply();
                    h1.removeCallbacks(r1);

                    h1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final Runnable r = this;
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){
                                //Log.i("fingerprint","permission missing");
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Fingerprint permission missing!!")
                                        .content("This app needs fingerprint permission to work properly and securely")
                                        .positiveText("Settings")
                                        .negativeText("Exit")
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                h1.removeCallbacks(r);
                                                finish();
                                                System.exit(0);
                                            }
                                        })
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                Intent it = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                Uri uri = Uri.fromParts("package",getPackageName(),null);
                                                r2 =r;
                                                it.setData(uri);
                                                startActivity(it);

                                            }
                                        })
                                        .cancelable(false)
                                        .show();
                            }
                            else{
                                if (!hasEnrolledFingerprints()) {
                                    new MaterialDialog.Builder(MainActivity.this)
                                            .title("Fingerprints data not found!!")
                                            .content("Please enroll atleast one fingerprint")
                                            .positiveText("Settings")
                                            .negativeText("Exit")
                                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    h1.removeCallbacks(r);
                                                    finish();
                                                    System.exit(0);
                                                }
                                            })
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                                    r2 =r;
                                                    startActivityForResult(intent,2);

                                                }
                                            })
                                            .cancelable(false)
                                            .show();
                                }
                                else{
                                    h1.removeCallbacks(r);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ChangeLayouts();
                                        }
                                    });

                                }

                            }
                        }
                    }, 0);
                }
            }
        };
        h1.postDelayed(r1,0);
    }
}
