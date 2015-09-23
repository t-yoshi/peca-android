package org.peercast.pecaport.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 独自のSignature Permissionが、本当に自分のアプリで定義されているかをチェックする。
 * @author T Yoshizawa
 */
public class PackageSignature {
    private static final String TAG = "PackageSignature";

    //BUILD_TYPE=debugの署名
    private static final String SIG_HASH_DEBUG = "chemsQCLU8aOAATOKZ+7d67fYs69EfnGIfcYmB8pPKE=";

    //T Yoshizawaの署名のSha256ハッシュ値
    private static final String SIG_HASH_RELEASE = "V9XKEau3UYkxhmVBuKwTOTG+uUd64aowm/EpS3yBqRc=";

    private final PackageManager mManager;
    private final String mSigHash;
    private final String mPackageName;

    private PackageSignature(PackageManager manager, String packageName) {
        mManager = manager;
        mPackageName = packageName;
        boolean debuggable = false;// BuildConfig.BUILD_TYPE.equals("debug");
        mSigHash = debuggable ? SIG_HASH_DEBUG : SIG_HASH_RELEASE;
        if (debuggable)
            Log.i(TAG, "BuildType: Debuggable");
    }

    /**
     * パッケージ名
     */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * 署名のsha256/base64
     */
    @Nullable
    public String getSha256Hash() {
        try {
            PackageInfo info = mManager.getPackageInfo(mPackageName, PackageManager.GET_SIGNATURES);
            if (info.signatures.length != 1)
                return null;

            Signature sig = info.signatures[0];
            return sha256digest(sig.toByteArray());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * このパッケージは私の署名か
     */
    public boolean validate() {
        return mSigHash.equals(getSha256Hash());
    }


    public static PackageSignature fromDefinedPermission(Context c, @NonNull String permission)
            throws PackageManager.NameNotFoundException {
        PackageManager manager = c.getPackageManager();
        PermissionInfo pi = manager.getPermissionInfo(permission, PackageManager.GET_META_DATA);

        if (pi.protectionLevel != PermissionInfo.PROTECTION_SIGNATURE)
            throw new IllegalArgumentException("ProtectionLevel not signature: " + permission);

        return new PackageSignature(manager, pi.packageName);
    }

    public static PackageSignature fromPackageName(Context c, @NonNull String packageName)
            throws PackageManager.NameNotFoundException {
        PackageManager manager = c.getPackageManager();
        PackageInfo pi = manager.getPackageInfo(packageName, 0);
        return new PackageSignature(manager, pi.packageName);
    }

    private static String sha256digest(byte[] data) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(data);
            //行末無用
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }



}
