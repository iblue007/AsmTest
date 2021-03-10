package io.github.iamyours.transform

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import jdk.internal.org.objectweb.asm.ClassReader
import jdk.internal.org.objectweb.asm.tree.AnnotationNode
import jdk.internal.org.objectweb.asm.tree.ClassNode
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry;

class RouterTransform extends Transform {
    private static final String DEFAULT_NAME = "RouterTransform";
    Project project;

    RouterTransform(Project project) {
        this.project = project;
    }

    @Override
    String getName() {
        return DEFAULT_NAME;
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        println("===开始transfrom===")
        def routeJarInput
        def sTime = System.currentTimeMillis();
        def inputs = transformInvocation.inputs;
        def outputProvider = transformInvocation.outputProvider;
        outputProvider.deleteAll()
        //println("===outputProvider:"+outputProvider.)
        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.directoryInputs) {
                //从目录中读取class,过滤系统等不需要的class,最后保存到map中
                readClassWithPath(directoryInput.file)
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
                System.err.println("======file:" + directoryInput.file.absolutePath + "--dest:" + dest.absolutePath)
                //======file:E:\work_pk22\AsmTest\app\build\intermediates\classes\debug--dest:E:\work_pk22\AsmTest\app\build\intermediates\transforms\RouterTransform\debug\36
            }
            for (JarInput jarInput : input.jarInputs) {
                if (jarInput.name.startsWith(ROUTE_NAME)) {
                    routeJarInput = jarInput
                }
                if (jarInput.scopes.contains(QualifiedContent.Scope.SUB_PROJECTS)) {//module library
                    //从module中获取注解信息
                    readClassWithJar(jarInput)
                }
                copyFile(jarInput, outputProvider)
            }
        }
        def endTime = System.currentTimeMillis()
        println("rout map:" + routeMap)
        //rout map:[/news/news_list:com.example.news.NewsListActivity, /app/main:com.example.asmtest.MainActivity]
        //所有的路由信息已经通过ASM去取保存到了map中，接下来只要操作routeMap的字节码，将这些信息保存到loadInto方法中就可以了
        insertCodeIntoJar(routeJarInput, transformInvocation.outputProvider)
        println("===========route transform finished:" + (endTime - sTime))
    }

    //从jar中读取class
    void readClassWithJar(JarInput jarInput) {
        JarFile jarFile = new JarFile(jarInput.file)
        Enumeration enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement()
            String entryName = jarEntry.name
            if (!entryName.endsWith(".class")) {
                continue
            }
            String className = entryName.substring(0, entryName.length() - 6).replaceAll("/", ".")
            InputStream is = jarFile.getInputStream(jarEntry)
            addRouteMap(is, className)
        }

    }

    void insertCodeIntoJar(JarInput jarInput, TransformOutputProvider outputProvider) {
        File jarFile = jarInput.file
        def tmp = new File(jarFile.getParent(), jarFile.name + ".tmp")
        if (tmp.exists()) tmp.delete()
        def file = new JarFile(jarFile)
        def dest = getDestFile(jarInput, outputProvider)
        Enumeration enumeration = file.entries()
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(tmp))
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement()
            String entryName = jarEntry.name
            ZipEntry zipEntry = new ZipEntry(entryName)
            InputStream is = file.getInputStream(jarEntry)
            jos.putNextEntry(zipEntry)
            if (isRouteMapClass(entryName)) {//如果class的名称和我们
                jos.write(hackRouteMap(jarFile))
            } else {
                jos.write(IOUtils.toByteArray(is))
            }
            is.close()
            jos.closeEntry()
        }
        jos.close()
        file.close()
        FileUtils.copyFile(tmp, dest)
        println("======tmp:" + tmp + "---dest:" + dest)
        //======tmp:C:\Users\dpzx -ptyy\.gradle\caches\modules-2\files-2.1\io.github.iamyours\router-api\1.0.0\f65d6ef397d72e173610e211d741d51802903ef6\router-api-1.0.0.jar.tmp
        // ---dest:E:\work_pk22\AsmTest\app\build\intermediates\transforms\RouterTransform\debug\34.jar
    }

    byte[] hackRouteMap(File jarFile) {
        ClassPool pool = ClassPool.getDefault()
        pool.insertClassPath(jarFile.absolutePath)
        CtClass ctClass = pool.get(ROUTE_MAP_CLASS_NAME)
        CtMethod method = ctClass.getDeclaredMethod("loadInto")
        StringBuffer code = new StringBuffer("{")
        for (String key : routeMap.keySet()) {
            String value = routeMap[key]
            code.append("\$1.put(\"" + key + "\",\"" + value + "\");")
        }
        code.append("}")
        method.setBody(code.toString())
        byte[] bytes = ctClass.toBytecode()
        ctClass.stopPruning(true)
        ctClass.defrost()
        return bytes
    }

    boolean isRouteMapClass(String entryName) {
        return ROUTE_MAP_CLASS_FILE_NAME == entryName;
    }

    void copyFile(JarInput jarInput, TransformOutputProvider outputProvider) {
        def dest = getDestFile(jarInput, outputProvider)
        FileUtils.copyFile(jarInput.file, dest)
    }

    //获取文件的输出地址
    static File getDestFile(JarInput jarInput, TransformOutputProvider outputProvider) {
        def destName = jarInput.name
        // 重名名输出文件,因为可能同名,会覆盖
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        //获得输出文件
        File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        println("======getDestFile:" + jarInput.file.absolutePath + "----hexName:" + hexName + "--destName:" + destName + "--dest:" + dest)
     //=====getDestFile:C:\Users\dpzx -ptyy\.gradle\caches\transforms-1\files-1.1\appcompat-v7-28.0.0.aar\cb6853ea6fe02d5cb7adacafaf2661c8\jars\classes.jar
        // ----hexName:1354ba935a43e5a5963b840fd2b5aa15
        // --destName:com.android.support:appcompat-v7:28.0.0
        // --dest:E:\work_pk22\AsmTest\app\build\intermediates\transforms\RouterTransform\debug\37.jar
        return dest
    }
    //从目录中读取class
    void readClassWithPath(File dir) {
        def root = dir.absolutePath
        dir.eachFileRecurse { File file ->
            def filePath = file.absolutePath
            if (!filePath.endsWith(".class")) return
            def className = getClassName(root, filePath)
            if (isSystemClass(className)) return
            addRouteMap(filePath, className)
        }
    }
    //获取类名
    String getClassName(String root, String classPath) {
        System.err.println("------root:" + root + "---classPath：" + classPath)
        return classPath.substring(root.length() + 1, classPath.length() - 6)
                .replaceAll("/", ".")//unix/linuc
                .replaceAll("\\\\", ".")//windows
    }

    boolean isSystemClass(String fileName) {
        for (def exclude : DEFAULT_EXCLUDE) {
            if (fileName.matches(exclude)) {
                return true
            }
        }
        return false
    }

    //默认排除
    static final DEFAULT_EXCLUDE = [
            '^android\\..*',
            '^androidx\\..*',
            '.*\\.R$',
            '.*\\.R\\$.*$',
            '.*\\.BuildConfig$',
    ]
    def routeMap = [:]
    static final ANNOTATION_DESC = "Lio/github/iamyours/router/annotation/Route;"
    static final ROUTE_NAME = "io.github.iamyours:router-api:"
    private static final String ROUTE_MAP_CLASS_NAME = "io.github.iamyours.router.RouteMap"
    private static
    final String ROUTE_MAP_CLASS_FILE_NAME = ROUTE_MAP_CLASS_NAME.replaceAll("\\.", "/") + ".class"
    /**
     * 从class中获取Route注解信息
     * @param filePath
     */
    void addRouteMap(String filePath, String className) {
        addRouteMap(new FileInputStream(filePath), className)
    }

    void addRouteMap(InputStream is, String className) {
        ClassReader classReader = new ClassReader(is)
        ClassNode classNode = new ClassNode()
        classReader.accept(classNode, 1)
        def list = classNode.invisibleAnnotations
        for (AnnotationNode an : list) {
            if (ANNOTATION_DESC == an.desc) {
                System.err.println("------an.value:" + an.toString())
                def path = an.values[1]
                routeMap[path] = className
            }
        }
    }
}
