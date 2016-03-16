package org.peercast.pecaport.util;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Future;

/**
 * Executor#execute()を遅延実行する。
 *
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public abstract class LazyTask<T> {


    private LazyTask() {
    }

    /**
     * taskQueueの先頭からcount個取り出し、execute()を実行する。
     * @return 最大count個の要素を持つFutureのコレクション
     * */
    public static <T> Collection<Future<T>> lazyExecute(int count, Queue<LazyTask<T>> taskQueue) {
        Collection<Future<T>> results = new ArrayList<>(count);
        while (!taskQueue.isEmpty() && count-- > 0) {
            results.add(taskQueue.poll().execute());
        }
        return results;
    }

    abstract protected Future<T> execute();

    /**controlPoint#execute(callback)を遅延実行するラッパー。*/
    static public LazyTask<Void> wrapExecute(final ControlPoint controlPoint, final ActionCallback callback) {
        return new LazyTask<Void>() {
            @Override
            @SuppressWarnings("unchecked")
            public Future<Void> execute() {
                return controlPoint.execute(callback);
            }
        };
    }


}
