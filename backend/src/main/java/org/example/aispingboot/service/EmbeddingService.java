package org.example.aispingboot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 文本向量化服务（语义检索）：调用硅基流动 SiliconFlow 的 BGE-M3 embedding 接口。
 *
 * - 与 DeepSeek 对话接口相互独立，仅用于把文本转成向量、计算语义相似度；
 * - 接口不可用 / 未配置 Key / 网络异常时返回 null，调用方自动降级为纯关键词检索，不影响对话主流程。
 */
@Service
public class EmbeddingService {

    @Value("${app.embedding.base-url:https://api.siliconflow.cn}")
    private String baseUrl;

    @Value("${app.embedding.api-key:}")
    private String apiKey;

    @Value("${app.embedding.model:BAAI/bge-m3}")
    private String model;

    private final RestTemplate restTemplate;

    public EmbeddingService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /**
     * 是否已配置可用（未配置 Key 时语义检索整体关闭，退回关键词检索）
     */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 批量生成文本向量；任一环节失败返回 null（调用方降级，不抛出）。
     *
     * @param texts 文本列表（建议 1~20 条）
     * @return 与入参同顺序的向量列表，失败为 null
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (!isEnabled() || texts == null || texts.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", texts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    baseUrl + "/v1/embeddings", HttpMethod.POST, entity, Map.class);
            if (resp.getBody() == null || !(resp.getBody().get("data") instanceof List)) {
                return null;
            }

            // 按 index 排序，保证与入参顺序一致
            Map<Integer, float[]> byIndex = new TreeMap<>();
            for (Object o : (List<?>) resp.getBody().get("data")) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<?, ?> item = (Map<?, ?>) o;
                Object idx = item.get("index");
                Object emb = item.get("embedding");
                if (!(idx instanceof Number) || !(emb instanceof List)) {
                    continue;
                }
                List<?> embList = (List<?>) emb;
                float[] vec = new float[embList.size()];
                for (int i = 0; i < embList.size(); i++) {
                    vec[i] = ((Number) embList.get(i)).floatValue();
                }
                byIndex.put(((Number) idx).intValue(), vec);
            }

            List<float[]> result = new ArrayList<>(byIndex.values());
            return result.isEmpty() ? null : result;
        } catch (RuntimeException e) {
            // 语义检索为增强能力：任何异常都降级，不打断主流程
            return null;
        }
    }

    /**
     * 单条文本向量化；失败返回 null。
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<float[]> list = embedBatch(Collections.singletonList(text.trim()));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * 余弦相似度，映射到 [0,1]（原余弦范围 [-1,1]，归一化便于加权）。
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        double cos = dot / (Math.sqrt(na) * Math.sqrt(nb));
        return (cos + 1) / 2;
    }
}
