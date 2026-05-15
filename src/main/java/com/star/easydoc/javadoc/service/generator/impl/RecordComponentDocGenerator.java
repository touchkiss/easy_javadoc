package com.star.easydoc.javadoc.service.generator.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.Maps;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.util.ResourceUtil;
import com.star.easydoc.common.Consts;
import com.star.easydoc.common.util.VcsUtil;
import com.star.easydoc.config.EasyDocConfig;
import com.star.easydoc.config.EasyDocConfigComponent;
import com.star.easydoc.javadoc.service.variable.JavadocVariableGeneratorService;
import com.star.easydoc.service.gpt.GptService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Record组件文档生成器
 *
 * @author wangchao
 * @date 2024/03/11
 */
public class RecordComponentDocGenerator extends AbstractDocGenerator {

    private EasyDocConfig config = ServiceManager.getService(EasyDocConfigComponent.class).getState();
    private GptService gptService = ServiceManager.getService(GptService.class);
    private JavadocVariableGeneratorService javadocVariableGeneratorService = ServiceManager.getService(
        JavadocVariableGeneratorService.class);

    private static final String DEFAULT_TEMPLATE = "/** $DOC$ */";

    @Override
    public String generate(PsiElement psiElement) {
        if (!(psiElement instanceof PsiRecordComponent)) {
            return StringUtils.EMPTY;
        }
        PsiRecordComponent component = (PsiRecordComponent)psiElement;

        if (Consts.AI_TRANSLATOR.contains(config.getTranslator())) {
            return generateWithAI(psiElement);
        }

        String template = DEFAULT_TEMPLATE;
        EasyDocConfig.TemplateConfig templateConfig = config.getRecordComponentTemplateConfig();
        if (templateConfig != null && Boolean.FALSE.equals(templateConfig.getIsDefault())) {
            template = templateConfig.getTemplate();
        }

        return javadocVariableGeneratorService.generate(component, template,
            config.getRecordComponentTemplateConfig().getCustomMap(), getComponentInnerVariable(component));
    }

    private String generateWithAI(PsiElement psiElement) {
        String prompt;
        try {
            String folder = Consts.OPENAI_GPT.equals(config.getTranslator()) ? "prompts/openai" : "prompts/chatglm";
            prompt = IOUtils.toString(ResourceUtil.getResourceAsStream(getClass().getClassLoader(), folder, "field.prompt"),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        prompt = prompt.replace("{code}", psiElement.getText());
        return gptService.chat(prompt);
    }

    private Map<String, Object> getComponentInnerVariable(PsiRecordComponent component) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("author", config.getAuthor());
        map.put("componentName", component.getName());
        map.put("componentType", component.getType().getCanonicalText());
        map.put("branch", VcsUtil.getCurrentBranch(component.getProject()));
        map.put("projectName", component.getProject().getName());
        return map;
    }

    @Override
    protected EasyDocConfig getConfig() {
        return config;
    }
}
