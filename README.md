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

mvn -pl model-gateway-db-migration -am spring-boot:run
mvn -pl model-gateway-service -am spring-boot:run
```

服务使用 PostgreSQL 持久化运营数据，包括模型路由、Prompt Catalog 和 Prompt 审计记录。默认连接参数如下，可通过环境变量覆盖：

```bash
export MODEL_GATEWAY_DATASOURCE_URL=jdbc:postgresql://localhost:5432/model_gateway
export MODEL_GATEWAY_DATASOURCE_USERNAME=model_gateway
export MODEL_GATEWAY_DATASOURCE_PASSWORD=your-password
```

数据库 schema 和 PromptTemplate 初始数据由 `model-gateway-db-migration` 的独立启动入口运行 Flyway 管理，迁移脚本位于 `model-gateway-db-migration/src/main/resources/db/migration`。模型路由以数据库为准，初始化数据可从 `model-gateway-db-migration/src/main/resources/db/data/seed_model_routes.sql` 手工导入；`model-gateway-service` 启动时不再自动执行 Flyway 或写入默认路由。

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

提示词通过数据库中的 Prompt Catalog 管理，不建议硬编码在业务代码中。Prompt 支持按 `key`、`version`、`scenario`、`locale` 和 `role` 管理模板，既可以保存 `SYSTEM` 提示词，也可以保存 `USER` 提示词。后续模板变更可通过管理接口或运营后台维护。

PostgreSQL schema 迁移脚本由 Flyway 管理，`db/data` 下的 SQL 可手工导入 PromptTemplate 初始数据。例如 `seed_ecommerce_asset_design_prompt.sql` 包含 `ecommerce-asset-design.user` 用户提示词模板。

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

`model-gateway-examples` 中的示例会先读取数据库中的 `PromptTemplate`。系统提示词示例会将 `promptKey` 和 `promptVersion` 放入请求 `metadata`，由网关服务端负责注入；用户提示词示例 `EcommerceAssetDesignExample` 会在客户端把商品信息渲染进 `USER` 模板，再发送给 Gemini 3.1 Pro Preview 生成电商素材设计计划。

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

如果只有 Google Cloud service account credential，没有 Gemini API Key，可以使用 `vertex-gemini` provider。将 service account JSON 保存到本机或部署环境的安全路径，避免提交到 Git，然后配置：

```bash
export VERTEX_GEMINI_PROJECT_ID=your-gcp-project-id
export VERTEX_GEMINI_LOCATION=us-central1
export VERTEX_GEMINI_CREDENTIALS_PATH=/secure/path/service-account.json

mvn -pl model-gateway-service -am spring-boot:run
```

`vertex-gemini` 会使用 service account 换取 OAuth2 access token，并调用 Vertex AI Gemini 的 `generateContent` / `streamGenerateContent` 接口。电商素材示例路由 `gemini-3.1-pro-preview` 默认使用 `vertex-gemini` provider。

## Ecommerce Asset Workflow

`solution-ecommerce-worker` 提供电商素材生成工作流：Temporal Workflow 负责编排、重试和并发，`ModelGatewayEcommercePlanAgent` 使用 Embabel `@Agent`、`@Action`、`@AchievesGoal` 注解表达“加载提示词、调用 Gemini、解析计划、校验/修复计划”的智能规划步骤。计划完成后，Workflow 会按需生成模特图、并行生成 10 张展示图，最后使用其中一张展示图作为首帧生成视频。当前可灵调用由 `MockKlingClient` 表达简洁请求/响应边界，后续替换为真实可灵 HTTP Client 即可。

当前 Activity 会显式调用 Embabel Agent 的 action 方法，保证 Temporal Workflow 内没有 LLM、HTTP、文件或环境变量读取等非确定性逻辑。后续如果要启用 Embabel runtime 的自动 GOAP 规划执行，可在 `EcommercePlanAgent` 这一层替换调用方式，不需要改 Temporal Workflow。

本地一次性运行需要先启动 Temporal local service 和 `model-gateway-service`：

```bash
mvn -pl solution-ecommerce-worker -am compile

mvn -pl solution-ecommerce-worker exec:java \
  -Dexec.mainClass=com.example.solution.ecommerce.worker.EcommerceAssetWorkflowRunner \
  -Dexec.args="--product-info 项链 --product-images /path/p1.png,/path/p2.png --model-images /path/m1.png --output-file /tmp/ecommerce-result.json"
```

如果只想启动长驻 Worker：

```bash
mvn -pl solution-ecommerce-worker exec:java \
  -Dexec.mainClass=com.example.solution.ecommerce.worker.EcommerceAssetWorkerApplication
```

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

Gemini 图像生成/编辑使用 `gemini-image` 路由，默认模型为 `gemini-2.5-flash-image`。生成图片会返回在统一响应的 `mediaOutputs` 字段中：

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-tenant",
    "projectId": "video-project",
    "scenario": "IMAGE_GENERATION",
    "capability": "IMAGE_GENERATION",
    "modelHint": "gemini-image",
    "messages": [
      {"role": "USER", "content": "生成一张 16:9 电影感画面：未来城市黄昏，主角骑着悬浮摩托穿过霓虹街道。"}
    ],
    "parameters": {
      "responseModalities": ["TEXT", "IMAGE"]
    },
    "metadata": {
      "promptKey": "image-generation.system",
      "promptVersion": "v1",
      "promptVariables": {
        "aspectRatio": "16:9",
        "visualStyle": "cinematic",
        "language": "zh-CN"
      }
    }
  }'
```

响应中的图片结构示例：

```json
{
  "content": "已生成图片。",
  "mediaOutputs": [
    {
      "type": "image",
      "mimeType": "image/png",
      "base64": "...",
      "url": null,
      "metadata": {
        "source": "gemini-inline-data"
      }
    }
  ]
}
```

## 发布 Client Starter 到 Nexus

`model-gateway-client-starter` 依赖 `model-gateway-api` 和 `model-gateway-core`。如果其它服务要引用 starter，需要把 starter 及其依赖模块一起发布到 Nexus。

先在 Nexus 中创建 Maven 仓库：

- `maven-releases`: hosted release repository
- `maven-snapshots`: hosted snapshot repository
- `maven-public`: group repository，包含 `maven-releases`、`maven-snapshots` 和 `maven-central`

然后配置 Nexus 访问地址和账号：

```bash
export NEXUS_BASE_URL=http://k8s-kong-kongalb-d47fba56e3-58647003.us-east-1.elb.amazonaws.com/nexus
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=your-nexus-password

export NEXUS_MAVEN_RELEASES_URL="${NEXUS_BASE_URL}/repository/maven-releases/"
export NEXUS_MAVEN_SNAPSHOTS_URL="${NEXUS_BASE_URL}/repository/maven-snapshots/"
export NEXUS_MAVEN_PUBLIC_URL="${NEXUS_BASE_URL}/repository/maven-public/"
```

发布 starter 及其依赖模块：

```bash
mvn -s .mvn/settings-nexus.xml \
  -Dnexus.release.repository.url="${NEXUS_MAVEN_RELEASES_URL}" \
  -Dnexus.snapshot.repository.url="${NEXUS_MAVEN_SNAPSHOTS_URL}" \
  -pl model-gateway-client-starter \
  -am \
  deploy
```

其它服务消费时，在 Maven `settings.xml` 中配置同一个 `nexus-public` repository/profile，然后添加依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>model-gateway-client-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
