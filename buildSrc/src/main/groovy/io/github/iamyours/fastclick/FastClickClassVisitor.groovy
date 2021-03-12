package io.github.iamyours.fastclick

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class FastClickClassVisitor extends ClassVisitor {


    FastClickClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        println("======access:" + access + "--name:" + name + "--desc:" + desc + "--signature:" + signature + "---exceptions:" + exceptions.toString())
        def methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)

//        MethodVisitor myMv = new MethodVisitor(Opcodes.ASM6, mv) {
//            @Override
//            AnnotationVisitor visitAnnotation(String desc1, boolean visible) {
//                System.out.println("visitAnnotation: desc: " + desc1);
//                return super.visitAnnotation(desc1, visible)
//            }
//            @Override
//            void visitCode() {
//                super.visitCode()
//            }
//        }
        if (name == "onClick" && desc == "(Landroid/view/View;)V") {
            return new FastMethodVisitor(api, methodVisitor, access, name, desc)
        } else {
            return methodVisitor;
        }
    }
    //AdviceAdapter是一个继承自MethodVisitor的类,它能够方便的回调方法进入(onMethodEnter)和方法退出(onMethodExit). 我们只需要在方法进入,也就是onMethodEnter方法里面进行插桩即可.
    class FastMethodVisitor extends AdviceAdapter {
        boolean isNoDouble = false //是否是NoDouble
        FastMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(api, methodVisitor, access, name, descriptor)
        }

        @Override
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            println("======descriper:" + descriptor)
            if (descriptor == "Lcom/example/asmtest/NoDoubleClick;") {
                isNoDouble = true
            }
            return super.visitAnnotation(descriptor, visible)
        }
//方法进入
        @Override
        protected void onMethodEnter() {
            super.onMethodEnter()
            println("======isNoDouble:" + isNoDouble)
            if (!isNoDouble) {
                mv.visitMethodInsn(INVOKESTATIC, "com/example/asmtest/FastClickUtil", "isFastDoubleClick", "()Z", false);
                Label label = new Label()
                mv.visitJumpInsn(IFEQ, label)
                mv.visitInsn(RETURN)
                mv.visitLabel(label)
            }
        }

/*
        利用ASM Bytecode outline 插件生成的ASM代码如下(其中Label部分可以忽略):
        {

        }
         */

        /*
        插入之后的代码如下:
         public void onCreate(Bundle savedInstanceState) {

        }
         */

    }
}