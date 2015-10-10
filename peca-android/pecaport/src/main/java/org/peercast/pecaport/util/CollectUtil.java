package org.peercast.pecaport.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.functors.InstanceofPredicate;
import org.apache.commons.collections4.iterators.IteratorIterable;

import java.util.Iterator;

/**
 *
 */
public class CollectUtil {
    private CollectUtil() {
    }

    public static <T extends View> Iterable<T> childViews(final ViewGroup viewGroup, final Class<T> cls) {
        return new IteratorIterable<>(new Iterator<T>() {
            int index;
            final int count = viewGroup.getChildCount();

            @Override
            public boolean hasNext() {
                return index < count;
            }

            @Override
            public T next() {
                return cls.cast(viewGroup.getChildAt(index++));
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }

    @NonNull
    public static <I, O> Iterable<O> filterInstanceOf(@Nullable Iterable<I> iterable, final Class<O> cls) {
        return IteratorUtils.asIterable(IteratorUtils.transformedIterator(
                iterable == null ? IteratorUtils.<I>emptyIterator() :
                        IteratorUtils.filteredIterator(iterable.iterator(),
                                InstanceofPredicate.instanceOfPredicate(cls)),
                new Transformer<I, O>() {
                    @Override
                    public O transform(I input) {
                        return cls.cast(input);
                    }
                }
        ));
    }


    public static Iterable<Integer> intRange(final int start, final int stop, final int step) {
        if (step == 0)
            throw new IllegalArgumentException("step == 0");

        return new IteratorIterable<>(new Iterator<Integer>() {
            int value = start;

            @Override
            public boolean hasNext() {
                if (step > 0)
                    return value < stop;
                else
                    return value > stop;
            }

            @Override
            public Integer next() {
                int v = value;
                value += step;
                return v;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }

}
