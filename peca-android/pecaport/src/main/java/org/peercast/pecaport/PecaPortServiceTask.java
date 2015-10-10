package org.peercast.pecaport;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.PortMapping;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
abstract class PecaPortServiceTask {
    private static final String TAG = "PecaPortServiceTask";

    final String method;
    final String clientIp;
    final UnsignedIntegerTwoBytes port;


    protected final Logger logger = Logger.getLogger(getClass().getName());


    protected PecaPortServiceTask(String method, String clientIp, int port) {
        this.method = method;
        this.clientIp = clientIp;
        this.port = new UnsignedIntegerTwoBytes(port);
    }

    /**
     * すでに開かれている
     */
    protected boolean existsMapping(final Collection<PortMapping> mappings) {
        return CollectionUtils.exists(mappings, new Predicate<PortMapping>() {
            @Override
            public boolean evaluate(PortMapping m) {
                return m.getProtocol() == PortMapping.Protocol.TCP &&
                        m.getExternalPort().equals(port)
                        ;
            }
        });
    }

    /**
     * executeする必要があるか
     */
    abstract public boolean needExecute(Collection<PortMapping> mappings);

    /**
     * ポート開閉を実行する
     */
    abstract public Future execute(PortManipulator manipulator, PortManipulator.OnResultListener listener);

    /**
     * ポートを開くタスク
     */
    public static class Add extends PecaPortServiceTask {
        public Add(String clientIp, int port) {
            super("Add", clientIp, port);
        }

        public Future execute(PortManipulator manipulator, PortManipulator.OnResultListener listener) {
            PortMapping mapping = new PortMapping();
            mapping.setExternalPort(port);
            mapping.setProtocol(PortMapping.Protocol.TCP);
            mapping.setInternalPort(port);
            mapping.setInternalClient(clientIp);
            mapping.setDescription(PecaPortService.DESCRIPTION);
            mapping.setEnabled(true);
            return manipulator.addPort(mapping, listener);
        }

        @Override
        public boolean needExecute(Collection<PortMapping> mappings) {
            boolean exists = existsMapping(mappings);
            logger.info("Already mapping: port=" + port);
            return !exists;
        }
    }

    /**
     * ポートを閉じるタスク
     */
    public static class Delete extends PecaPortServiceTask {
        public Delete(String clientIp, int port) {
            super("Delete", clientIp, port);
        }

        @Override
        public boolean needExecute(Collection<PortMapping> mappings) {
            boolean exists = existsMapping(mappings);
            logger.finest("No mapping: port=" + port);
            return exists;
        }

        public Future execute(PortManipulator manipulator, PortManipulator.OnResultListener listener) {
            PortMapping mapping = new PortMapping();
            mapping.setExternalPort(port);
            mapping.setProtocol(PortMapping.Protocol.TCP);
            return manipulator.deletePort(mapping, listener);
        }


    }

    /**
     * クライアントが違う場合にのみポートを閉じるタスク<br>
     * (すでに割り当てられているとConflictInMappingEntry が発生するので)
     */
    public static class DeleteOnDifferentClient extends Delete {
        public DeleteOnDifferentClient(String clientIp, int port) {
            super(clientIp, port);
        }

        @Override
        public boolean needExecute(Collection<PortMapping> mappings) {
            return CollectionUtils.exists(mappings, new Predicate<PortMapping>() {
                @Override
                public boolean evaluate(PortMapping m) {
                    boolean b = m.getProtocol() == PortMapping.Protocol.TCP &&
                            m.getExternalPort().equals(port) &&
                            !m.getInternalClient().equals(clientIp); //IPの違い
                    //Log.d(TAG, b+": "+m.getInternalClient() + "," + clientIp);
                    return b;
                }
            });
        }
    }
}
