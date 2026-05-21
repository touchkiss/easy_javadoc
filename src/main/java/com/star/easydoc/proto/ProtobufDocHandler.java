package com.star.easydoc.proto;

import java.util.Comparator;
import java.util.List;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.protobuf.lang.psi.PbNamedElement;
import com.intellij.util.ThrowableRunnable;
import com.star.easydoc.service.translator.TranslatorService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Handles comment generation for Protocol Buffer (.proto) files.
 * Isolated to prevent NoClassDefFoundError when protoeditor plugin is absent —
 * only loaded when language ID "protobuf" is detected.
 */
public class ProtobufDocHandler {

    private static final Logger LOGGER = Logger.getInstance(ProtobufDocHandler.class);

    private final TranslatorService translatorService = ServiceManager.getService(TranslatorService.class);

    /** Data unit for batch proto comment writes. */
    public static final class ProtoWriteEntry {
        public final PsiElement target;
        public final String comment;

        ProtoWriteEntry(PsiElement target, String comment) {
            this.target = target;
            this.comment = comment;
        }
    }

    /** Single-element entry point (used by GenerateJavadocAction for single cursor position). */
    public void handle(Project project, PsiFile psiFile, PsiElement psiElement) {
        if (psiElement == null) {
            return;
        }

        PbNamedElement target = PsiTreeUtil.getParentOfType(psiElement, PbNamedElement.class, false);
        if (target == null) {
            return;
        }

        String name = target.getName();
        if (StringUtils.isBlank(name)) {
            return;
        }

        // translate() splits camelCase/snake_case/ALL_CAPS into words before calling the API,
        // which is required for names like COMMERCE_SUCCESS that APIs echo back unchanged.
        // trim() removes the leading space that StringUtil.split produces for ALL_CAPS names.
        String translated = StringUtils.trim(translatorService.translate(name, psiElement));
        if (StringUtils.isBlank(translated)) {
            return;
        }

        writeComment(project, psiFile, target, translated);
    }

    /**
     * Translate a single proto element and return a write entry for batch processing.
     * Intended to be called from a background thread inside a ReadAction.
     * Returns null if the element has no name or translation produces a blank result.
     */
    @Nullable
    public ProtoWriteEntry generateEntry(PsiElement psiElement) {
        if (psiElement == null) {
            return null;
        }
        PbNamedElement target = PsiTreeUtil.getParentOfType(psiElement, PbNamedElement.class, false);
        if (target == null) {
            return null;
        }
        String name = target.getName();
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String translated = StringUtils.trim(translatorService.translate(name, psiElement));
        if (StringUtils.isBlank(translated)) {
            return null;
        }
        return new ProtoWriteEntry(target, translated);
    }

    /**
     * Write all proto comments in a single WriteCommandAction.
     * Entries are sorted by offset descending so each insertion does not invalidate
     * the offsets of elements above it in the document.
     */
    public void writeCommentsBatch(Project project, PsiFile psiFile, List<ProtoWriteEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        Document document = psiDocumentManager.getDocument(psiFile);
        if (document == null) {
            return;
        }
        try {
            WriteCommandAction.writeCommandAction(project)
                .withName("Batch Generate Proto Comments")
                .run((ThrowableRunnable<Throwable>) () -> {
                    // Process bottom-to-top so each insertion doesn't shift earlier offsets
                    entries.sort(Comparator.comparingInt(
                        (ProtoWriteEntry en) -> en.target.getTextRange().getStartOffset()).reversed());
                    for (ProtoWriteEntry entry : entries) {
                        if (!entry.target.isValid()) {
                            continue;
                        }
                        int startOffset = entry.target.getTextRange().getStartOffset();
                        int lineNumber = document.getLineNumber(startOffset);
                        int lineStart = document.getLineStartOffset(lineNumber);
                        String linePrefix = document.getText(new TextRange(lineStart, startOffset));
                        String indent = linePrefix.replaceAll("\\S.*", "");
                        document.insertString(lineStart,
                            indent + "/*\n" + indent + " * " + entry.comment + "\n" + indent + " */\n");
                    }
                });
        } catch (Throwable throwable) {
            LOGGER.error("Proto batch comment write error", throwable);
        }
    }

    private void writeComment(Project project, PsiFile psiFile, PsiElement target, String comment) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        Document document = psiDocumentManager.getDocument(psiFile);
        if (document == null) {
            return;
        }
        try {
            WriteCommandAction.writeCommandAction(project).run((ThrowableRunnable<Throwable>) () -> {
                int startOffset = target.getTextRange().getStartOffset();
                int lineNumber = document.getLineNumber(startOffset);
                int lineStart = document.getLineStartOffset(lineNumber);
                String linePrefix = document.getText(new TextRange(lineStart, startOffset));
                String indent = linePrefix.replaceAll("\\S.*", "");
                document.insertString(lineStart,
                    indent + "/*\n" + indent + " * " + comment + "\n" + indent + " */\n");
            });
        } catch (Throwable throwable) {
            LOGGER.error("Proto comment write error", throwable);
        }
    }
}
