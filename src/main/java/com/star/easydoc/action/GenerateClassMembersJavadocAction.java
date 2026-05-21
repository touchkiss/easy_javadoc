package com.star.easydoc.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiNamedElement;
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

        // Collect members on EDT (read-only PSI traversal, instant)
        List<PsiElement> elements = new ArrayList<>();
        if (psiElement instanceof PsiClass) {
            collectMembers(elements, (PsiClass) psiElement);
        } else if (psiElement == null || psiElement instanceof PsiJavaFile) {
            PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
            Arrays.stream(classes).forEach(cls -> collectMembers(elements, cls));
        }
        // 其他元素（方法、字段等）静默忽略

        if (elements.isEmpty()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "生成成员 Javadoc 注释", true) {
            private final List<WriterService.WriteEntry> entries = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                int total = elements.size();
                for (int i = 0; i < total; i++) {
                    indicator.checkCanceled();
                    PsiElement elem = elements.get(i);
                    indicator.setFraction((double) i / total);
                    String name = ReadAction.compute(() ->
                        elem instanceof PsiNamedElement ? ((PsiNamedElement) elem).getName() : "");
                    indicator.setText("处理: " + name);

                    String comment = ReadAction.compute(() -> docGeneratorService.generate(elem));
                    if (StringUtils.isBlank(comment)) {
                        continue;
                    }
                    entries.add(new WriterService.WriteEntry(elem, comment, StringUtil.endCount(comment, '\n')));
                    indicator.checkCanceled();
                }
            }

            @Override
            public void onSuccess() {
                writerService.writeJavadocBatch(project, entries);
            }
        });
    }

    private void collectMembers(List<PsiElement> result, PsiClass psiClass) {
        result.addAll(Arrays.asList(psiClass.getMethods()));
        result.addAll(Arrays.asList(psiClass.getFields()));
        for (PsiClass inner : psiClass.getInnerClasses()) {
            collectMembers(result, inner);
        }
    }
}
