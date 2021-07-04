// IPeerCastService.aidl
package org.peercast.core;

import org.peercast.core.INotificationCallback;

interface IPeerCastService {
    void registerNotificationCallback(in INotificationCallback callback) = 0;

    void unregisterNotificationCallback(in INotificationCallback callback) = 1;

    int getPort() = 2;
}