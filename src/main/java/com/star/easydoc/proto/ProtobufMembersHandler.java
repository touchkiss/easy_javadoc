package com.star.easydoc.proto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.protobuf.lang.psi.PbDefinition;
import com.intellij.protobuf.lang.psi.PbEnumValue;
import com.intellij.protobuf.lang.psi.PbMapField;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.protobuf.lang.psi.PbSimpleField;

/**
 * Batch comment generation for all members of a proto container.
 * Finds the nearest message/service/enum from the cursor, collects all members
 * (fields, rpc methods, enum values) recursively, and generates comments for each.
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

        // Process from highest offset to lowest to avoid stale PSI positions:
        // each document.insertString shifts offsets upward, so processing bottom-up
        // ensures lower elements' original offsets remain valid.
        members.sort(Comparator.comparingInt((PsiElement e) -> e.getTextRange().getStartOffset()).reversed());

        for (PsiElement member : members) {
            docHandler.handle(project, psiFile, member);
        }
    }
}
