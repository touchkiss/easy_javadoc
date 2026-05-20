package com.star.easydoc.action;

import java.util.Arrays;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocComment;
import com.star.easydoc.common.util.StringUtil;
import com.star.easydoc.javadoc.service.JavaDocGeneratorServiceImpl;
import com.star.easydoc.service.WriterService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 批量生成类内所有成员（方法、字段）的 Javadoc，不弹对话框
 */
public class GenerateClassMembersJavadocAction extends AnAction {

    private JavaDocGeneratorServiceImpl docGeneratorService = ServiceManager.getService(JavaDocGeneratorServiceImpl.class);
    private WriterService writerService = ServiceManager.getService(WriterService.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(LangDataKeys.PROJECT);
        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        if (project == null || psiFile == null) {
            return;
        }

        // Proto file support: delegate to ProtobufMembersHandler (JVM lazy load, safe in CE)
        if ("protobuf".equalsIgnoreCase(psiFile.getLanguage().getID())) {
            Editor editor = e.getData(LangDataKeys.EDITOR);
            PsiElement elementAt = (editor != null)
                ? psiFile.findElementAt(editor.getCaretModel().getOffset())
                : psiElement;
            new com.star.easydoc.proto.ProtobufMembersHandler().handle(project, psiFile, elementAt);
            return;
        }

        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        if (psiElement instanceof PsiClass) {
            genMembersJavadoc(project, (PsiClass)psiElement);
        } else if (psiElement == null || psiElement instanceof PsiJavaFile) {
            // 光标在文件层级时，处理所有顶级类
            PsiClass[] classes = ((PsiJavaFile)psiFile).getClasses();
            Arrays.stream(classes).forEach(cls -> genMembersJavadoc(project, cls));
        }
        // 其他元素（方法、字段等）静默忽略
    }

    private void genMembersJavadoc(Project project, PsiClass psiClass) {
        Arrays.stream(psiClass.getMethods()).forEach(m -> saveJavadoc(project, m));
        Arrays.stream(psiClass.getFields()).forEach(f -> saveJavadoc(project, f));
        Arrays.stream(psiClass.getInnerClasses()).forEach(inner -> genMembersJavadoc(project, inner));
    }

    private void saveJavadoc(Project project, PsiElement psiElement) {
        if (psiElement == null) {
            return;
        }
        String comment = docGeneratorService.generate(psiElement);
        if (StringUtils.isBlank(comment)) {
            return;
        }
        PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(project);
        PsiDocComment psiDocComment = factory.createDocCommentFromText(comment);
        writerService.writeJavadoc(project, psiElement, psiDocComment, StringUtil.endCount(comment, '\n'));
    }
}
