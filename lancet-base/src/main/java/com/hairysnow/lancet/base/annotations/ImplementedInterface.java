package com.hairysnow.lancet.base.annotations;

import com.hairysnow.lancet.base.Scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Created by gengwanpeng on 17/3/20.
 */
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.TYPE, ElementType.METHOD})
public @interface ImplementedInterface {

    String[] value();

    Scope scope() default Scope.SELF;
}
