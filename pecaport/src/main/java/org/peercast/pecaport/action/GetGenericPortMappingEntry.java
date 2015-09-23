package org.peercast.pecaport.action;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.PortMapping;

import java.util.Map;

 /**
 * (c) 2015- T Yoshizawa
 * (c) 2015, 4th Line GmbH, Switzerland, http://4thline.com/
 *
 * GNU Lesser General Public License Version 2.1
 */
abstract public class GetGenericPortMappingEntry extends ActionCallback {

    public GetGenericPortMappingEntry(Service service, int index){
        super(new ActionInvocation(service.getAction("GetGenericPortMappingEntry")));
        getActionInvocation().setInput("NewPortMappingIndex", new UnsignedIntegerTwoBytes(index));
    }

    @Override
    public void success(ActionInvocation invocation) {
        Map<String, ActionArgumentValue<Service>> outputMap = invocation.getOutputMap();
        success(new PortMapping(outputMap));
    }

    protected abstract void success(PortMapping portMapping);


}
