package com.hairysnow.lancet.weaver.meta;

import com.hairysnow.lancet.weaver.graph.Graph;
import com.hairysnow.lancet.weaver.parser.AnnotationMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by gengwanpeng on 17/5/3.
 */
public class ClassMetaInfo {

    public String className;
    public List<AnnotationMeta> annotationMetas = new ArrayList<>();

    public List<MethodMetaInfo> methods = new ArrayList<>(4);

    public ClassMetaInfo(String className) {
        this.className = className;
    }

    public List<HookInfoLocator> toLocators(Graph nodeMap) {
        return methods.stream()
                .map(m -> {

                    HookInfoLocator locator = new HookInfoLocator(nodeMap);
                    locator.setSourceNode(className, m.sourceNode);

                    annotationMetas.forEach(i -> {
                        i.accept(locator);
                    });

                    locator.goMethod();

                    m.metaList.forEach(annotationMeta -> annotationMeta.accept(locator));

                    if (locator.satisfied()) {
                        locator.transformNode();
                    }
                    return locator;
                }).collect(Collectors.toList());
    }
}
