package io.github.iamyours.fastclick

import com.android.build.api.transform.Transform
import org.gradle.api.Project


class FastClickTransformPlugin extends BaseTransformPlugin {

    @Override
    Transform getCustomTransform(Project project) {
        println "=========自定义路由插件22========="
        return new FastClickTransform()
    }
}