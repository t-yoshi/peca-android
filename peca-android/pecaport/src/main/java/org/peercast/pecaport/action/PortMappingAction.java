package org.peercast.pecaport.action;

import android.support.annotation.NonNull;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.model.PortMapping;

/**
 * (c) 2015- T Yoshizawa
 * (c) 2015, 4th Line GmbH, Switzerland, http://4thline.com/
 *
 * GNU Lesser General Public License Version 2.1
 */
public abstract class PortMappingAction extends ActionCallback {
    protected final Method mMethod;
    protected final PortMapping mPortMapping;

    protected PortMappingAction(@NonNull Method method, Service service, PortMapping portMapping) {
        super(new ActionInvocation(service.getAction(method.name() + "PortMapping")), null);
        mMethod = method;
        mPortMapping = portMapping;


        if (method == Method.Add) {
            getActionInvocation().setInput("NewInternalClient", portMapping.getInternalClient());
            getActionInvocation().setInput("NewInternalPort", portMapping.getInternalPort());
            getActionInvocation().setInput("NewLeaseDuration", portMapping.getLeaseDurationSeconds());
            getActionInvocation().setInput("NewEnabled", portMapping.isEnabled());
            if (portMapping.hasRemoteHost())
                getActionInvocation().setInput("NewRemoteHost", portMapping.getRemoteHost());
            if (portMapping.hasDescription())
                getActionInvocation().setInput("NewPortMappingDescription", portMapping.getDescription());
        }

        getActionInvocation().setInput("NewExternalPort", portMapping.getExternalPort());
        getActionInvocation().setInput("NewProtocol", portMapping.getProtocol());
        if (portMapping.hasRemoteHost())
            getActionInvocation().setInput("NewRemoteHost", portMapping.getRemoteHost());

    }

    public enum Method {
        /**AddPortMappingを実行する*/
        Add,
        /**DeletePortMappingを実行する*/
        Delete
    }

}
