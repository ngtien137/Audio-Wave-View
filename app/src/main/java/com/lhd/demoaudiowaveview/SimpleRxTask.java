package com.lhd.demoaudiowaveview;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SimpleRxTask {
    private Listener listener;
    private CompletableEmitter emitter;
    private Disposable disposable;
    private Completable completable;
    private CompletableObserver completableObserver;
    // private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private boolean isRunning;

    public boolean isRunning() {
        return isRunning;
    }

    public SimpleRxTask(Listener listener) {
        this.listener = listener;
        completable = Completable.create(emitter -> {
            listener.onDoing();
            this.emitter = emitter;
            emitter.onComplete();
        });

        completableObserver = new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;
                isRunning = true;
                listener.onStart();
            }

            @Override
            public void onComplete() {
                listener.onComplete();
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

        };
    }

    public void cancel() {
        disposable.dispose();
        if (listener != null) {
            listener.onCancel();
        }
    }

    public void start() {
        completable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(completableObserver);
    }

    interface Listener {
        void onStart();

        void onComplete();

        void onDoing();

        void onCancel();
    }
}
