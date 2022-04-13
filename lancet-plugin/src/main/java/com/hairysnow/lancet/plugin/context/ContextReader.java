package com.hairysnow.lancet.plugin.context;

import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.google.common.collect.ImmutableList;
import com.hairysnow.lancet.plugin.TransformContext;
import com.hairysnow.lancet.plugin.preprocess.ParseFailureException;
import com.hairysnow.lancet.weaver.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created by gengwanpeng on 17/5/2.
 * <p>
 * This class will unzip all jars,and accept all class with input ClassFetcher in thread pool.
 * Used in pre-analysis and formal analysis.
 */
public class ContextReader {

    private AtomicBoolean lock = new AtomicBoolean(false);
    private TransformContext context;
    private ClassifiedContentProvider provider;

    /**
     * android.build.tools: 4.0.0+使用{@link ForkJoinPool#commonPool()()}避免task编译时的内存溢出
     */
//    private ExecutorService service = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
//            0L, TimeUnit.MILLISECONDS,
//            new LinkedBlockingQueue<>(), (r, executor) -> {
//        Log.i("partial parse failed, executor has been shutdown");
//    });
    public ContextReader(TransformContext context) {
        this.context = context;
    }

    /**
     * read the classes in thread pool and send class to fetcher.
     * param incremental is incremental compile
     * param fetcher the fetcher to visit classes
     * throws IOException
     * throws InterruptedException
     */
    public void accept(boolean incremental, ClassFetcher fetcher) throws IOException, InterruptedException {

        Log.w("start accept");
        provider = ClassifiedContentProvider.newInstance(new JarContentProvider(), new DirectoryContentProvider(incremental));
        Log.w("generate provider success");
        // get all jars
        Collection<JarInput> jars = !incremental ? context.getAllJars() :
                ImmutableList.<JarInput>builder()
                        .addAll(context.getAddedJars())
                        .addAll(context.getRemovedJars())
                        .addAll(changedToDeleteAndAdd())
                        .build();
        Log.w("filter jars success");
        // accept the jar in thread pool
        List<Future<Void>> tasks = Stream.concat(jars.stream(), context.getAllDirs().stream())
                .distinct()
                .map(q -> new QualifiedContentTask(q, fetcher))
//                .map(t -> service.submit(t))
                .map(t -> ForkJoinPool.commonPool().submit(t))
                .collect(Collectors.toList());
        // block until all task has finish.
        for (Future<Void> future : tasks) {
            try {
                future.get();
            } catch (ExecutionException e) {
                if (incremental && e.getCause() instanceof ParseFailureException) {
                    shutDownAndRestart();
                    continue;
                }
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof InterruptedException) {
                    throw (InterruptedException) cause;
                } else {
                    Log.e("tasks execute error, message: " + e.getMessage());
                    throw new RuntimeException(e.getCause());
                }
            }
        }

    }

    /**
     * Transform the change operation to delete & add.
     *
     * @return
     */
    private Collection<? extends JarInput> changedToDeleteAndAdd() {
        List<JarInput> jarInputs = new ArrayList<>();
        context.getChangedJars().forEach(jarInput -> {
            if (!Status.NOTCHANGED.equals(jarInput.getStatus())) {
                Log.w("changedToDeleteAndAdd,jarInput:" + jarInput);
                jarInputs.add(new StatusOverrideJarInput(context, jarInput, jarInput.getStatus()));
            }
        });
        return jarInputs;
    }


    private void shutDownAndRestart() {
//        if (lock.compareAndSet(false, true)) {
//            service.shutdown();
//            service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        }
    }

    /**
     * Task to accept target QualifiedContent
     */
    private class QualifiedContentTask implements Callable<Void> {

        private QualifiedContent content;
        private ClassFetcher fetcher;

        QualifiedContentTask(QualifiedContent content, ClassFetcher fetcher) {
            this.content = content;
            this.fetcher = fetcher;
        }

        @Override
        public Void call() throws Exception {
            provider.forEach(content, fetcher);
            return null;
        }
    }
}
