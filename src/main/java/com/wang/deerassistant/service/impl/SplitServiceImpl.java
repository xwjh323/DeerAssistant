package com.wang.deerassistant.service.impl;

import com.wang.deerassistant.dto.ChunkPreview;
import com.wang.deerassistant.service.SplitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Service;

import java.util.*;

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

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);

        // Step 1: 扫描所有 Heading
        List<HeadingInfo> headings = collectHeadings(document, markdown);

        // Step 2: 构建区间（heading 到下一个 heading 之间）
        List<Section> sections = buildSections(headings, markdown);

        // Step 3: 过滤层级，只保留 <= maxLevel
        sections.removeIf(s -> s.level > maxLevel);

        // Step 4: 为每段生成 titlePath
        for (Section s : sections) {
            s.titlePath = buildTitlePath(sections, s);
        }

        // Step 5: 如果内容太长，对其进行二次 recursive 切割
        List<ChunkPreview> results = new ArrayList<>();

        int idx = 1;
        for (Section sec : sections) {

            if (sec.content.length() <= chunkSize) {
                ChunkPreview cp = new ChunkPreview();
                cp.setIndex(idx++);
                cp.setTitlePath(sec.titlePath);
                cp.setContent(sec.content);
                cp.setEstimatedTokens(sec.content.length() / 3);
                results.add(cp);
                continue;
            }

            // 二次 recursive 切割
            var splitter = dev.langchain4j.data.document.splitter.DocumentSplitters
                    .recursive(chunkSize, 50);

            var baseDoc = dev.langchain4j.data.document.Document.from(sec.content);
            var segments = splitter.split(baseDoc);

            for (var seg : segments) {
                ChunkPreview cp = new ChunkPreview();
                cp.setIndex(idx++);
                cp.setTitlePath(sec.titlePath);
                cp.setContent(seg.text());
                cp.setEstimatedTokens(seg.text().length() / 3);
                results.add(cp);
            }
        }

        return results;
    }

    // ========== Heading 信息结构 ==========
    private static class HeadingInfo {
        int level;
        String title;
        int startPos;
        int endPos;
    }

    private static class Section {
        int level;
        String title;
        String content;
        List<String> titlePath;
    }

    // ========== Step1：收集所有标题 ==========
    private List<HeadingInfo> collectHeadings(Node document, String content) {

        List<HeadingInfo> list = new ArrayList<>();

        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading h) {
                HeadingInfo info = new HeadingInfo();
                info.level = h.getLevel();

                Node n = h.getFirstChild();
                if (n instanceof Text) {
                    info.title = ((Text) n).getLiteral();
                } else {
                    info.title = "标题解析失败";
                }

                info.startPos = findNodeStartOffset(h, content);
                list.add(info);

                super.visit(h);
            }
        });

        // 补充 endPos
        for (int i = 0; i < list.size(); i++) {
            if (i < list.size() - 1) {
                list.get(i).endPos = list.get(i + 1).startPos;
            } else {
                list.get(i).endPos = content.length();
            }
        }

        return list;
    }

    // 估算节点在原文中的 offset
    private int findNodeStartOffset(Node node, String content) {
        String literal = node.getFirstChild() instanceof Text ?
                ((Text) node.getFirstChild()).getLiteral() : "";

        return content.indexOf(literal);
    }

    // ========== Step2：基于标题构建 sections ==========
    private List<Section> buildSections(List<HeadingInfo> heads, String content) {
        List<Section> list = new ArrayList<>();
        for (HeadingInfo h : heads) {
            Section s = new Section();
            s.level = h.level;
            s.title = h.title;
            s.content = content.substring(h.startPos, h.endPos).trim();
            list.add(s);
        }
        return list;
    }

    // ========== Step3：构造标题路径 ==========
    private List<String> buildTitlePath(List<Section> all, Section sec) {

        List<String> path = new ArrayList<>();

        for (Section s : all) {
            if (s.level <= sec.level && s.content.startsWith(s.title)) {
                path.add(s.title);
            }
            if (s == sec) break;
        }

        return path;
    }
}
