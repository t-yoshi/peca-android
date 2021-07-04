// INotificationEventCallback.aidl
package org.peercast.core;

oneway interface INotificationCallback {
      void onNotifyMessage(int types, String message) = 0;

      void onNotifyChannel(int notifyType, String chId, String jsonChannelInfo) = 1;
}