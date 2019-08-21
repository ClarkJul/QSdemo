package com.nbc.quickstart.core;

import com.nbc.quickstart.utils.Factory;
import com.nbc.quickstart.utils.NamedTask;
import com.nbc.quickstart.utils.NamedTaskExecutor;
import com.nbc.quickstart.utils.SingleThreadNamedTaskExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractExecutor {

    private final Factory<NamedTaskExecutor> mExecutorFactory;
    private android.util.ArrayMap<String, NamedTaskExecutor> mExecutors;
    protected volatile boolean mClosed = false;

    protected AbstractExecutor(String tag) {
        mExecutorFactory = SingleThreadNamedTaskExecutor.factory(new BackgroundLoaderThreadFactory(tag));
    }

    protected synchronized void execute(NamedTask task) {
        if (mExecutors == null) {
            mExecutors = new android.util.ArrayMap<>();
        }
        mClosed = false;
        String name = task.getName();
        NamedTaskExecutor executor = mExecutors.get(name);
        if (executor == null) {
            executor = mExecutorFactory.create();
            mExecutors.put(name, executor);
        }
        executor.execute(task);
    }

    protected synchronized void close() {
        if (mExecutors == null) return;
        mClosed = true;
        for (NamedTaskExecutor executor : mExecutors.values()) {
            executor.close();
        }
        mExecutors.clear();
    }

    /**
     * {@link ThreadFactory} which sets a meaningful name for the thread.
     */
    public class BackgroundLoaderThreadFactory implements ThreadFactory {
        private final AtomicInteger mCount = new AtomicInteger(1);
        private final String mTag;

        public BackgroundLoaderThreadFactory(String tag) {
            mTag = tag;
        }

        public Thread newThread(final Runnable r) {
            Thread t =  new Thread(r, mTag + "-" + mCount.getAndIncrement());

            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }
}
