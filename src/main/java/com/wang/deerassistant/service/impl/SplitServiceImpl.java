package com.wang.deerassistant.service.impl;

import com.wang.deerassistant.dto.ChunkPreview;
import com.wang.deerassistant.service.SplitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplitServiceImpl implements SplitService {

    @Override
    public List<ChunkPreview> previewSplit(String content, Map<String, Object> config) {

        String mode = (String) config.getOrDefault("mode", "auto");
        if ("heading".equals(mode)) {
            return headingSplit(content, config);
        } else {
            return autoSplit(content, config);
        }
    }

    // =============================
    // 1. 自动分段（你现有的逻辑）
    // =============================
    private List<ChunkPreview> autoSplit(String content, Map<String, Object> config) {

        int chunkSize = (int) config.getOrDefault("chunkSize", 300);
        int overlap = (int) config.getOrDefault("chunkOverlap", 50);

        // 用 LangChain4j 的 recursive splitter 做预览
        var splitter = dev.langchain4j.data.document.splitter.DocumentSplitters
                .recursive(chunkSize, overlap);

        var baseDoc = dev.langchain4j.data.document.Document.from(content);
        var segments = splitter.split(baseDoc);

        List<ChunkPreview> list = new ArrayList<>();
        int idx = 1;
        for (var seg : segments) {

            ChunkPreview cp = new ChunkPreview();
            cp.setIndex(idx++);
            cp.setTitlePath(Collections.emptyList());
            cp.setContent(seg.text());
            cp.setEstimatedTokens(seg.text().length() / 3);

            list.add(cp);
        }
        return list;
    }

    // =============================
    // 2. Markdown 层级分段（核心）
    // =============================
    private List<ChunkPreview> headingSplit(String markdown, Map<String, Object> config) {

        int maxLevel = (int) config.getOrDefault("maxLevel", 3);
        int chunkSize = (int) config.getOrDefault("chunkSize", 500);

        // 1. 按行拆分
        String[] lines = markdown.split("\n");

        // 2. 正则识别标题行
        Pattern headingPattern = Pattern.compile("^(#{1,6})\\s+(.+)$");
        List<HeadingLine> headings = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            Matcher m = headingPattern.matcher(lines[i]);
            if (m.find()) {
                int level = m.group(1).length();
                String title = m.group(2).trim();
                headings.add(new HeadingLine(level, title, i));
            }
        }

        // 无标题则整个内容作为一级标题
        if (headings.isEmpty()) {
            return autoSplit(markdown, config);
        }

        // 3. 构建 section 列表（每个标题到下一个标题前为一个 section）
        List<SectionBlock> sections = new ArrayList<>();

        for (int i = 0; i < headings.size(); i++) {

            HeadingLine h = headings.get(i);
            int startLine = h.lineIndex;                    // 标题所在行
            int endLine = (i < headings.size() - 1)
                    ? headings.get(i + 1).lineIndex - 1     // 前一个标题的内容区间结束
                    : lines.length - 1;                     // 最后一段到文档末尾

            // 构建内容
            StringBuilder sb = new StringBuilder();
            for (int j = startLine; j <= endLine; j++) {
                sb.append(lines[j]).append("\n");
            }

            sections.add(new SectionBlock(h.level, h.title, sb.toString().trim(), h.lineIndex));
        }

        // 4. 用标题栈构建 titlePath（正确做法）
        Deque<String> titleStack = new ArrayDeque<>();
        List<SectionBlock> withPath = new ArrayList<>();

        for (SectionBlock sec : sections) {
            // 若当前标题层级 ≤ 栈顶层级，则 pop 掉深层
            while (!titleStack.isEmpty() && sec.level <= stackLevel(titleStack.peek())) {
                titleStack.pop();
            }

            // push 当前标题
            titleStack.push("#".repeat(sec.level) + " " + sec.title);

            // 构造 titlePath（倒序输出为从 H1 → H2 → H3）
            List<String> path = new ArrayList<>(titleStack);
            Collections.reverse(path);

            sec.titlePath = path;
            withPath.add(sec);
        }

        // 5. 过滤 maxLevel
        withPath.removeIf(s -> s.level > maxLevel);

        // 6. 最终生成 chunk 列表 + 二次切分
        List<ChunkPreview> result = new ArrayList<>();
        int index = 0;

        for (SectionBlock sec : withPath) {

            // 简单长度判断
            if (sec.content.length() <= chunkSize) {
                ChunkPreview cp = new ChunkPreview();
                cp.setIndex(index++);
                cp.setTitlePath(sec.titlePath);
                cp.setContent(sec.content);
                cp.setEstimatedTokens(sec.content.length() / 3);
                result.add(cp);
                continue;
            }

            // 长内容 → recursive 二次切分
            var splitter = dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(
                    chunkSize,
                    (int) config.getOrDefault("chunkOverlap", 50)
            );

            var doc = dev.langchain4j.data.document.Document.from(sec.content);
            var pieces = splitter.split(doc);

            for (var piece : pieces) {
                ChunkPreview cp = new ChunkPreview();
                cp.setIndex(index++);
                cp.setTitlePath(sec.titlePath);
                cp.setContent(piece.text());
                cp.setEstimatedTokens(piece.text().length() / 3);
                result.add(cp);
            }
        }

        return result;
    }

    /**
     * 辅助类：标题行（level + text + 行号）
     */
    private static class HeadingLine {
        int level;
        String title;
        int lineIndex;
        HeadingLine(int level, String title, int lineIndex) {
            this.level = level;
            this.title = title;
            this.lineIndex = lineIndex;
        }
    }

    /**
     * 辅助类：完整 section（标题 + 内容 + 路径）
     */
    private static class SectionBlock {
        int level;
        String title;
        String content;
        int lineIndex;
        List<String> titlePath;
        SectionBlock(int level, String title, String content, int lineIndex) {
            this.level = level;
            this.title = title;
            this.content = content;
            this.lineIndex = lineIndex;
        }
    }

    /**
     * 辅助工具：通过标题文字推测 level，例如 "# H1" → level=1
     */
    private int stackLevel(String t) {
        int count = 0;
        for (char c : t.toCharArray()) {
            if (c == '#') count++;
            else break;
        }
        return count;
    }
}
