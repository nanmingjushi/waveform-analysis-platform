import os
import json
import pandas as pd
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("waveform-analyzer")

def safe_float(val):
    """安全转换为浮点数，无法转换则返回 None"""
    try:
        if pd.isna(val) or str(val).strip() == "":
            return None
        return float(val)
    except:
        return None

def fmt_num(val):
    """格式化为两位小数字符串，None 返回空字符串"""
    if val is None:
        return ""
    try:
        return f"{val:.2f}"
    except:
        return str(val)

def find_row_by_keyword(df, keyword, col=0):
    """在 DataFrame 的指定列中搜索包含 keyword 的行索引（忽略前后空格）"""
    for idx, row in df.iterrows():
        cell = str(row[col]).strip()
        if keyword in cell:
            return idx
    return None

@mcp.tool()
def analyze_power_quality_excel(file_path: str) -> str:
    if not os.path.exists(file_path):
        return json.dumps({"status": "error", "message": f"文件不存在: {file_path}"})

    try:
        with pd.ExcelFile(file_path, engine='xlrd') as xls:
            df_vol = pd.read_excel(xls, '电压谐波', header=None)
            df_cur = pd.read_excel(xls, '电流谐波', header=None)
            df_pwr = pd.read_excel(xls, '功率', header=None)

            # ---- 监测位置 ----
            station = "未知超充站"
            if len(df_vol) > 1:
                cell = str(df_vol.iloc[1, 0])
                if "监测位置：" in cell:
                    station = cell.replace("监测位置：", "").strip()

            # ========== 电压谐波数据 ==========
            harmonic_voltage = []

            # 1. 基波电压（搜索关键词）
            base_vol_idx = find_row_by_keyword(df_vol, "基波电压(V)")
            if base_vol_idx is None:
                raise ValueError("未找到 '基波电压(V)' 行")
            row_base = df_vol.iloc[base_vol_idx]

            # 提取各相平均值（索引3,8,13）和95%值（索引5,10,15），结论（索引6,11,16），限值（索引17）
            hv_base = {
                "paramName": "基波电压(kV)",
                "abAvg": fmt_num(safe_float(row_base[3]) / 1000 if safe_float(row_base[3]) is not None else None),
                "ab95":  fmt_num(safe_float(row_base[5]) / 1000 if safe_float(row_base[5]) is not None else None),
                "bcAvg": fmt_num(safe_float(row_base[8]) / 1000 if safe_float(row_base[8]) is not None else None),
                "bc95":  fmt_num(safe_float(row_base[10]) / 1000 if safe_float(row_base[10]) is not None else None),
                "caAvg": fmt_num(safe_float(row_base[13]) / 1000 if safe_float(row_base[13]) is not None else None),
                "ca95":  fmt_num(safe_float(row_base[15]) / 1000 if safe_float(row_base[15]) is not None else None),
                "conclusion": "",  # 基波无结论
                "limit": fmt_num(safe_float(row_base[17]))  # 限值
            }
            harmonic_voltage.append(hv_base)

            # 2. 2~25次谐波电压（搜索“2至50次谐波含有率(%)”行，取下一行开始24行）
            merge_idx = find_row_by_keyword(df_vol, "2至50次谐波含有率(%)")
            if merge_idx is None:
                raise ValueError("未找到 '2至50次谐波含有率(%)' 行")
            # 谐波数据
            start_idx = merge_idx

            for idx in range(merge_idx, len(df_vol)):
                if str(df_vol.iloc[idx, 1]).strip() == "2":
                    start_idx = idx
                    break
            harmonic_rows = df_vol.iloc[start_idx : start_idx + 24]

            for idx, row in harmonic_rows.iterrows():
                # 次数在B列（索引1）
                order_val = row[1]
                try:
                    order = int(float(order_val)) if order_val is not None else None
                except:
                    order = None
                if order is None or order < 2 or order > 25:
                    continue
                harmonic_voltage.append({
                    "paramName": f"{order}次谐波电压(%)",
                    "abAvg": fmt_num(safe_float(row[3])),
                    "ab95":  fmt_num(safe_float(row[5])),
                    "bcAvg": fmt_num(safe_float(row[8])),
                    "bc95":  fmt_num(safe_float(row[10])),
                    "caAvg": fmt_num(safe_float(row[13])),
                    "ca95":  fmt_num(safe_float(row[15])),
                    "conclusion": str(row[6]).strip() if not pd.isna(row[6]) else "",
                    "limit": fmt_num(safe_float(row[17]))
                })

            # 3. 电压总畸变率
            thd_idx = find_row_by_keyword(df_vol, "电压总畸变率(%)")
            if thd_idx is not None:
                row = df_vol.iloc[thd_idx]
                harmonic_voltage.append({
                    "paramName": "电压总畸变率THDu(%)",
                    "abAvg": fmt_num(safe_float(row[3])),
                    "ab95":  fmt_num(safe_float(row[5])),
                    "bcAvg": fmt_num(safe_float(row[8])),
                    "bc95":  fmt_num(safe_float(row[10])),
                    "caAvg": fmt_num(safe_float(row[13])),
                    "ca95":  fmt_num(safe_float(row[15])),
                    "conclusion": str(row[6]).strip() if not pd.isna(row[6]) else "",
                    "limit": fmt_num(safe_float(row[17]))
                })

            # 4. 长时间闪变（用于混合指标）
            plt_idx = find_row_by_keyword(df_vol, "长时间闪变")
            plt_data = None
            if plt_idx is not None:
                row = df_vol.iloc[plt_idx]
                plt_data = {
                    "ab_avg": fmt_num(safe_float(row[3])),
                    "ab_95":  fmt_num(safe_float(row[5])),
                    "bc_avg": fmt_num(safe_float(row[8])),
                    "bc_95":  fmt_num(safe_float(row[10])),
                    "ca_avg": fmt_num(safe_float(row[13])),
                    "ca_95":  fmt_num(safe_float(row[15])),
                    "conclusion": str(row[6]).strip() if not pd.isna(row[6]) else "",
                    "limit": fmt_num(safe_float(row[17]))
                }

            # 5. 过电压/欠电压（用于电压偏差）
            over_idx = find_row_by_keyword(df_vol, "过电压(%)")
            under_idx = find_row_by_keyword(df_vol, "欠电压(%)")
            over_row = df_vol.iloc[over_idx] if over_idx is not None else None
            under_row = df_vol.iloc[under_idx] if under_idx is not None else None

            voltage_deviation = []
            for phase, col_off in [("A", 2), ("B", 7), ("C", 12)]:
                max_val = safe_float(over_row[col_off]) if over_row is not None else 0.0
                min_val = safe_float(under_row[col_off]) if under_row is not None else 0.0
                conclusion = ""
                if over_row is not None:
                    conclusion = str(over_row[col_off+3]).strip() if not pd.isna(over_row[col_off+3]) else "合格"
                voltage_deviation.append({
                    "paramName": f"{phase}相电压偏差(%)",
                    "maxValue": fmt_num(max_val) if max_val is not None else "0.00",
                    "minValue": fmt_num(min_val) if min_val is not None else "0.00",
                    "conclusion": conclusion
                })

            # ========== 电流谐波数据 ==========
            harmonic_current = []

            # 1. 基波电流（搜索关键词）
            base_cur_idx = find_row_by_keyword(df_cur, "基波电流(A)")
            if base_cur_idx is None:
                raise ValueError("未找到 '基波电流(A)' 行")
            cur_base = df_cur.iloc[base_cur_idx]

            hc_base = {
                "paramName": "基波电流(A)",
                "aAvg": fmt_num(safe_float(cur_base[3])),
                "a95":  fmt_num(safe_float(cur_base[5])),
                "bAvg": fmt_num(safe_float(cur_base[8])),
                "b95":  fmt_num(safe_float(cur_base[10])),
                "cAvg": fmt_num(safe_float(cur_base[13])),
                "c95":  fmt_num(safe_float(cur_base[15])),
                "conclusion": "",
                "limit": fmt_num(safe_float(cur_base[17]))
            }
            harmonic_current.append(hc_base)

            # 2. 2~25次谐波电流
            merge_cur_idx = find_row_by_keyword(df_cur, "2至50次谐波含有率(A)")
            if merge_cur_idx is None:
                raise ValueError("未找到 '2至50次谐波含有率(A)' 行")
                start_idx = merge_idx

                for idx in range(merge_idx, len(df_vol)):
                    if str(df_vol.iloc[idx, 1]).strip() == "2":
                        start_idx = idx
                        break
            cur_harmonic_rows = df_cur.iloc[merge_cur_idx : merge_cur_idx + 24]

            for idx, row in cur_harmonic_rows.iterrows():
                order_val = row[1]
                try:
                    order = int(float(order_val)) if order_val is not None else None
                except:
                    order = None
                if order is None or order < 2 or order > 25:
                    continue
                harmonic_current.append({
                    "paramName": f"{order}次谐波电流(A)",
                    "aAvg": fmt_num(safe_float(row[3])),
                    "a95":  fmt_num(safe_float(row[5])),
                    "bAvg": fmt_num(safe_float(row[8])),
                    "b95":  fmt_num(safe_float(row[10])),
                    "cAvg": fmt_num(safe_float(row[13])),
                    "c95":  fmt_num(safe_float(row[15])),
                    "conclusion": str(row[6]).strip() if not pd.isna(row[6]) else "",
                    "limit": fmt_num(safe_float(row[17]))
                })

            # ========== 混合指标（功率Sheet） ==========
            mix_table = []

            # 频率
            freq_idx = find_row_by_keyword(df_pwr, "频率(Hz)")
            if freq_idx is not None:
                row = df_pwr.iloc[freq_idx]
                mix_table.append({
                    "indicatorName": "供电频率(Hz)",
                    "phaseAB": f"最大:{row[1]} / 平均:{row[2]} / 最小:{row[3]}",
                    "phaseBC": "",
                    "phaseCA": "",
                    "limitValue": str(row[16]).strip() if not pd.isna(row[16]) else "",
                    "conclusion": str(row[5]).strip() if not pd.isna(row[5]) else ""
                })

            # 负序电压不平衡度
            unbal_idx = find_row_by_keyword(df_pwr, "负序电压不平衡度(%)")
            if unbal_idx is not None:
                row = df_pwr.iloc[unbal_idx]
                mix_table.append({
                    "indicatorName": "负序电压不平衡度Ɛu(%)",
                    "phaseAB": f"最大:{row[1]} / 平均:{row[2]} / 最小:{row[3]} / 95%:{row[4]}",
                    "phaseBC": "",
                    "phaseCA": "",
                    "limitValue": str(row[16]) if not pd.isna(row[16]) else "",
                    "conclusion": str(row[5]).strip() if not pd.isna(row[5]) else ""
                })

            # 长时间闪变（从电压谐波获取）
            if plt_data:
                mix_table.append({
                    "indicatorName": "长时间闪变Plt",
                    "phaseAB": f"平均:{plt_data['ab_avg']} / 95%:{plt_data['ab_95']}",
                    "phaseBC": f"平均:{plt_data['bc_avg']} / 95%:{plt_data['bc_95']}",
                    "phaseCA": f"平均:{plt_data['ca_avg']} / 95%:{plt_data['ca_95']}",
                    "limitValue": plt_data['limit'],
                    "conclusion": plt_data['conclusion']
                })

            # ========== 组装输出 ==========
            output_payload = {
                "status": "success",
                "monitoringLocation": station,
                "baseVoltage": "10kV",
                "harmonicVoltageTable": harmonic_voltage,
                "harmonicCurrentTable": harmonic_current,
                "mixIndicatorTable": mix_table,
                "voltageDeviationTable": voltage_deviation
            }

            return json.dumps(output_payload, ensure_ascii=False)

    except Exception as e:
        import traceback
        traceback.print_exc()
        return json.dumps({"status": "error", "message": f"解析异常: {str(e)}"})

if __name__ == "__main__":
    mcp.run()