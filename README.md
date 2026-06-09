# Spring AI Model Gateway

基于 Spring Boot、Spring AI 和 Maven 多模块工程实现的通用模型网关基础服务。首期聚焦 LLM/多模态大模型调用治理，为 AI 视频生成解决方案工程提供统一 HTTP API 和 Java Client Starter。

## Modules

- `model-gateway-api`: 稳定 DTO、枚举、错误响应和模型能力协议。
- `model-gateway-core`: 路由、Provider 注册、调用编排、配额、用量记录和降级。
- `model-gateway-provider-openai`: OpenAI-compatible Chat Completions Provider。
- `model-gateway-provider-gemini`: Gemini 原生 Provider，支持文本、图片和视频理解。
- `model-gateway-db-migration`: Flyway 数据库迁移脚本，管理 PostgreSQL schema 和种子数据。
- `model-gateway-service`: 可运行 Spring Boot 网关服务。
- `model-gateway-client-starter`: 内部 Java Client Starter。
- `model-gateway-examples`: AI 视频场景调用示例。

## Quick Start

```bash
export OPENAI_COMPATIBLE_BASE_URL=https://api.openai.com/v1
export OPENAI_COMPATIBLE_API_KEY=your-api-key
export OPENAI_COMPATIBLE_MODEL=gpt-4o-mini

mvn -pl model-gateway-service -am spring-boot:run
```

服务使用 PostgreSQL 持久化运营数据，包括模型路由、Prompt Catalog 和 Prompt 审计记录。默认连接参数如下，可通过环境变量覆盖：

```bash
export MODEL_GATEWAY_DATASOURCE_URL=jdbc:postgresql://localhost:5432/model_gateway
export MODEL_GATEWAY_DATASOURCE_USERNAME=model_gateway
export MODEL_GATEWAY_DATASOURCE_PASSWORD=your-password
```

数据库 schema 和 PromptTemplate 初始数据由 Flyway 管理，迁移脚本位于 `model-gateway-db-migration/src/main/resources/db/migration`。`application.yml` 中的 `model.gateway.routes` 仍会作为运行期兜底种子数据导入数据库；如果数据库里已存在同 ID 路由，不会覆盖已有数据。

调用通用接口：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "STORYBOARD_PLANNING",
    "capability": "TEXT_GENERATION",
    "messages": [
      {"role": "USER", "content": "为一条咖啡广告生成 3 个分镜。"}
    ]
  }'
```

开启 API Key 鉴权：

```bash
export MODEL_GATEWAY_SECURITY_ENABLED=true
export MODEL_GATEWAY_API_KEYS=dev-key
```

开启租户配额：

```bash
export MODEL_GATEWAY_QUOTA_ENABLED=true
export MODEL_GATEWAY_REQUESTS_PER_MINUTE=120
```

## Prompt Registry

系统提示词通过数据库中的 Prompt Catalog 管理，不建议硬编码在业务代码中。初始 Prompt 数据由 Flyway 迁移脚本导入，后续通过管理接口或运营后台维护。Prompt 支持按 `key`、`version`、`scenario`、`locale` 管理模板，并在模型调用前自动注入为系统消息。

PostgreSQL 初始化脚本由 Flyway 管理，`V2__seed_prompt_templates.sql` 包含 `storyboard.system`、`script.system`、`asset-tagging.system`、`shot-prompt-rewrite.system` 等 PromptTemplate 初始数据。

如果请求没有显式指定提示词，网关会按 `scenario` 查找 `default-for-scenario: true` 的模板。例如 `STORYBOARD_PLANNING` 会默认使用 `storyboard.system`。

也可以在请求 `metadata` 中显式选择提示词并传入模板变量：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "STORYBOARD_PLANNING",
    "capability": "JSON_MODE",
    "messages": [
      {"role": "USER", "content": "为一条咖啡广告生成 6 个分镜。"}
    ],
    "metadata": {
      "promptKey": "storyboard.system",
      "promptVersion": "v1",
      "promptVariables": {
        "videoType": "广告短片",
        "language": "zh-CN"
      }
    }
  }'
```

管理接口：

- `GET /admin/v1/prompts`: 查看当前 Prompt Catalog。
- `POST /admin/v1/prompts`: 新增提示词模板。
- `PATCH /admin/v1/prompts/{key}/{version}`: 更新指定版本模板。

每次通过仓库保存 Prompt 时都会写入 `prompt_template_audit` 审计表，便于追踪模板版本变更。模型路由同样通过数据库表 `model_routes` 持久化，管理接口 `GET/POST/PATCH /admin/v1/model-routes` 读写数据库。

内部 Java Client 支持读取 Prompt Catalog：

```java
modelGatewayClient.prompts()
    .map(prompts -> prompts.stream()
        .filter(prompt -> prompt.key().equals("storyboard.system"))
        .findFirst()
        .orElseThrow());
```

`model-gateway-examples` 中的示例会先读取数据库中的 `PromptTemplate`，再将 `promptKey` 和 `promptVersion` 放入请求 `metadata`，由网关服务端负责注入系统提示词。

## DeepSeek

DeepSeek Provider 和路由已作为配置模板内置。启用后，客户端既可以通过 `modelHint: deepseek-chat` 显式选择 DeepSeek，也可以让网关根据 `scenario`、`capability` 和路由优先级自动选择 DeepSeek。启用方式：

```bash
export DEEPSEEK_ENABLED=true
export DEEPSEEK_API_KEY=your-deepseek-api-key

mvn -pl model-gateway-service -am spring-boot:run
```

请求时可以通过 `modelHint` 指定 DeepSeek 路由：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "STORYBOARD_PLANNING",
    "capability": "TEXT_GENERATION",
    "modelHint": "deepseek-chat",
    "messages": [
      {"role": "USER", "content": "为一条科幻短片生成 6 个分镜。"}
    ]
  }'
```

如需使用推理模型，可指定 `modelHint` 为 `deepseek-reasoner`，或通过 `DEEPSEEK_REASONER_ENABLED=false` 单独关闭该路由。

## Qwen

Qwen 通过阿里云 DashScope OpenAI 兼容模式接入，默认配置已内置但默认禁用。启用方式：

```bash
export QWEN_ENABLED=true
export QWEN_API_KEY=your-dashscope-api-key

mvn -pl model-gateway-service -am spring-boot:run
```

请求时可以通过 `modelHint` 指定 Qwen 路由：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "SCRIPT_GENERATION",
    "capability": "TEXT_GENERATION",
    "modelHint": "qwen-plus",
    "messages": [
      {"role": "USER", "content": "为一条国风短视频生成 30 秒脚本。"}
    ]
  }'
```

视觉理解场景可以指定 `modelHint` 为 `qwen-vl`，默认模型为 `qwen-vl-max`。如需单独关闭视觉路由，可设置 `QWEN_VL_ENABLED=false`。

## Gemini

Gemini 通过原生 `generateContent` / `streamGenerateContent` 接口接入，支持文本、图片和视频理解。默认配置已内置但默认禁用。启用方式：

```bash
export GEMINI_ENABLED=true
export GEMINI_API_KEY=your-gemini-api-key

mvn -pl model-gateway-service -am spring-boot:run
```

请求时可以通过 `modelHint` 指定 Gemini 路由：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "STORYBOARD_PLANNING",
    "capability": "TEXT_GENERATION",
    "modelHint": "gemini-flash",
    "messages": [
      {"role": "USER", "content": "为一条旅行 vlog 生成 5 个分镜和旁白。"}
    ]
  }'
```

需要更强推理能力时可指定 `modelHint` 为 `gemini-pro`。多模态视觉理解场景可指定 `modelHint` 为 `gemini-vision`，默认模型同样使用 `gemini-2.5-flash`。

图片或视频理解请求可以在 `messages[].media` 中传入媒体。小文件可传 `base64`，大文件建议先上传到 Gemini Files API 后通过 `metadata.geminiFileUri` 传入文件 URI：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "ASSET_TAGGING",
    "capability": "VISION_UNDERSTANDING",
    "modelHint": "gemini-vision",
    "messages": [
      {
        "role": "USER",
        "content": "请分析这个视频素材，输出场景、主体、镜头运动和可用于视频生成的提示词。",
        "media": [
          {
            "type": "video",
            "mimeType": "video/mp4",
            "metadata": {
              "geminiFileUri": "files/your-uploaded-video"
            }
          }
        ]
      }
    ]
  }'
```
