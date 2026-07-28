package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.ChatModelFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantRegistryTest {

    private final ChatModelFactory factory = mock(ChatModelFactory.class);
    private final AlertQueryTools tools = mock(AlertQueryTools.class);
    private final AssistantRegistry registry =
            new AssistantRegistry(factory, tools, new InMemoryChatMemoryStore());

    @Test
    void cachesOneAssistantPerProviderModelPair() {
        when(factory.create(eq("groq"), eq("m1"))).thenReturn(mock(ChatModel.class));
        when(factory.create(eq("groq"), eq("m2"))).thenReturn(mock(ChatModel.class));

        Assistant a1 = registry.get("groq", "m1");
        Assistant a1Again = registry.get("groq", "m1");
        Assistant a2 = registry.get("groq", "m2");

        assertThat(a1).isSameAs(a1Again);
        assertThat(a2).isNotSameAs(a1);
        verify(factory, times(1)).create("groq", "m1"); // built once, then cached
        verify(factory, times(1)).create("groq", "m2");
    }
}
