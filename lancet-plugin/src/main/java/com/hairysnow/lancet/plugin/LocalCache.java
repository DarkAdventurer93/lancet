package com.hairysnow.lancet.plugin;

import com.android.build.api.transform.Status;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.hairysnow.lancet.plugin.preprocess.MetaGraphGeneratorImpl;
import com.hairysnow.lancet.weaver.graph.CheckFlow;
import com.hairysnow.lancet.weaver.graph.ClassEntity;
import com.hairysnow.lancet.weaver.log.Log;

import org.apache.commons.io.Charsets;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by gengwanpeng on 17/4/26.
 */
public class LocalCache {

    // Persistent storage for metas
    private File localCache;
    private final Metas metas;
    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public LocalCache(File dir) {
        localCache = new File(dir, "buildCache.json");
        metas = loadCache();
    }

    private Metas loadCache() {
        if (localCache.exists() && localCache.isFile()) {
            try {
                Reader reader = Files.newReader(localCache, Charsets.UTF_8);
                return gson.fromJson(reader, Metas.class).withoutNull();
            } catch (IOException e) {
                Log.e("load cache error: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (JsonParseException e) {
                if (!localCache.delete()) {
                    Log.e("load cache error: cache file has been modified, but can't delete." + e.getMessage());
                    throw new RuntimeException("cache file has been modified, but can't delete.", e);
                }
            }
        }
        return new Metas();
    }


    public List<String> hookClasses() {
        return metas.hookClasses;
    }

    public List<String> hookClassesInDir() {
        return metas.hookClassesInDir;
    }

    public CheckFlow hookFlow() {
        return metas.flow;
    }


    /**
     * if hook class has modified.
     * param context TransformContext for this compile
     *
     * @return true if hook class hasn't modified.
     */
    public boolean isHookClassModified(TransformContext context) {
        List<String> hookClasses = metas.jarsWithHookClasses;
        return Stream.concat(context.getRemovedJars().stream(), context.getChangedJars().stream())
                .anyMatch(jarInput -> hookClasses.contains(jarInput.getFile().getAbsolutePath()));
    }

    public void accept(MetaGraphGeneratorImpl graph) {
        metas.classMetas.forEach(m -> graph.add(m, Status.NOTCHANGED));
    }

    public void saveToLocal() {
        try {
            Files.createParentDirs(localCache);
            Writer writer = Files.newWriter(localCache, Charsets.UTF_8);
            gson.toJson(metas.withoutNull(), Metas.class, writer);
            writer.close();
        } catch (IOException e) {
            Log.e("saveToLocal error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void clear() throws IOException {
        if (localCache.exists() && localCache.isFile()) {
            try {
                localCache.delete();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("can't delete cache file: " + e);
                throw new IOException("can't delete cache file");
            }
        }
    }

    public void savePartially(List<ClassEntity> classMetas) {
        metas.classMetas = classMetas;
        saveToLocal();
    }

    public void saveFully(List<ClassEntity> classMetas, List<String> hookClasses, List<String> hookClassesInDir, List<String> jarWithHookClasses) {
        metas.classMetas = classMetas;
        metas.hookClasses = hookClasses;
        metas.hookClassesInDir = hookClassesInDir;
        metas.jarsWithHookClasses = jarWithHookClasses;
        saveToLocal();
    }
}
