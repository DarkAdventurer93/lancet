package com.hairysnow.lancet.weaver.parser;

/**
 * Created by gengwanpeng on 17/5/3.
 */
public interface AcceptableAnnoParser extends AnnoParser{

    boolean accept(String desc);
}
