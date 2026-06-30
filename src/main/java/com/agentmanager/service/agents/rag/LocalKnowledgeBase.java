package com.agentmanager.service.agents.rag;

import com.agentmanager.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class LocalKnowledgeBase {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-z0-9]+");
    private final ResourceLoader resourceLoader;
    private final String knowledgeBaseLocation;
    private List<KnowledgeChunk> chunks = List.of();

    public LocalKnowledgeBase(
            ResourceLoader resourceLoader,
            @Value("${imd.knowledge-base-path:classpath:knowledge/imd_knowledge_base.md}") String knowledgeBaseLocation
    ) {
        this.resourceLoader = resourceLoader;
        this.knowledgeBaseLocation = knowledgeBaseLocation;
    }

    @PostConstruct
    void load() {
        Resource resource = resolveResource();
        try {
            if (!resource.exists()) {
                throw new BusinessException("Base local do IMD nao encontrada: " + knowledgeBaseLocation);
            }
            try (InputStream inputStream = resource.getInputStream()) {
                String markdown = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                chunks = splitIntoChunks(markdown);
            }
        } catch (IOException exception) {
            throw new BusinessException("Erro ao carregar base local do IMD: " + exception.getMessage());
        }
    }

    private Resource resolveResource() {
        if (knowledgeBaseLocation.startsWith("classpath:")
                || knowledgeBaseLocation.startsWith("file:")
                || knowledgeBaseLocation.startsWith("http:")
                || knowledgeBaseLocation.startsWith("https:")) {
            return resourceLoader.getResource(knowledgeBaseLocation);
        }

        Path path = Path.of(knowledgeBaseLocation);
        if (Files.exists(path)) {
            return resourceLoader.getResource("file:" + path.toAbsolutePath());
        }
        return resourceLoader.getResource(knowledgeBaseLocation);
    }

    public List<KnowledgeChunk> retrieve(String query, int limit) {
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty() || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .map(chunk -> new KnowledgeChunk(
                        chunk.title(),
                        chunk.content(),
                        score(queryTerms, chunk)
                ))
                .filter(chunk -> chunk.score() > 0)
                .sorted(Comparator.comparingInt(KnowledgeChunk::score).reversed())
                .limit(limit)
                .toList();
    }

    public String formatChunks(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "Nenhum trecho relevante encontrado na base local.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            builder.append("Trecho ").append(i + 1).append(" - ")
                    .append(chunk.title()).append(":\n")
                    .append(chunk.content()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<KnowledgeChunk> splitIntoChunks(String markdown) {
        List<KnowledgeChunk> loadedChunks = new ArrayList<>();
        String currentTitle = "Base de Conhecimento do IMD";
        StringBuilder currentContent = new StringBuilder();

        for (String line : markdown.split("\\R")) {
            if (line.startsWith("## ")) {
                addChunk(loadedChunks, currentTitle, currentContent);
                currentTitle = line.replaceFirst("^#+\\s*", "").trim();
                currentContent = new StringBuilder();
            }
            currentContent.append(line).append('\n');
        }
        addChunk(loadedChunks, currentTitle, currentContent);
        return loadedChunks;
    }

    private void addChunk(List<KnowledgeChunk> loadedChunks, String title, StringBuilder content) {
        String value = content.toString().trim();
        if (!value.isBlank()) {
            loadedChunks.add(new KnowledgeChunk(title, value, 0));
        }
    }

    private int score(Set<String> queryTerms, KnowledgeChunk chunk) {
        Set<String> chunkTerms = terms(chunk.title() + " " + chunk.content());
        int score = 0;
        for (String term : queryTerms) {
            if (chunkTerms.contains(term)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> terms(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String[] words = WORD_SPLIT.split(normalized);
        Set<String> terms = new LinkedHashSet<>();
        for (String word : words) {
            if (word.length() > 2) {
                terms.add(word);
            }
        }
        return terms;
    }
}
