package org.peercast.pecaport.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 値、または例外
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class Either <T, E extends Exception>{

    private T mValue;
    private E mException;

    private Either(T val){
        mValue = val;
    }

    private Either(E ex){
        mException = ex;
    }

    public static <T, E extends Exception> Either<T, E> value(@Nullable T v){
        return new Either<>(v);
    }

    public static <T, E extends Exception> Either<T, E> except(@NonNull E e){
        return new Either<>(e);
    }

    @Nullable
    public T tryGet() throws E {
        if (mException != null)
            throw mException;
        return mValue;
    }

}
