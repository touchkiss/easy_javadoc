package com.star.easydoc.proto;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.protobuf.lang.psi.PbDefinition;
import com.intellij.protobuf.lang.psi.PbEnumValue;
import com.intellij.protobuf.lang.psi.PbMapField;
import com.intellij.protobuf.lang.psi.PbNamedElement;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.protobuf.lang.psi.PbSimpleField;
import org.jetbrains.annotations.NotNull;

/**
 * Batch comment generation for all members of a proto container.
 * Finds the nearest message/service/enum from the cursor, collects all members
 * (fields, rpc methods, enum values) recursively, and generates comments for each
 * on a background thread to keep the UI responsive.
 *
 * Isolated in the proto package to prevent NoClassDefFoundError when protoeditor
 * plugin is absent — only instantiated after language ID "protobuf" is confirmed.
 */
public class ProtobufMembersHandler {

    private final ProtobufDocHandler docHandler = new ProtobufDocHandler();

    public void handle(Project project, PsiFile psiFile, PsiElement psiElement) {
        if (psiElement == null) {
            return;
        }

        // Find nearest definition container: message, service, or enum.
        // strict=false means: if psiElement itself is a PbDefinition, use it directly.
        PsiElement container = PsiTreeUtil.getParentOfType(psiElement, PbDefinition.class, false);
        if (container == null) {
            // Cursor is at file level (outside any definition) — use the entire file
            container = psiFile;
        }

        List<PsiElement> members = new ArrayList<>();
        members.addAll(PsiTreeUtil.collectElementsOfType(container, PbSimpleField.class));
        members.addAll(PsiTreeUtil.collectElementsOfType(container, PbMapField.class));
        members.addAll(PsiTreeUtil.collectElementsOfType(container, PbServiceMethod.class));
        members.addAll(PsiTreeUtil.collectElementsOfType(container, PbEnumValue.class));

        if (members.isEmpty()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "生成 Proto 注释", true) {
            private final List<ProtobufDocHandler.ProtoWriteEntry> entries = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                int total = members.size();
                for (int i = 0; i < total; i++) {
                    indicator.checkCanceled();
                    PsiElement member = members.get(i);
                    indicator.setFraction((double) i / total);
                    String name = ReadAction.compute(() -> {
                        PbNamedElement named = PsiTreeUtil.getParentOfType(member, PbNamedElement.class, false);
                        return named != null ? named.getName() : "";
                    });
                    indicator.setText("处理: " + name);

                    ProtobufDocHandler.ProtoWriteEntry entry = ReadAction.compute(
                        () -> docHandler.generateEntry(member));
                    if (entry != null) {
                        entries.add(entry);
                    }
                    indicator.checkCanceled();
                }
            }

            @Override
            public void onSuccess() {
                docHandler.writeCommentsBatch(project, psiFile, entries);
            }
        });
    }
}
