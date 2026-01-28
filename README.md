# DeerAssistant 🦌

一个基于 **Spring Boot 3 + LangChain4j + pgvector(RAG)** 的鹿科动物对话智能体后端服务，支持：

- ✅ 用户注册/登录（JWT 鉴权）
- ✅ 流式对话（SSE / text-event-stream）
- ✅ 多会话管理（会话列表、历史记录、新建/删除会话）
- ✅ RAG 检索增强（pgvector + metadata 过滤 + chunkIndex 排序）
- ✅ 知识库管理（KB / Document 上传、预览分段、重向量化、删除）
- ✅ 模型配置管理（管理端维护 chat/embedding 模型，支持测试与默认项）
- ✅ 图片识别（Vision 服务）+ OSS 上传（对话中带图片）
- ✅ Knowledge Base 路由（自动选择最合适 KB，且带冷却避免频繁切换）

---

## Tech Stack

- Java 17 / Spring Boot 3.3.3
- MyBatis-Plus（MySQL 业务数据）
- PostgreSQL + pgvector（向量库）
- LangChain4j（OpenAI 兼容接口：Chat / Embedding / Streaming）
- JWT（java-jwt）+ Spring Interceptor
- WebFlux WebClient（调用 Vision 服务）
- Aliyun OSS（对话图片上传 & 签名 URL）
- SSE（SseEmitter）

---

## Project Structure (核心模块)

- `controller/`
    - `AuthController`：注册/登录/管理员登录
    - `ChatController`：SSE 流式聊天、会话、历史、图片聊天、RAG & KB 路由
    - `admin/*`：管理端接口（KB/Doc/Model/User）
    - `RagController`：手动添加文本到向量库
- `service/`
    - 会话、历史、KB 路由、文档拆分、向量写入、模型配置、Vision、OSS
- `config/`
    - 数据源（MySQL/Postgres/pgvector）、LangChain4j 模型、JWT、OSS、WebMvc
- `entity/` + `mapper/`：MyBatis-Plus 实体与 Mapper
- `resources/`
    - `application.yml`：推荐使用环境变量注入敏感信息
    - `application-local.yml`：本地示例（⚠️不要提交真实密钥）
    - `logback-spring.xml`

---

## Quick Start

### 1) Requirements

- JDK 17
- Maven 3.8+
- MySQL 8+
- PostgreSQL 14+ 并安装 pgvector 扩展

### 2) Create Databases (示例)

> 下面仅示例思路；表结构请结合你当前 MySQL/PG 已有表或自行创建。

- MySQL：业务库（users、chat_history、chat_session、knowledge_base、knowledge_document、model_config 等）
- PostgreSQL：向量表（默认 `deer_knowledge`，metadata 里会写 kbId/docId/chunkIndex/titlePath）