package com.example.sid.NotesCrypt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherEngine {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String AES_KEY = "secret_key";
    private static final String test = "testdata";


    private static PrivateKey getPrivKey(Context mContext) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return  (PrivateKey) keyStore.getKey(mContext.getString(R.string.privalias), null);
    }

    private  PublicKey getPubKey(Context mContext) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return  keyStore.getCertificate(mContext.getString(R.string.privalias)).getPublicKey();
    }


    /*public static Key unwrapKey() throws NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        Context context = mContext.get();
        cipher.init(Cipher.UNWRAP_MODE, getPrivKey());
        SharedPreferences shp = context.getSharedPreferences("dataa", Context.MODE_PRIVATE);
        return cipher.unwrap(Base64.decode(shp.getString("secretkey",""),Base64.NO_WRAP),"AES",Cipher.SECRET_KEY);
    }*/

    public static String decrypt(final String text) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException, BadPaddingException, IllegalBlockSizeException, KeyStoreException, CertificateException, UnrecoverableKeyException {

        final Cipher dcipher = Cipher.getInstance(AES_ALGORITHM);

        final byte[] iv = Base64.decode(text.substring(0,16),Base64.NO_WRAP);


        //final Key unwrapsecretkey = unwrapKey();


        //dcipher.init(Cipher.DECRYPT_MODE, unwrapsecretkey, new IvParameterSpec(iv));
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        final SecretKey keyStoreKey = (SecretKey) keyStore.getKey(AES_KEY, null);
        //dcipher.init(Cipher.DECRYPT_MODE, keyStoreKey, new IvParameterSpec(iv));

        dcipher.init(Cipher.DECRYPT_MODE, keyStoreKey, new GCMParameterSpec(128,iv));

        //dcipher.init(Cipher.DECRYPT_MODE, keyStoreKey);
        return new String(dcipher.doFinal(Base64.decode(text.substring(16), Base64.NO_WRAP)),"UTF-8");
    }

    public static String encrypt(final String text) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, CertificateException, UnrecoverableKeyException, KeyStoreException, InvalidParameterSpecException {

        final Cipher ecipher = Cipher.getInstance(AES_ALGORITHM);


        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        final SecretKey keyStoreKey = (SecretKey) keyStore.getKey(AES_KEY, null);

        //ecipher.init(Cipher.ENCRYPT_MODE, keyStoreKey,new IvParameterSpec(IV));


        ecipher.init(Cipher.ENCRYPT_MODE, keyStoreKey, new SecureRandom());
        //final byte[] IV = ecipher.getIV();
        final byte[] IV = ecipher.getParameters().getParameterSpec(GCMParameterSpec.class).getIV();  // AES/GCM/NoPadding requires only 12 byte long IVs

        Log.i("info",Base64.encodeToString(IV,Base64.NO_WRAP));
        return  Base64.encodeToString(IV,
                Base64.NO_WRAP)+Base64.encodeToString(ecipher.doFinal(text.getBytes("UTF-8")),
                Base64.NO_WRAP);

    }

    public static String encryptWithKey(final String alias,final String text) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, CertificateException, UnrecoverableKeyException, KeyStoreException, InvalidParameterSpecException {

        final Cipher ecipher = Cipher.getInstance(AES_ALGORITHM);


        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        final SecretKey keyStoreKey = (SecretKey) keyStore.getKey(alias, null);

        //ecipher.init(Cipher.ENCRYPT_MODE, keyStoreKey,new IvParameterSpec(IV));


        ecipher.init(Cipher.ENCRYPT_MODE, keyStoreKey, new SecureRandom());
        //final byte[] IV = ecipher.getIV();
        final byte[] IV = ecipher.getParameters().getParameterSpec(GCMParameterSpec.class).getIV();  // AES/GCM/NoPadding requires only 12 byte long IVs

        Log.i("info",Base64.encodeToString(IV,Base64.NO_WRAP));
        return  Base64.encodeToString(IV,
                Base64.NO_WRAP)+Base64.encodeToString(ecipher.doFinal(text.getBytes("UTF-8")),
                Base64.NO_WRAP);

    }

    public static boolean check_password(String pass, WeakReference<Context> contextWeakReference) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        final int iterationCount = 4000;

        final Context context = contextWeakReference.get();
        final SharedPreferences shp = context.getSharedPreferences("dataa", Context.MODE_PRIVATE);

        final Cipher dcipher = Cipher.getInstance(RSA_ALGORITHM);
        dcipher.init(Cipher.DECRYPT_MODE,getPrivKey(contextWeakReference.get()));

        //Log.i("info",shp.getString("salt",""));
        final byte[] salt = dcipher.doFinal(Base64.decode(shp.getString("salt",""),Base64.NO_WRAP));

        final SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
        final PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, iterationCount, 256);
        final SecretKey secretKey = factory.generateSecret(keySpec);

        keySpec.clearPassword();

        final SecretKey secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

        final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        final SecretKey keyStoreKey = (SecretKey) keyStore.getKey(AES_KEY, null);

        final Cipher c1 = Cipher.getInstance(AES_ALGORITHM);
        c1.init(Cipher.ENCRYPT_MODE, secret, new SecureRandom());
        final byte[] iv = c1.getIV();
        final byte[] enc = c1.doFinal(test.getBytes("UTF-8"));

        c1.init(Cipher.DECRYPT_MODE,keyStoreKey, new GCMParameterSpec(128,iv));

        try{
            byte[] dec = c1.doFinal(enc);
            /*if(Arrays.equals(b2,test.getBytes("UTF-8"))){
                return true;
            }
            else
                return false;*/
            return true;
        }
        catch(AEADBadTagException e){ return false; }



        /*Cipher wcipher = Cipher.getInstance(RSA_ALGORITHM);
        wcipher.init(Cipher.WRAP_MODE,getPubKey());

        byte[] wrapKey = wcipher.wrap(secret);
        byte[] key = unwrapKey().getEncoded();

        //Log.i("wrapkey",Base64.encodeToString(secret.getEncoded(),Base64.NO_WRAP));
        //Log.i("key",Base64.encodeToString(key,Base64.NO_WRAP));
        if(Arrays.equals(secret.getEncoded(),key))
        return true;
        else
            return false;*/
    }

}
