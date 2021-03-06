package com.schibsted.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import com.schibsted.android.sdk.exceptions.SPiDKeychainException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

/**
 * Helper class used to securely encrypt/decrypt access token to SharedPreferences
 */
public class SPiDKeychain {
    protected static final String UTF8 = "utf-8";

    /**
     * Encrypts access token and saves it to SharedPreferences
     *
     * @param context       Android context used to generate key for SharedPreferences
     * @param encryptionKey Key used to encrypt the access token
     * @param accessToken   Access token to be saved
     */
    protected static void encryptAccessTokenToSharedPreferences(Context context, String encryptionKey, SPiDAccessToken accessToken) {
        SPiDLogger.log("Saving: " + accessToken.getAccessToken() + ", " + Long.toString(accessToken.getExpiresAt().getTime()) + ", " + accessToken.getRefreshToken() + ", " + accessToken.getUserID());
        SharedPreferences secure = context.getSharedPreferences(context.getPackageName() + ".sdk", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = secure.edit();
        try {
            editor.putString("access_token", encryptString(context, encryptionKey, accessToken.getAccessToken()));
            editor.putString("expires_at", encryptString(context, encryptionKey, Long.toString(accessToken.getExpiresAt().getTime())));
            editor.putString("refresh_token", encryptString(context, encryptionKey, accessToken.getRefreshToken()));
            editor.putString("user_id", encryptString(context, encryptionKey, accessToken.getUserID()));
        } catch (GeneralSecurityException e) {
            clearAccessTokenFromSharedPreferences(context);
            throw new SPiDKeychainException("GeneralSecurityException", e);
        } catch (UnsupportedEncodingException e) {
            clearAccessTokenFromSharedPreferences(context);
            throw new SPiDKeychainException("UnsupportedEncodingException", e);
        }
        editor.commit();
    }

    /**
     * Decrypts access token from SharedPreferences
     *
     * @param context       Android context used to generate name for SharedPreferences
     * @param encryptionKey Key used to decrypt the access token
     * @return Access token if found, otherwise null
     */
    protected static SPiDAccessToken decryptAccessTokenFromSharedPreferences(Context context, String encryptionKey) {
        SharedPreferences secure = context.getSharedPreferences(context.getPackageName() + ".sdk", Context.MODE_PRIVATE);
        if (secure.contains("access_token")) {
            SPiDAccessToken token;
            try {
                String accessToken = decryptString(context, encryptionKey, secure.getString("access_token", ""));
                Long expiresAt = new Long(decryptString(context, encryptionKey, secure.getString("expires_at", "")));
                String refreshToken = decryptString(context, encryptionKey, secure.getString("refresh_token", ""));
                String userId = decryptString(context, encryptionKey, secure.getString("user_id", ""));
                token = new SPiDAccessToken(accessToken, expiresAt, refreshToken, userId);
            } catch (GeneralSecurityException e) {
                clearAccessTokenFromSharedPreferences(context);
                throw new SPiDKeychainException("GeneralSecurityException", e);
            } catch (UnsupportedEncodingException e) {
                clearAccessTokenFromSharedPreferences(context);
                throw new SPiDKeychainException("UnsupportedEncodingException", e);
            }
            return token;
        } else {
            return null;
        }
    }

    /**
     * Clears access token from SharedPreferences
     *
     * @param context Android context used to generate name for SharedPreferences
     */
    protected static void clearAccessTokenFromSharedPreferences(Context context) {
        SharedPreferences secure = context.getSharedPreferences(context.getPackageName() + ".sdk", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = secure.edit();
        editor.remove("access_token");
        editor.remove("expires_at");
        editor.remove("refresh_token");
        editor.remove("user_id");
        editor.commit();
    }

    /**
     * Encrypts a string using the "PBEWithMD5AndDES" algorithm
     *
     * @param context       Android context used to generate encryption salt
     * @param encryptionKey Key used to encrypt the access token
     * @param value         String to be encrypted
     * @return Encrypted string
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    private static String encryptString(Context context, String encryptionKey, String value) throws GeneralSecurityException, UnsupportedEncodingException {
        final byte[] bytes = value != null ? value.getBytes(UTF8) : new byte[0];
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(encryptionKey.toCharArray()));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(), Settings.System.ANDROID_ID).getBytes(UTF8), 20));
        return new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP), UTF8);
    }

    /**
     * Decrypts a string using the "PBEWithMD5AndDES" algorithm
     *
     * @param context       Android context used to generate decrypt salt
     * @param encryptionKey Key used to decrypt the access token
     * @param value         String to be decrypted
     * @return Decrypted string
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    private static String decryptString(Context context, String encryptionKey, String value) throws GeneralSecurityException, UnsupportedEncodingException {
        final byte[] bytes = value != null ? Base64.decode(value, Base64.DEFAULT) : new byte[0];
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(encryptionKey.toCharArray()));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(), Settings.System.ANDROID_ID).getBytes(UTF8), 20));
        return new String(pbeCipher.doFinal(bytes), UTF8);
    }
}
