package org.peercast.pecaport;

import android.support.annotation.Nullable;
import android.util.Log;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;

import java.util.concurrent.Future;

/**
 * ポートを開閉する。
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 * */
public class PortManipulator {
    private static final String TAG = "PortManipulator";

    private final OnResultListener mDebugListener = new OnResultListener() {
        @Override
        public void onSuccess(Method m, RemoteService service, PortMapping mapping) {
            Log.d(TAG, "onSuccess: (" + m + ")" + m);
        }

        @Override
        public void onFailure(Method m, RemoteService service, PortMapping mapping, String errMsg) {
            Log.d(TAG, "onFailure: (" + m + ") " + mapping + ", " + errMsg);
        }

    };
    private ControlPoint mControlPoint;
    private RemoteService mService;

    PortManipulator(ControlPoint controlPoint, RemoteService rs) {
        mControlPoint = controlPoint;
        mService = rs;
    }

    /**
     * 非同期で指定のIPとポートを開放する。
     * <P>推奨: Future.get()で完了を待機する。<br>
     * そのままではControlPoint#execute()内で例外が起きても無視するため。</P>
     */
    public Future addPort(final PortMapping mapping, OnResultListener l) {
        final OnResultListener listener = l != null ? l : mDebugListener;
        return mControlPoint.execute(new PortMappingAdd(mService, mapping) {
            @Override
            public void success(ActionInvocation invocation) {
                listener.onSuccess(OnResultListener.Method.Add, mService, mapping);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                listener.onFailure(OnResultListener.Method.Add, mService, mapping, defaultMsg);
            }
        });
    }

    public Future deletePort(final PortMapping pm, @Nullable OnResultListener l) {
        final OnResultListener listener = l != null ? l : mDebugListener;

        return mControlPoint.execute(new PortMappingDelete(mService, pm) {
            @Override
            public void success(ActionInvocation invocation) {
                listener.onSuccess(OnResultListener.Method.Delete, mService, pm);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                listener.onFailure(OnResultListener.Method.Delete, mService, pm, defaultMsg);
            }
        });
    }

    public interface OnResultListener {

        /**
         * ポート開閉に成功した
         */
        void onSuccess(Method m, RemoteService service, PortMapping mapping);

        /**
         * ポート開閉に失敗した
         */
        void onFailure(Method m, RemoteService service, PortMapping mapping, String errMsg);

        enum Method {
            /**
             * @see #addPort(PortMapping, OnResultListener)
             */
            Add,
            /**
             * @see #deletePort(PortMapping, OnResultListener)
             */
            Delete
        }
    }

    /**
     * 何らかの原因により、ポート操作を許可できない。
     */
    public static class DenyException extends Exception {
        DenyException(String msg) {
            super(msg);
        }
    }
}
