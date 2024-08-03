package com.github.applejuiceyy.figuraextras.views.avatar.http;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;

public class SplittingSubscription<T> implements Flow.Subscriber<T> {

    private final UnaryOperator<T> copier;
    private final List<BackloggedSubscriber<T>> list;
    private Flow.Subscription subscription;
    private long requestedItems = 0;
    private long awaitingItems = 0;

    private boolean finished = false;
    private boolean errored = false;
    private Throwable error = null;


    private boolean awaitingSubscription = true;


    public SplittingSubscription(UnaryOperator<T> copier, Flow.Subscriber<T>[] subscribers) {
        this.copier = copier;
        this.list = Arrays.stream(subscribers).map((o) -> new BackloggedSubscriber<>(this, o)).toList();
    }

    @SafeVarargs
    public static <T> SplittingSubscription<T> bind(UnaryOperator<T> copier, Flow.Subscriber<T>... subscribers) {
        return new SplittingSubscription<>(copier, subscribers);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            this.subscription.cancel();
        }
        this.subscription = subscription;
        if (awaitingSubscription) {
            awaitingSubscription = false;
            for (BackloggedSubscriber<T> tBackloggedSubscriber : list) {
                tBackloggedSubscriber.startSubscriptions();
            }
        }
        if (awaitingItems > 0) {
            subscription.request(awaitingItems);
        }
    }

    @Override
    public void onNext(T item) {
        awaitingItems -= 1;
        for (int i = 0; i < list.size(); i++) {
            BackloggedSubscriber<T> tBackloggedSubscriber = list.get(i);
            tBackloggedSubscriber.onNext(i == list.size() - 1 ? item : copier.apply(item));
        }
    }

    @Override
    public void onError(Throwable throwable) {
        finished = true;
        errored = true;
        error = throwable;
        for (BackloggedSubscriber<T> tBackloggedSubscriber : list) {
            if (tBackloggedSubscriber.isTop()) {
                tBackloggedSubscriber.sendFinish();
            }
        }
    }

    @Override
    public void onComplete() {
        finished = true;
        for (BackloggedSubscriber<T> tBackloggedSubscriber : list) {
            if (tBackloggedSubscriber.isTop()) {
                tBackloggedSubscriber.sendFinish();
            }
        }
    }

    static class BackloggedSubscriber<T> {
        private final Flow.Subscriber<T> delegate;
        private final SplittingSubscription<T> owner;
        Queue<T> backlog;
        long remainingItems;
        long requestedItems;

        public BackloggedSubscriber(SplittingSubscription<T> owner, Flow.Subscriber<T> delegate) {
            backlog = new ArrayDeque<>();
            this.owner = owner;
            this.delegate = delegate;
        }

        public void startSubscriptions() {
            BackloggedSubscriber<T> self = this;
            delegate.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    remainingItems += n;
                    requestedItems += n;

                    while (!backlog.isEmpty() && remainingItems > 0) {
                        delegate.onNext(backlog.poll());
                        remainingItems -= 1;
                    }

                    if (requestedItems > owner.requestedItems) {
                        long amount = requestedItems - owner.requestedItems;
                        owner.subscription.request(amount);
                        owner.awaitingItems += amount;
                        owner.requestedItems = requestedItems;
                    }

                    if (isTop()) {
                        sendFinish();
                    }
                }

                @Override
                public void cancel() {
                    //noinspection DataFlowIssue intellij also doesn't like when I remove the final
                    owner.list.remove(self);
                    if (owner.list.size() == 0) {
                        owner.subscription.cancel();
                    }
                }
            });
        }

        public void onNext(T item) {
            if (remainingItems > 0) {
                remainingItems -= 1;
                delegate.onNext(item);
            } else {
                backlog.add(item);
            }
            if (isTop()) {
                sendFinish();
            }
        }

        public boolean isTop() {
            return (requestedItems - remainingItems) == (owner.requestedItems - owner.awaitingItems) && backlog.isEmpty();
        }

        public void sendFinish() {
            if (owner.finished) {
                if (owner.errored) {
                    delegate.onError(owner.error);
                } else {
                    delegate.onComplete();
                }
            }
        }
    }
}
