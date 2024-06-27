package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DynamicPartialCode extends DefaultCode {
    public DynamicPartialCode(TemplateContext tc, DefaultMustacheFactory df, String name) {
        super(tc, df, null, name, ">*");
    }

    private final ConcurrentMap<String, PartialCode> partialCodeMap = new ConcurrentHashMap<>();

    @Override
    public Writer execute(Writer writer, List<Object> scopes) {
        String partialName = (String) binding.get(scopes);
        if (partialName == null) {
            return appendText(writer);
        }
        PartialCode partialCode = partialCodeMap.computeIfAbsent(partialName, name -> {
            PartialCode pc = new PartialCode(tc, df, partialName);
            pc.append(appended);
            pc.init();
            return pc;
        });
        return partialCode.execute(writer, scopes);
    }
}
