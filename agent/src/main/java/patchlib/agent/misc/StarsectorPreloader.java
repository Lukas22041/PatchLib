package patchlib.agent.misc;

import patchlib.agent.data.ClassDataImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.scan.DiscoveryData;
import patchlib.api.data.ClassData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StarsectorPreloader {

    private DiscoveryData data;
    private ClassLoader classLoader;

    public StarsectorPreloader(DiscoveryData data, ClassLoader classLoader) {
        this.data = data;
        this.classLoader = classLoader;
    }

    public void preload() {
        PatchLibLogger.info("Starting starsector class preload");
        long start = System.currentTimeMillis();

        //Skip modded classes for now, only preload starsectors own classes
        //If ever changed, the passed in class loader also needs to be changed to the mod class loader.
        List<ClassData> starsectorClasses = data.classes().stream().filter(ClassData::isFromStarsector).toList();

        int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()-1));
        ExecutorService executorService = createExecutor(threads);
        List<StarsectorPreloadTask> tasks = new ArrayList<>();

        int chunkCount = threads * 8;
        int chunkSize = (starsectorClasses.size() + chunkCount - 1) / chunkCount;

        for (int from = 0; from < starsectorClasses.size(); from += chunkSize) {
            List<ClassData> chunk = starsectorClasses.subList(from, Math.min(from + chunkSize, starsectorClasses.size()));
            tasks.add(new StarsectorPreloadTask(chunk, classLoader));
        }

        int loaded = 0;
        int skipped = 0;
        try {
            List<Future<StarsectorPreloadTask.StarsectorPreloadTaskResult>> futures = executorService.invokeAll(tasks);
            for (Future<StarsectorPreloadTask.StarsectorPreloadTaskResult> future : futures) {
                try {
                    StarsectorPreloadTask.StarsectorPreloadTaskResult result = future.get();
                    loaded += result.loaded;
                    skipped += result.skipped;
                } catch (Throwable ex) {
                    PatchLibLogger.error("Failed to preload a chunk of starsector classes", ex);
                }
            }
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to preload starsector classes", ex);
        } finally {
            executorService.shutdown();
        }

        float diff = System.currentTimeMillis() - start ;
        PatchLibLogger.info("Loaded " + loaded + " starsector classes during preload");
        PatchLibLogger.info("Skipped " + skipped + " starsector classes during preload");
        PatchLibLogger.info("Finished starsector class preload in " + diff + " ms");
        PatchLibLogger.blank();
    }

    private ExecutorService createExecutor(int threads) {
        AtomicInteger count = new AtomicInteger();

        return Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "PatchLib-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    private static class StarsectorPreloadTask implements Callable<StarsectorPreloadTask.StarsectorPreloadTaskResult> {

        public record StarsectorPreloadTaskResult(int loaded, int skipped) { }

        private List<ClassData> classDataList;
        private ClassLoader classLoader;

        public StarsectorPreloadTask(List<ClassData> classDataList, ClassLoader classLoader) {
            this.classDataList = classDataList;
            this.classLoader = classLoader;
        }

        @Override
        public StarsectorPreloadTaskResult call() throws Exception {
            int loaded = 0;
            int skipped = 0;
            for (ClassData classData : classDataList) {
                try {
                    //Load with "initialize" set to false prevents static blocks from being called early.
                    Class.forName(classData.getName(), false, classLoader);
                    loaded++;
                } catch (Throwable ex) {
                    skipped++;
                }

            }

            return new StarsectorPreloadTaskResult(loaded, skipped);
        }
    }
}
