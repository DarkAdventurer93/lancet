package com.hairysnow.lancet.weaver.parser.anno;

import com.hairysnow.lancet.weaver.log.Log;
import com.hairysnow.lancet.weaver.meta.HookInfoLocator;
import com.hairysnow.lancet.weaver.parser.AcceptableAnnoParser;
import com.hairysnow.lancet.weaver.parser.AnnotationMeta;

import org.objectweb.asm.tree.AnnotationNode;


/**
 * Created by gengwanpeng on 17/5/4.
 */
public class AcceptAny implements AcceptableAnnoParser {

    public static AcceptAny INSTANCE = new AcceptAny();

    private AcceptAny(){
    }

    @Override
    public boolean accept(String desc) {
        return true;
    }

    @Override
    public AnnotationMeta parseAnno(AnnotationNode annotationNode) {
        Log.w("Annotation " + annotationNode.desc + " is lost.");
        return new AnnotationMeta(annotationNode.desc) {

            @Override
            public void accept(HookInfoLocator locator) {
            }
        };
    }
}
