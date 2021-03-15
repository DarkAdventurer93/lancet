package com.hairysnow.lancet.weaver;

import com.hairysnow.lancet.weaver.asm.ClassTransform;
import com.hairysnow.lancet.weaver.entity.TransformInfo;
import com.hairysnow.lancet.weaver.graph.Graph;
import com.hairysnow.lancet.weaver.log.Log;


/**
 * Created by gengwanpeng on 17/3/21.
 */
public class AsmWeaver implements Weaver {

    /**
     * Create a AsmWeaver instance. In a compilation process, the AsmWeaver instance will only be created once.
     *
     * param transformInfo the transformInfo for this compilation process.
     * param graph
     * @return
     */
    public static Weaver newInstance(TransformInfo transformInfo, Graph graph) {
        return new AsmWeaver(transformInfo, graph);
    }

    private final TransformInfo transformInfo;
    private final Graph graph;

    private AsmWeaver(TransformInfo transformInfo, Graph graph) {
        Log.d(transformInfo.toString());
        this.graph = graph;
        this.transformInfo = transformInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassData[] weave(byte[] input, String relativePath) {
        if(!relativePath.endsWith(".class")){
            throw new IllegalArgumentException("relativePath is not a class: " + relativePath);
        }
        String internalName = relativePath.substring(0, relativePath.lastIndexOf('.'));
        try {
            return ClassTransform.weave(transformInfo, graph, input, internalName);
        }catch (RuntimeException e){
            Log.w("failed to transform: " + relativePath, e);
            return new ClassData[]{new ClassData(input, internalName)};
        }
    }

}
