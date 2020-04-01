package org.peercast.pecaport.cling

import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.support.igd.callback.*
import org.fourthline.cling.support.model.Connection
import org.fourthline.cling.support.model.PortMapping


class GetExternalIPFactory(private val service: RemoteService) : ActionCallbackFactory<String>() {
    override fun create(delegate: ActionCallbackDelegate<String>): ActionCallback {
        return object : GetExternalIP(service), ActionCallbackDelegate<String> by delegate {}
    }
}

class GetStatusInfoFactory(private val service: RemoteService) : ActionCallbackFactory<Connection.StatusInfo>() {
    override fun create(delegate: ActionCallbackDelegate<Connection.StatusInfo>): ActionCallback {
        return object : GetStatusInfo(service), ActionCallbackDelegate<Connection.StatusInfo> by delegate {}
    }
}

class PortMappingEntryGetFactory(private val service: RemoteService,
                                 private val index: Long) : ActionCallbackFactory<PortMapping>() {
    override fun create(delegate: ActionCallbackDelegate<PortMapping>): ActionCallback {
        return object : PortMappingEntryGet(service, index), ActionCallbackDelegate<PortMapping> by delegate {}
    }
}

class PortMappingAddFactory(private val service: RemoteService, private val portMapping: PortMapping) : ActionCallbackFactory<ActionInvocation<*>>() {
    override fun create(delegate: ActionCallbackDelegate<ActionInvocation<*>>): ActionCallback {
        return object : PortMappingAdd(service, portMapping), ActionCallbackDelegate<ActionInvocation<*>> by delegate {}
    }
}

class PortMappingDeleteFactory(private val service: RemoteService, private val portMapping: PortMapping) : ActionCallbackFactory<ActionInvocation<*>>() {
    override fun create(delegate: ActionCallbackDelegate<ActionInvocation<*>>): ActionCallback {
        return object : PortMappingDelete(service, portMapping), ActionCallbackDelegate<ActionInvocation<*>> by delegate {}
    }
}
