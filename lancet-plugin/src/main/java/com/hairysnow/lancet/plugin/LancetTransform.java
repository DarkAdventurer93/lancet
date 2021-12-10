package com.hairysnow.lancet.plugin;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.SecondaryFile;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.hairysnow.lancet.plugin.context.ContextReader;
import com.hairysnow.lancet.plugin.preprocess.PreClassAnalysis;
import com.hairysnow.lancet.weaver.AsmWeaver;
import com.hairysnow.lancet.weaver.MetaParser;
import com.hairysnow.lancet.weaver.Weaver;
import com.hairysnow.lancet.weaver.entity.TransformInfo;
import com.hairysnow.lancet.weaver.log.Impl.FileLoggerImpl;
import com.hairysnow.lancet.weaver.log.Log;
import com.hairysnow.lancet.weaver.parser.AsmMetaParser;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class LancetTransform extends Transform {

    private final LancetExtension lancetExtension;
    private final GlobalContext global;
    private LocalCache cache;


    public LancetTransform(Project project, LancetExtension lancetExtension) {
        this.lancetExtension = lancetExtension;
        this.global = new GlobalContext(project);
        // load the LocalCache from disk
        this.cache = new LocalCache(global.getLancetDir());
    }

    @Override
    public String getName() {
        return "lancet";
    }


    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }


    @Override
    public Set<QualifiedContent.ScopeType> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }


    /**
     * @return Hook classes we found in last compilation. If they has been changed,gradle will auto go full compile.
     */
    @Override
    public Collection<SecondaryFile> getSecondaryFiles() {
        return cache.hookClassesInDir()
                .stream()
                .map(File::new)
                .map(SecondaryFile::nonIncremental)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<File> getSecondaryDirectoryOutputs() {
        return Collections.singletonList(global.getLancetDir());
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        initLog();
        Log.printCopyright();
        Log.i("start time: " + System.currentTimeMillis());

        // collect the information this compile need
        TransformContext context = new TransformContext(transformInvocation, global);

        Log.w("after android plugin, incremental: " + context.isIncremental());
        Log.i("now: " + System.currentTimeMillis());

        boolean incremental = lancetExtension.getIncremental();

        PreClassAnalysis preClassAnalysis = new PreClassAnalysis(cache);

        incremental = preClassAnalysis.execute(incremental, context);

        Log.w("after pre analysis, incremental: " + incremental);
        Log.i("now: " + System.currentTimeMillis());

        MetaParser parser = createParser(context);
        if (incremental && !context.getGraph().checkFlow()) {
            incremental = false;
            context.clear();
        }
        Log.w("after check flow, incremental: " + incremental);
        Log.i("now: " + System.currentTimeMillis());

        context.getGraph().flow().clear();
        TransformInfo transformInfo = parser.parse(context.getHookClasses(), context.getGraph());

        Weaver weaver = AsmWeaver.newInstance(transformInfo, context.getGraph());
        new ContextReader(context).accept(incremental, new TransformProcessor(context, weaver));
        Log.w("build successfully done");
        Log.i("now: " + System.currentTimeMillis());

        cache.saveToLocal();
        Log.i("cache saved");
        Log.i("now: " + System.currentTimeMillis());
    }

    private AsmMetaParser createParser(TransformContext context) {
        URL[] urls = Stream.concat(context.getAllJars().stream(), context.getAllDirs().stream()).map(QualifiedContent::getFile)
                .map(File::toURI)
                .map(u -> {
                    try {
                        return u.toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                })
                .toArray(URL[]::new);
        Log.d("urls:\n" + Joiner.on("\n ").join(urls));
        ClassLoader cl = URLClassLoader.newInstance(urls, null);
        return new AsmMetaParser(cl);
    }

    private void initLog() throws IOException {
        Log.setLevel(lancetExtension.getLogLevel());
        if (!Strings.isNullOrEmpty(lancetExtension.getFileName())) {
            String name = lancetExtension.getFileName();
            if (name.contains(File.separator)) {
                throw new IllegalArgumentException("Log file name can't contains file separator");
            }
            File logFile = new File(global.getLancetDir(), "log_" + lancetExtension.getFileName());
            Files.createParentDirs(logFile);
            Log.setImpl(FileLoggerImpl.of(logFile.getAbsolutePath()));
        }
    }
}

