package io.github.iamyours.fastclick

import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class BaseTransformPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "=========自定义路由插件111========="
        AppExtension appExtension = project.extensions.getByType(AppExtension)
        appExtension.registerTransform(getCustomTransform(project))
        //注册之后会在TransformManager#addTransform中生成一个task.
       // project.android.registerTransform(getCustomTransform())
    }

    abstract Transform getCustomTransform(Project project);
}