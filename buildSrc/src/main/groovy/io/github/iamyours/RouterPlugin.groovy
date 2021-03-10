package io.github.iamyours

import io.github.iamyours.transform.RouterTransform
import org.gradle.api.Plugin;
import org.gradle.api.Project;


class RouterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

//        AppExtension appExtension = project.extensions.getByType(AppExtension);
//        appExtension.registerTransform(RouterTransform(project));
        println "=========自定义路由插件========="
        project.android.registerTransform(new RouterTransform(project));
    }
}
