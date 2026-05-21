package com.star.easydoc.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.intellij.ide.util.PackageChooserDialog;
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
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPackage;
import com.star.easydoc.common.util.StringUtil;
import com.star.easydoc.config.EasyDocConfig;
import com.star.easydoc.config.EasyDocConfigComponent;
import com.star.easydoc.javadoc.service.JavaDocGeneratorServiceImpl;
import com.star.easydoc.service.PackageInfoService;
import com.star.easydoc.service.WriterService;
import com.star.easydoc.service.translator.TranslatorService;
import com.star.easydoc.view.inner.GenerateAllView;
import com.star.easydoc.view.inner.PackageDescribeView;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;

/**
 * 生成所有文档注释
 *
 * @author <a href="mailto:wangchao.star@gmail.com">wangchao</a>
 * @version 1.0.0
 * @since 2019-10-28 00:26:00
 */
public class GenerateAllJavadocAction extends AnAction {

    private JavaDocGeneratorServiceImpl docGeneratorService = ServiceManager.getService(JavaDocGeneratorServiceImpl.class);
    private WriterService writerService = ServiceManager.getService(WriterService.class);
    private EasyDocConfig config = ServiceManager.getService(EasyDocConfigComponent.class).getState();
    private TranslatorService translatorService = ServiceManager.getService(TranslatorService.class);
    private PackageInfoService packageInfoService = ServiceManager.getService(PackageInfoService.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(LangDataKeys.PROJECT);
        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        if (psiFile instanceof PsiJavaFile) {
            if (psiElement == null) {
                return;
            }
            javadocProcess(project, psiFile, psiElement);
        } else if (psiFile instanceof KtFile) {
            if (psiElement == null) {
                return;
            }
            kdocProcess(project, (KtFile)psiFile, (KtElement)psiElement);
        } else if ("protobuf".equalsIgnoreCase(psiFile.getLanguage().getID())) {
            Editor editor = e.getData(LangDataKeys.EDITOR);
            PsiElement elementAt = (editor != null)
                ? psiFile.findElementAt(editor.getCaretModel().getOffset())
                : psiElement;
            new com.star.easydoc.proto.ProtobufMembersHandler().handle(project, psiFile, elementAt);
        }
    }

    private void javadocProcess(Project project, PsiFile psiFile, PsiElement psiElement) {
        if (psiElement instanceof PsiDirectory) {
            PackageChooserDialog selector = new PackageChooserDialog("选择多个Packages创建package-info", project);
            PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage((PsiDirectory)psiElement);
            if (psiPackage != null) {
                selector.selectPackage(psiPackage.getQualifiedName());
            }
            selector.show();

            List<PsiPackage> packages = selector.getSelectedPackages();
            if (packages == null || packages.isEmpty()) {
                return;
            }
            Map<PsiPackage, String> packMap = packages.stream()
                .collect(Collectors.toMap(s -> s, s -> translatorService.autoTranslate(s.getName(), psiElement)));

            PackageDescribeView packageDescribeView = new PackageDescribeView(packMap);
            if (packageDescribeView.showAndGet()) {
                Map<PsiPackage, String> finalMap = packageDescribeView.getFinalMap();
                for (Map.Entry<PsiPackage, String> entry : finalMap.entrySet()) {
                    packageInfoService.handle(entry.getKey(), entry.getValue());
                }
            }
            return;
        }

        if (!(psiElement instanceof PsiClass)) {
            return;
        }

        GenerateAllView generateAllView = new GenerateAllView();
        generateAllView.getClassCheckBox().setSelected(Optional.ofNullable(config.getGenAllClass()).orElse(false));
        generateAllView.getMethodCheckBox().setSelected(Optional.ofNullable(config.getGenAllMethod()).orElse(false));
        generateAllView.getFieldCheckBox().setSelected(Optional.ofNullable(config.getGenAllField()).orElse(false));
        generateAllView.getInnerClassCheckBox().setSelected(Optional.ofNullable(config.getGenAllInnerClass()).orElse(false));
        generateAllView.getRecordComponentCheckBox().setSelected(Optional.ofNullable(config.getGenAllRecordComponent()).orElse(true));

        if (!generateAllView.showAndGet()) {
            return;
        }

        boolean isGenClass = generateAllView.getClassCheckBox().isSelected();
        boolean isGenMethod = generateAllView.getMethodCheckBox().isSelected();
        boolean isGenField = generateAllView.getFieldCheckBox().isSelected();
        boolean isGenInnerClass = generateAllView.getInnerClassCheckBox().isSelected();
        boolean isGenRecordComponent = generateAllView.getRecordComponentCheckBox().isSelected();

        config.setGenAllClass(isGenClass);
        config.setGenAllMethod(isGenMethod);
        config.setGenAllField(isGenField);
        config.setGenAllInnerClass(isGenInnerClass);
        config.setGenAllRecordComponent(isGenRecordComponent);

        // Collect elements on EDT (read-only PSI traversal, instant)
        List<PsiElement> elements = new ArrayList<>();
        collectElements(elements, (PsiClass)psiElement,
            isGenClass, isGenMethod, isGenField, isGenInnerClass, isGenRecordComponent);

        if (elements.isEmpty()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "生成 Javadoc 注释", true) {
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

    private void kdocProcess(Project project, KtFile psiFile, KtElement psiElement) {
        // TODO: 2022/12/4 实现kdoc批量
    }

    /** Recursively collect PsiElements to generate comments for, on EDT. */
    private void collectElements(List<PsiElement> result, PsiClass psiClass,
        boolean isGenClass, boolean isGenMethod, boolean isGenField,
        boolean isGenInnerClass, boolean isGenRecordComponent) {
        boolean shouldGenClass = isGenClass || (psiClass.isRecord() && isGenRecordComponent);
        if (shouldGenClass) {
            result.add(psiClass);
        }
        if (isGenMethod) {
            result.addAll(Arrays.asList(psiClass.getMethods()));
        }
        if (isGenField) {
            result.addAll(Arrays.asList(psiClass.getFields()));
        }
        if (isGenInnerClass) {
            for (PsiClass inner : psiClass.getInnerClasses()) {
                collectElements(result, inner, isGenClass, isGenMethod, isGenField, true, isGenRecordComponent);
            }
        }
    }
}
