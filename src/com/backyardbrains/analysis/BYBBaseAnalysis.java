package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.lang.ref.WeakReference;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

abstract class BYBBaseAnalysis<T> {

    private static final String TAG = makeLogTag(BYBBaseAnalysis.class);

    private final String filePath;
    private final AnalysisListener<T> listener;
    private final AnalysisThread<T> analysisThread;

    /**
     *
     */
    interface AnalysisListener<T> {

        /**
         *  @param filePath
         * @param results
         */
        void onAnalysisDone(@NonNull String filePath, @Nullable T[] results);

        /**
         *
         * @param filePath
         */
        void onAnalysisFailed(@NonNull String filePath);
    }

    BYBBaseAnalysis(@NonNull String filePath, @NonNull AnalysisListener<T> listener) {
        this.filePath = filePath;
        this.listener = listener;
        this.analysisThread = new AnalysisThread<>(this);
    }

    /**
     *
     */
    @Nullable abstract T[] process() throws Exception;

    /**
     * Triggers the analysis process.
     */
    final void startAnalysis() {
        analysisThread.start();
    }

    /**
     *
     * @param result
     */
    @SuppressWarnings("WeakerAccess") void onResult(@Nullable T[] result) {
    }

    /**
     *
     */
    @SuppressWarnings("WeakerAccess") void onFailed() {
    }

    /**
     *
     */
    void asyncOnResult(@Nullable T[] result) {
        LOGD(TAG, "asyncOnResult");
        listener.onAnalysisDone(filePath, result);
        onResult(result);
    }

    /**
     *
     */
    void asyncOnFailed() {
        LOGD(TAG, "asyncOnFailed");
        listener.onAnalysisFailed(filePath);

        onFailed();
    }

    /**
     * Background thread that initializes the analysis process.
     */
    private static class AnalysisThread<T> extends Thread {

        private WeakReference<BYBBaseAnalysis<T>> analysisRef;

        AnalysisThread(BYBBaseAnalysis<T> analysis) {
            analysisRef = new WeakReference<>(analysis);
        }

        @Override public void run() {
            final BYBBaseAnalysis<T> analysis;
            if ((analysis = analysisRef.get()) != null) {
                try {
                    analysis.asyncOnResult(analysis.process());
                } catch (Exception e) {
                    analysis.asyncOnFailed();
                }
            }
        }
    }
}
