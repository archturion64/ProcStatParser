package com.github.archturion64.procstatparser;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CpuLoadSubscription implements Flow.Subscription {

    private final Flow.Subscriber<? super List<Short>> subscriber;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CpuLoadSubscription(Flow.Subscriber<? super List<Short>> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        executor.submit( () -> {
            for ( long i = 0L; i < n; i++)
            {
                try {
                    subscriber.onNext(ProcStatParser.readCpuLoad());
                } catch (Exception e)
                {
                    subscriber.onError(e);
                }
            }
        });
    }

    @Override
    public void cancel() {
        subscriber.onComplete();
    }
}
