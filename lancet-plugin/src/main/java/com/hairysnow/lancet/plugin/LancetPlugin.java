package com.hairysnow.lancet.plugin;

import com.android.build.gradle.BaseExtension;
import com.hairysnow.lancet.weaver.log.Log;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.Properties;

public class LancetPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        if (project.getPlugins().findPlugin("com.android.application") == null
                && project.getPlugins().findPlugin("com.android.library") == null) {
            throw new ProjectConfigurationException("Need android application/library plugin to be applied first", (Throwable) null);
        }


        boolean disableSensorsAnalyticsPlugin = false;
        Properties properties = new Properties();
        if (project.getRootProject().file("gradle.properties").exists()) {
           try {
               properties.load(new DataInputStream(new FileInputStream(project.getRootProject().file("gradle.properties"))));
               disableSensorsAnalyticsPlugin = Boolean.parseBoolean(properties.getProperty("lancet.disablePlugin", "false")) ||
                       Boolean.parseBoolean(properties.getProperty("lancet.disablePlugin", "false"));
           }catch (Exception e){
               Log.e("read gradle.properties error == "+e.getMessage());
           }
        }
        if (!disableSensorsAnalyticsPlugin) {
            BaseExtension baseExtension = (BaseExtension) project.getExtensions().getByName("android");
            LancetExtension lancetExtension = project.getExtensions().create("lancet", LancetExtension.class);
            baseExtension.registerTransform(new LancetTransform(project, lancetExtension));
        } else {
            Log.e("------------您已关闭了Lancet AOP插件--------------");
        }



       
    }
}
