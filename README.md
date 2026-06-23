## 试验录波快速解析平台 (Waveform Analysis Platform)

#### 项目简介

试验录波文件快速解析平台（Waveform Analysis Platform）是一个基于Spring Boot开发的电力系统波形数据读取与波形图像解析平台，本后端系统对应的前端项目[waveform-analysis-platform-ui](https://github.com/nanmingjushi/waveform-analysis-platform-ui)。系统采用多模块架构，主要用于解析电力系统中的录波文件（COMTRADE格式）、波形图像识别关键参数以及自动生成电能质量测试数据报告，以此来替代传统的人工工作。

#### 核心功能模块

- `module-admin`

  项目的启动入口，负责系统的整体配置、用户管理以及基于JWT的登录认证。

- `module-common`

  基础公共模块，存放全局统一响应体、全局异常处理器以及各类常用工具类。

- `module-comtrade`

  COMTRADE格式录波文件读取解析模块，负责读取和解析符合IEEE标准的COMTRADE录波文件（.cfg和.dat文件），支持将数据解析并导出为标准CSV格式。

- `module-waveform-vision`

  波形图像识别关键参数模块，集成原生OpenCV图像处理库，用于识别和分析录波波形图像中的特征参数（如暂态最大值、稳态值、阶跃响应时间等）。

- `module-power-quality`

  电能质量测试数据自动化报告生成模块，负责读取仪器导出的Excel数据，并根据预设的Word模板自动填充并生成标准的电能质量测试数据报告。

#### 技术选型

本平台后端基于Spring Boot框架进行业务骨架的搭建，数据持久层采用MyBatis框架对MySQL数据库进行基本的台账存取。针对核心的电力波形图像识别需求，系统集成OpenCV图形处理库并利用JavaCPP实现跨平台原生绑定；自动化报表处理部分则组合使用Hutool实现对仪器导出Excel数据的读取，并配合poi-tl与原生Apache POI完成Word模板的生成与写入；用户登录及接口鉴权采用本地JWT算法进行Token的签发与校验，接口调试页面由Knife4j自动生成。

#### 环境要求

本平台后端的开发与运行需要使用Java17语言环境，项目的打包和多模块构建依赖Maven 3.6及以上版本。

#### 示例

comtrade格式录波文件读取解析

![](https://github.com/nanmingjushi/waveform-analysis-platform/blob/master/%E7%A4%BA%E4%BE%8B/comtrade%E6%A0%BC%E5%BC%8F%E5%BD%95%E6%B3%A2%E6%96%87%E4%BB%B6%E8%AF%BB%E5%8F%96%E8%A7%A3%E6%9E%90.png?raw=true)

波形图像识别关键参数

![](https://github.com/nanmingjushi/waveform-analysis-platform/blob/master/%E7%A4%BA%E4%BE%8B/%E6%B3%A2%E5%BD%A2%E5%9B%BE%E5%83%8F%E8%AF%86%E5%88%AB%E5%85%B3%E9%94%AE%E5%8F%82%E6%95%B0.png?raw=true)

电能质量测试数据自动化读取

![](https://github.com/nanmingjushi/waveform-analysis-platform/blob/master/%E7%A4%BA%E4%BE%8B/%E7%94%B5%E8%83%BD%E8%B4%A8%E9%87%8F%E6%B5%8B%E8%AF%95%E6%95%B0%E6%8D%AE%E8%87%AA%E5%8A%A8%E5%8C%96%E8%AF%BB%E5%8F%96.png?raw=true)

电力系统AI专家助手

![](https://github.com/nanmingjushi/waveform-analysis-platform/blob/master/%E7%A4%BA%E4%BE%8B/%E7%94%B5%E5%8A%9B%E7%B3%BB%E7%BB%9FAI%E5%A4%A7%E6%A8%A1%E5%9E%8B%E4%B8%93%E5%AE%B6%E5%8A%A9%E6%89%8B.png?raw=true)



