package com.hairysnow.lancet.weaver;


import com.hairysnow.lancet.weaver.entity.TransformInfo;
import com.hairysnow.lancet.weaver.graph.Graph;

import java.util.List;



/**
 *
 * Created by gengwanpeng on 17/3/21.
 */
public interface MetaParser {

    TransformInfo parse(List<String> classes, Graph graph);
}
