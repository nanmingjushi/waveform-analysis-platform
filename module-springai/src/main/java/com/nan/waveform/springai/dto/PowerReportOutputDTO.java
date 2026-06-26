package com.nan.waveform.springai.dto;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/25
 *
 * 面向国网标准电能质量测试数据 Word 报告的 4大表格结构化 DTO
 */
public class PowerReportOutputDTO {

    // 顶层元数据：方便 Word 报告页眉、封面或正文开头直接引用
    public String monitoringLocation;       // 监测位置 (如: #1超充站 10kV母线)
    public String baseVoltage;              // 基波电压等级 (如: 10kV)

    // 表格 1：谐波电压统计表
    public List<VoltageRow> harmonicVoltageTable;

    // 表格 2：谐波电流统计表
    public List<CurrentRow> harmonicCurrentTable;

    // 表格 3：频率偏差、三相电压不平衡度及长时间闪变统计表
    public List<FlickerAndFrequencyRow> mixIndicatorTable;

    // 表格 4：电压偏差与过电压统计表
    public List<VoltageDeviationRow> voltageDeviationTable;

    // 综合专家评语与整改结论
    public String globalExpertConclusion;

    /**
     * 表格 1 对应行结构：线参数（AB、BC、CA）
     */
    /**
     * 表格 1 对应行结构：线参数（AB、BC、CA 平均值与95%值彻底拆分）
     */
    public static class VoltageRow {
        public String paramName;       // 指标名称
        public String abAvg;           // AB相平均值
        public String ab95;            // AB相95%值
        public String bcAvg;           // BC相平均值
        public String bc95;            // BC相95%值
        public String caAvg;           // CA相平均值
        public String ca95;            // CA相95%值
        public String limit;           // 限值
        public String conclusion;      // 结论 (合格 / 超标)
    }

    /**
     * 表格 2 对应行结构：相参数（A、B、C 平均值与95%值彻底拆分）
     */
    public static class CurrentRow {
        public String paramName;       // 指标名称
        public String aAvg;            // A相平均值
        public String a95;             // A相95%值
        public String bAvg;            // B相平均值
        public String b95;             // B相95%值
        public String cAvg;            // C相平均值
        public String c95;             // C相95%值
        public String limit;           // 限值
        public String conclusion;      // 结论 (合格 / 超标)
    }

    /**
     * 表格 3 对应行结构：混合核心指标（频率、不平衡度、闪变）
     */
    public static class FlickerAndFrequencyRow {
        public String indicatorName;   // 频率 / 负序电压不平衡度 / 长时间闪变
        public String phaseAB;         // AB线或A相数据多指标汇总描述
        public String phaseBC;         // BC线或B相数据多指标汇总描述
        public String phaseCA;         // CA线或C相数据多指标汇总描述
        public String limitValue;      // 标准限值描述
        public String conclusion;      // 评估结论
    }

    /**
     * 表格 4 对应行结构：电压偏差/过电压
     */
    public static class VoltageDeviationRow {
        public String paramName;       // 监测相别 (如: A相电压偏差, B相电压偏差)
        public double maxValue;        // 最大正偏差 (%)
        public double minValue;        // 最大负偏差 (%)
        public String conclusion;      // 结论描述 (合格 / 超标)
    }
}