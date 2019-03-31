package org.peercast.pecaport.cling

import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse


interface ActionCallbackDelegate<R> {
    fun success(result: R)
    fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String)
}

abstract class ActionCallbackFactory<R> {
    abstract fun create(delegate: ActionCallbackDelegate<R>): ActionCallback
}

//
//object : ActionCallbackFactory<String>(){
//  override fun create(delegate: ActionCallbackDelegate<String>){
//    return object : GetExternalIP(service), ActionCallbackDelegate<String> by delegate {}
//

