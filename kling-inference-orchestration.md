# Kling Unified Inference Orchestration

`kling-inference-*` 是可灵统一内部推理编排层的独立模块组，定位为开放平台、创作工具、模型网关和内部运营工具共享的视频生成任务底座。它不承担外部开放平台的计费、鉴权、商业限流职责，而是抽象长耗时推理任务的提交、状态流转、等待、取消、事件和结果读取。

## 模块划分

- `kling-inference-contract`: 面向服务端和 SDK 共享的外部契约，包括请求、任务、状态、事件、资产和结果模型。
- `kling-inference-core`: 编排核心接口，包括 `InferenceOrchestrationService`、`InferenceBackendClient`、`InferenceJobRepository` 和 `InferenceEventPublisher`。
- `kling-inference-service`: 独立 Spring Boot 服务，暴露内部 HTTP/SSE API。当前包含本地开发实现，生产环境应替换为数据库任务表、消息事件总线和真实底层推理服务。
- `kling-inference-client-sdk`: 面向调用方的 Java SDK，封装 Job API、Wait API、Events API，并提供 `CompletableFuture` 风格调用体验。

## 外部接口契约

### 标准异步 Job API

提交任务：

```http
POST /internal/v1/video-generations
Content-Type: application/json
```

查询任务：

```http
GET /internal/v1/video-generations/{jobId}
```

取消任务：

```http
POST /internal/v1/video-generations/{jobId}:cancel
POST /internal/v1/video-generations/{jobId}/cancel
```

订阅事件：

```http
GET /internal/v1/video-generations/{jobId}/events
Accept: text/event-stream
```

任务状态统一为：

```text
CREATED -> VALIDATING -> QUEUED -> SCHEDULING -> RUNNING -> SUCCEEDED
                                                       -> FAILED
                                                       -> CANCELLED
                                                       -> TIMEOUT
```

### Wait / Long Poll API

提交并等待短时间完成：

```http
POST /internal/v1/video-generations?wait=true&timeoutSeconds=30
```

等待已有任务状态变化或完成：

```http
GET /internal/v1/video-generations/{jobId}:wait?timeoutSeconds=30
GET /internal/v1/video-generations/{jobId}/wait?timeoutSeconds=30
```

Wait API 不要求底层推理变成真正同步；它只是由编排层屏蔽消息回调、轮询和状态存储细节。调用方可以拿到“类似同步”的体验，超时后仍可继续使用 `jobId` 查询或再次等待。

## SDK 使用示例

```java
KlingInferenceClient client = new WebClientKlingInferenceClient(
        KlingInferenceClientOptions.of("http://localhost:8091")
);

VideoGenerationRequest request = new VideoGenerationRequest(
        "req-001",
        "idem-001",
        new InferenceCaller("model-gateway", "INTERNAL_SERVICE", "tenant-a", "project-a", "user-a", Map.of()),
        GenerationType.TEXT_TO_VIDEO,
        "ai-video-storyboard",
        "kling-video",
        "v1",
        "A cinematic shot of a futuristic city at sunrise",
        null,
        List.of(),
        5,
        "16:9",
        "1080p",
        null,
        5,
        null,
        Map.of(),
        Map.of()
);

CompletableFuture<VideoGenerationResult> future = client.generateAsync(request);
```

## 后续生产实现建议

- `InferenceJobRepository` 使用 PostgreSQL/MyBatis-Plus 或团队统一任务存储实现。
- `InferenceEventPublisher` 对接 Kafka、RocketMQ、Pulsar 或内部事件总线。
- `InferenceBackendClient` 对接真正的可灵内部推理 API，不复用开放平台的商业计费、外部鉴权限流链路。
- 开放平台、创作工具和模型网关都只调用本编排层；各自保留自己的产品鉴权、计费、配额和审计逻辑。
