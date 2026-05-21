package com.star.easydoc.service;

import java.util.List;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaDocumentedElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ThrowableRunnable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;

/**
 * 写入服务
 *
 * @author wangchao
 * @date 2019/08/25
 */
public class WriterService {
    private static final Logger LOGGER = Logger.getInstance(WriterService.class);

    /** 批量写入的数据单元，commentText 为原始字符串，避免在后台线程创建 PsiDocComment */
    public static final class WriteEntry {
        final PsiElement element;
        final String commentText;
        final int newlineCount;

        public WriteEntry(PsiElement element, String commentText, int newlineCount) {
            this.element = element;
            this.commentText = commentText;
            this.newlineCount = newlineCount;
        }
    }

    /**
     * 批量写入 Javadoc，在单次 WriteCommandAction 内完成，产生单条撤销记录。
     * 必须在 EDT 上调用（Task.Backgroundable.onSuccess 已保证）。
     */
    public void writeJavadocBatch(Project project, List<WriteEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        try {
            WriteCommandAction.writeCommandAction(project)
                .withName("Batch Generate Javadoc")
                .run((ThrowableRunnable<Throwable>) () -> {
                    PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(project);
                    for (WriteEntry entry : entries) {
                        if (!entry.element.isValid() || entry.element.getContainingFile() == null) {
                            continue;
                        }
                        PsiDocComment comment = factory.createDocCommentFromText(entry.commentText);
                        doWriteJavadoc(entry.element, comment, entry.newlineCount);
                    }
                });
        } catch (Throwable throwable) {
            LOGGER.error("batch write javadoc error", throwable);
        }
    }

    private void doWriteJavadoc(PsiElement psiElement, PsiDocComment comment, int emptyLineNum) {
        if (psiElement instanceof PsiJavaDocumentedElement) {
            PsiDocComment existing = ((PsiJavaDocumentedElement) psiElement).getDocComment();
            if (existing == null) {
                psiElement.getNode().addChild(comment.getNode(), psiElement.getFirstChild().getNode());
            } else {
                existing.replace(comment);
            }
        }
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(psiElement.getProject());
        PsiElement javadocElement = psiElement.getFirstChild();
        int startOffset = javadocElement.getTextOffset();
        int endOffset = startOffset + javadocElement.getText().length();
        codeStyleManager.reformatText(psiElement.getContainingFile(), startOffset, endOffset + 1);
        if (emptyLineNum > 0) {
            PsiElement[] children = psiElement.getChildren();
            if (children.length > 1 && children[1] instanceof PsiWhiteSpaceImpl) {
                PsiWhiteSpaceImpl ws = (PsiWhiteSpaceImpl) children[1];
                String space = StringUtils.repeat("\n", emptyLineNum + 1);
                String exists = StringUtils.stripStart(ws.getText(), "\n");
                ws.replaceWithText(space + exists);
            }
        }
    }

    /**
     * 写入文档注释
     *
     * @param project 工程
     * @param psiElement psi
     * @param comment 文档注释
     * @param emptyLineNum 空行数量
     */
    public void writeJavadoc(Project project, PsiElement psiElement, PsiDocComment comment, int emptyLineNum) {
        try {
            WriteCommandAction.writeCommandAction(project).run(
                (ThrowableRunnable<Throwable>)() -> {
                    if (psiElement.getContainingFile() == null) {
                        return;
                    }

                    // 写入文档注释
                    if (psiElement instanceof PsiJavaDocumentedElement) {
                        PsiDocComment psiDocComment = ((PsiJavaDocumentedElement)psiElement).getDocComment();
                        if (psiDocComment == null) {
                            psiElement.getNode().addChild(comment.getNode(), psiElement.getFirstChild().getNode());
                        } else {
                            psiDocComment.replace(comment);
                        }
                    }

                    // 格式化文档注释
                    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(psiElement.getProject());
                    PsiElement javadocElement = psiElement.getFirstChild();
                    int startOffset = javadocElement.getTextOffset();
                    int endOffset = javadocElement.getTextOffset() + javadocElement.getText().length();
                    codeStyleManager.reformatText(psiElement.getContainingFile(), startOffset, endOffset + 1);

                    // 添加空行
                    if (emptyLineNum > 0) {
                        PsiElement whiteSpaceElement = psiElement.getChildren()[1];
                        if (whiteSpaceElement instanceof PsiWhiteSpaceImpl) {
                            // 修改whiteSpace
                            String space = StringUtils.repeat("\n", emptyLineNum + 1);
                            String exists = StringUtils.stripStart(whiteSpaceElement.getText(), "\n");
                            ((PsiWhiteSpaceImpl)whiteSpaceElement).replaceWithText(space + exists);
                        }
                    }
                });
        } catch (Throwable throwable) {
            LOGGER.error("write code error", throwable);
        }
    }

    /**
     * 写入
     *
     * @param project 工程
     * @param editor 编辑器
     * @param text 文本
     */
    public void write(Project project, Editor editor, String text) {
        if (project == null || editor == null || StringUtils.isBlank(text)) {
            return;
        }
        try {
            WriteCommandAction.writeCommandAction(project).run(
                (ThrowableRunnable<Throwable>)() -> {
                    int start = editor.getSelectionModel().getSelectionStart();
                    EditorModificationUtil.insertStringAtCaret(editor, text);
                    editor.getSelectionModel().setSelection(start, start + text.length());
                });
        } catch (Throwable throwable) {
            LOGGER.error("write code error", throwable);
        }
    }

    /**
     * 写入kdoc
     *
     * @param project 工程
     * @param ktElement kt
     * @param comment kdoc
     */
    public void writeKdoc(Project project, KtElement ktElement, KDoc comment) {
        try {
            WriteCommandAction.writeCommandAction(project).run(
                (ThrowableRunnable<Throwable>)() -> {
                    if (ktElement.getContainingFile() == null) {
                        return;
                    }

                    // 写入文档注释
                    if (ktElement instanceof KtDeclaration) {
                        KDoc kDoc = ((KtDeclaration)ktElement).getDocComment();
                        if (kDoc == null) {
                            ktElement.getNode().addChild(comment.getNode(), ktElement.getFirstChild().getNode());
                        } else {
                            kDoc.replace(comment);
                        }
                    }

                    // 格式化文档注释
                    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(ktElement.getProject());
                    PsiElement javadocElement = ktElement.getFirstChild();
                    int startOffset = javadocElement.getTextOffset();
                    int endOffset = javadocElement.getTextOffset() + javadocElement.getText().length();
                    codeStyleManager.reformatText(ktElement.getContainingFile(), startOffset, endOffset + 1);
                }
            );
        } catch (Throwable throwable) {
            LOGGER.error("写入错误", throwable);
        }
    }

}
