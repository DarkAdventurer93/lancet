package com.hairysnow.lancet.weaver.asm;

import com.hairysnow.lancet.weaver.ClassData;
import com.hairysnow.lancet.weaver.asm.classvisitor.HookClassVisitor;
import com.hairysnow.lancet.weaver.asm.classvisitor.InsertClassVisitor;
import com.hairysnow.lancet.weaver.asm.classvisitor.ProxyClassVisitor;
import com.hairysnow.lancet.weaver.asm.classvisitor.TryCatchInfoClassVisitor;
import com.hairysnow.lancet.weaver.entity.TransformInfo;
import com.hairysnow.lancet.weaver.graph.Graph;

import org.objectweb.asm.ClassReader;


/**
 * Created by Jude on 2017/4/25.
 */

public class ClassTransform {

    public static final String AID_INNER_CLASS_NAME = "_lancet";

    public static ClassData[] weave(TransformInfo transformInfo, Graph graph, byte[] classByte, String internalName) {
        ClassCollector classCollector = new ClassCollector(new ClassReader(classByte), graph);

        classCollector.setOriginClassName(internalName);

        MethodChain chain = new MethodChain(internalName, classCollector.getOriginClassVisitor(), graph);
        ClassContext context = new ClassContext(graph, chain, classCollector.getOriginClassVisitor());

        ClassTransform transform = new ClassTransform(classCollector, context);
        transform.connect(new HookClassVisitor(transformInfo.hookClasses));
        transform.connect(new ProxyClassVisitor(transformInfo.proxyInfo));
        transform.connect(new InsertClassVisitor(transformInfo.executeInfo));
        transform.connect(new TryCatchInfoClassVisitor(transformInfo.tryCatchInfo));
        transform.startTransform();
        return classCollector.generateClassBytes();
    }

    private LinkedClassVisitor mHeadVisitor;
    private LinkedClassVisitor mTailVisitor;
    private ClassCollector mClassCollector;
    private final ClassContext context;

    public ClassTransform(ClassCollector mClassCollector, ClassContext context) {
        this.mClassCollector = mClassCollector;
        this.context = context;
    }

    void connect(LinkedClassVisitor visitor) {
        if (mHeadVisitor == null) {
            mHeadVisitor = visitor;
        } else {
            mTailVisitor.setNextClassVisitor(visitor);
        }
        mTailVisitor = visitor;
        visitor.setClassCollector(mClassCollector);
        visitor.setContext(context);
    }

    void startTransform() {
        mTailVisitor.setNextClassVisitor(mClassCollector.getOriginClassVisitor());
        mClassCollector.mClassReader.accept(mHeadVisitor, 0);
    }
}
