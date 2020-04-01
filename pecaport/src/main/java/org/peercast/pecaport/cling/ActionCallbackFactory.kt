package org.peercast.pecaport.cling

import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse


interface ActionCallbackDelegate<R> {
    //FIX: 一部環境でllegalArgumentException: Parameter specified as non-null is null
    //成功でnullが帰ってくるのは納得できないが。
    fun success(result: R?)
    fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse?, defaultMsg: String)
}

abstract class ActionCallbackFactory<R> {
    abstract fun create(delegate: ActionCallbackDelegate<R>): ActionCallback
}

