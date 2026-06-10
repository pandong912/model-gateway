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

外部 API 使用 `KlingGenerationRequest` 作为通用任务信封，功能差异由强类型 payload 承载。例如视频生成使用 `VideoGenerationPayload`，图片生成使用 `ImageGenerationPayload`。

```java
KlingInferenceClient client = new WebClientKlingInferenceClient(
        KlingInferenceClientOptions.of("http://localhost:8091")
);

KlingGenerationRequest<? extends KlingGenerationPayload> request = KlingGenerationRequestBuilder
        .textToVideo("A cinematic shot of a futuristic city at sunrise")
        .idempotencyKey("idem-001")
        .caller(new InferenceCaller("model-gateway", "INTERNAL_SERVICE", "tenant-a", "project-a", "user-a", Map.of()))
        .scenario("ai-video-storyboard")
        .durationSeconds(5)
        .aspectRatio("16:9")
        .resolution("1080p")
        .build();

CompletableFuture<KlingGenerationResult> future = client.generateAsync(request);
```

## 当前生产闭环实现

- `kling_inference_jobs` 持久化任务快照，支持 `caller_id + idempotency_key` 幂等提交和 `backend_task_id` 反查。
- `kling_inference_events` 持久化任务事件，作为 Wait/SSE 唤醒和后续审计、排障的数据基础。
- 默认启用 `MockKlingInferenceBackendClient`，用于本地验证 `submit -> backend event -> wait -> result` 链路。
- 启用 `real-kling-backend` profile 后，服务会切换到 `KlingInternalInferenceBackendClient`，通过配置对接正式内部推理 API。

真实 backend 配置：

```yaml
kling:
  inference:
    backend:
      base-url: ${KLING_INFERENCE_BACKEND_BASE_URL:http://localhost:8099}
      text-to-video-submit-path: ${KLING_INFERENCE_BACKEND_TEXT_TO_VIDEO_SUBMIT_PATH:/v1/videos/text2video}
      image-to-video-submit-path: ${KLING_INFERENCE_BACKEND_IMAGE_TO_VIDEO_SUBMIT_PATH:/v1/videos/image2video}
      image-generation-submit-path: ${KLING_INFERENCE_BACKEND_IMAGE_GENERATION_SUBMIT_PATH:/v1/images/generations}
      image-editing-submit-path: ${KLING_INFERENCE_BACKEND_IMAGE_EDITING_SUBMIT_PATH:/v1/images/edits}
      cancel-path-template: ${KLING_INFERENCE_BACKEND_CANCEL_PATH_TEMPLATE:/internal/inference/tasks/{backendTaskId}/cancel}
      access-key: ${KLING_INFERENCE_BACKEND_ACCESS_KEY:}
      secret-key: ${KLING_INFERENCE_BACKEND_SECRET_KEY:}
```

`KlingInternalInferenceBackendClient` 会先根据 `GenerationType` 选择不同 submit path，并把强类型 payload 映射成开放平台请求体。调用开放平台时会用 `access-key` / `secret-key` 生成 HS256 JWT，并通过 `Authorization: Bearer <token>` 传递。当前请求映射仍是占位骨架，后续需要按开放平台实际字段名和响应字段补齐。

底层推理完成、失败或进度更新后，可以通过内部事件入口回写编排层：

```http
POST /internal/v1/backend-events
Content-Type: application/json
```

## 后续生产实现建议

- 如果团队已有统一事件总线，可以把当前 `PersistentInferenceEventPublisher` 的本地广播扩展为 Kafka、RocketMQ、Pulsar 或内部事件总线。
- `KlingInternalInferenceBackendClient` 需要根据团队正式内部推理 API 的请求/响应字段做一次精确适配。
- 开放平台、创作工具和模型网关都只调用本编排层；各自保留自己的产品鉴权、计费、配额和审计逻辑。
