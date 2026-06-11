# ⚡ 试验录波快速解析平台 (Waveform Analysis Platform)

基于SpringBoot 3 + Vue3的COMTRADE电力录波数据、电能质量数据与波形图像综合分析系统。

## 🏗️ 项目架构与模块划分

本项目采用前后端分离架构，后端基于Maven多模块标准构建。

### 📦 后端工程 (`waveform-analysis-platform`)

| 模块名称 | 职责描述 |
| :--- | :--- |
| **`module-admin`** | **入口模块**：包含主启动类与系统核心配置文件（如 `application.yml`）。负责集成各个业务模块并打包。 |
| **`module-common`** | **公共模块**：提供全局统一基建，如统一响应对象 `Result`、全局异常处理、公用工具类（Hutool）等。 |
| **`module-comtrade`** | **业务模块**：负责 COMTRADE 格式故障录波文件的快速读取、解析与数据抽取。 |
| **`module-power-quality`** | **业务模块**：负责电能质量测试数据（Excel/CSV）的读取、解析与结构化处理。 |
| **`module-waveform-vision`** | **业务模块**：负责波形图像的识别与关键参数提取。 |

### 🎨 前端工程(`waveform-analysis-platform-ui`)
  基于Vue 3 + Vite + Element-Plus + Pinia构建的现代化数据交互与波形可视化分析平台。

## 🛠️ 核心技术栈

- **后端:** JDK 17, Spring Boot 3.2.x, MyBatis-Plus, Knife4j (Swagger 3), EasyExcel, Hutool
- **前端:** Vue 3 (Composition API), Vue Router, Pinia, Element-Plus, ECharts
- **部署:** Nginx 前后端分离代理