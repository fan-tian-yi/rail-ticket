package com.tianyi.railticket;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 票价系数拟合分析（分析代码，不在生产路径上）—— PriceCalculator 的 A/B 就是由它得出的
 *
 * 模型：票价 = A × 里程^B；两边取自然对数化为线性问题 ln y = ln A + B·ln x，用最小二乘求解
 * 样本：G547 同一趟车的 10 个 OD 二等座票价（12306 真实数据），里程取项目 distance_cum
 * 口径：固定「同一趟车 + 同一天」做控制变量，消除各车次的浮动折扣差异
 *      —— 留一交叉验证 2.42%，优于混采多车次的 3.12%
 *
 * 重新抓价的接口（浏览器带 cookie 站内同源 fetch）：
 *   车站电报码 /otn/resources/js/framework/station_name.js
 *   余票      /otn/leftTicket/queryZ?leftTicketDTO.train_date=&from_station=&to_station=&purpose_codes=ADULT
 *   停站表    /otn/czxx/queryByTrainNo?train_no=&from_station_telecode=&to_station_telecode=&depart_date=
 *   区间票价  /otn/leftTicket/queryTicketPrice?train_no=&from_station_no=&to_station_no=&seat_types=&train_date=
 *             ⚠️ seat_types 必传，漏了会静默返回空 {"OT":[]}
 *             data.O = 二等座、data.M = 一等座、data.A9 = 商务座（元，形如 "¥576.0"）
 *
 * 调价正确姿势：改 SAMPLES → 跑本测试 → 把打印出的 A、B 替换到 PriceCalculator，不要手改系数
 */
@Disabled("仅在调整计价模型或更换里程口径时手动运行：结果是打印输出，无断言")
class PriceFittingAnalysisTest {

    /** (里程 km, 二等座票价 元)：G547 单趟车的全部可用 OD */
    private static final double[][] SAMPLES = {
            {281.2, 141}, {293.5, 137}, {327.6, 158}, {417.9, 194}, {608.8, 293},
            {621.1, 289}, {711.4, 321}, {902.3, 416}, {1039.0, 463}, {1320.2, 576},
    };

    /** 对照口径：多车次当日最低价 12 个区间（覆盖更广，但混了不同车次的浮动折扣） */
    private static final double[][] ALT_SAMPLES = {
            {114.8, 59}, {303.1, 135}, {293.5, 137}, {327.6, 144}, {417.9, 194}, {608.8, 281},
            {621.1, 289}, {711.4, 321}, {902.3, 416}, {924.2, 427}, {1039.0, 463}, {1320.2, 576},
    };

    /** t 分布双侧 97.5% 分位（95% 置信区间用），下标 = df - 1 */
    private static final double[] T_TABLE = {
            12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228, 2.201, 2.179};

    @Test
    void fit() {
        System.out.println("\n=== 留一交叉验证（选口径的判据：越小越稳）===");
        double[] looMain = loo(SAMPLES);
        double[] looAlt = loo(ALT_SAMPLES);
        System.out.printf("  %-24s LOO 平均 %.2f%%  最大 %.2f%%%n", "单趟车 G547（主）", looMain[0], looMain[1]);
        System.out.printf("  %-24s LOO 平均 %.2f%%  最大 %.2f%%%n", "多车次最低价（对照）", looAlt[0], looAlt[1]);

        report(SAMPLES, "主样本：G547 单趟车（控制变量）");
        report(ALT_SAMPLES, "对照：多车次当日最低价（仅参考）");

        double a = solve(SAMPLES)[0];
        double b = solve(SAMPLES)[1];
        System.out.println("\n=== 单价单调性（拟合曲线必须随里程递减）===");
        boolean decrease = true;
        double prev = Double.MAX_VALUE;
        for (int km = 100; km <= 1400; km += 200) {
            double unit = a * Math.pow(km, b) / km;
            if (unit >= prev) {
                decrease = false;
            }
            System.out.printf("  里程 %5d → 单价 %.4f 元/km%n", km, unit);
            prev = unit;
        }
        System.out.println(decrease ? "  [OK] 单调递减，满足递远递减" : "  [WARN] 未递减，检查 B 是否 >= 1");

        System.out.println("\n=== 复制到 PriceCalculator.java ===");
        System.out.printf("  private static final double A = %.4f;%n", a);
        System.out.printf("  private static final double B = %.4f;%n", b);
    }

    /** 打印拟合系数、置信区间、R² 与逐点残差 */
    private void report(double[][] pts, String label) {
        int n = pts.length;
        double[] r = solve(pts);
        double a = r[0], b = r[1], se = r[2], lnA = r[3], sxx = r[4], xb = r[5], sse = r[6];
        double df = n - 2;

        double yb = 0;
        for (double[] p : pts) {
            yb += Math.log(p[1]);
        }
        yb /= n;
        double sst = 0;
        for (double[] p : pts) {
            sst += Math.pow(Math.log(p[1]) - yb, 2);
        }

        double t = T_TABLE[Math.min((int) df, T_TABLE.length) - 1];
        double seB = Math.sqrt(sse / df / sxx);
        double seLnA = Math.sqrt(sse / df * (1.0 / n + xb * xb / sxx));

        System.out.printf("%n===== %s  n=%d =====", label, n);
        System.out.printf("%n  票价 = %.4f × 里程^%.4f%n", a, b);
        System.out.printf("  A = %.6f   95%%CI [%.4f, %.4f]%n", a, Math.exp(lnA - t * seLnA), Math.exp(lnA + t * seLnA));
        System.out.printf("  B = %.6f   95%%CI [%.4f, %.4f]%n", b, b - t * seB, b + t * seB);
        System.out.printf("  对数空间 R2 = %.6f   残差标准误 s = %.2f%%%n", 1 - sse / sst, se * 100);
        System.out.printf("  %8s%10s%10s%10s%n", "里程", "真实", "预测", "误差");

        double sum = 0, max = 0;
        for (double[] p : pts) {
            double pred = a * Math.pow(p[0], b);
            double err = (pred - p[1]) / p[1] * 100;
            sum += Math.abs(err);
            max = Math.max(max, Math.abs(err));
            System.out.printf("  %8.1f%10.2f%10.2f%+9.2f%%%n", p[0], p[1], pred, err);
        }
        System.out.printf("  平均|误差| %.2f%%   最大 %.2f%%%n", sum / n, max);
    }

    /** 留一交叉验证：每次剔一个点拟合、去预测被剔掉的那个点，返回 {平均, 最大} */
    private double[] loo(double[][] pts) {
        double sum = 0, max = 0;
        for (int i = 0; i < pts.length; i++) {
            double[][] train = new double[pts.length - 1][];
            int k = 0;
            for (int j = 0; j < pts.length; j++) {
                if (j != i) {
                    train[k++] = pts[j];
                }
            }
            double[] r = solve(train);
            double err = Math.abs(r[0] * Math.pow(pts[i][0], r[1]) - pts[i][1]) / pts[i][1] * 100;
            sum += err;
            max = Math.max(max, err);
        }
        return new double[]{sum / pts.length, max};
    }

    /** 对数线性最小二乘，返回 {A, B, 残差标准误, lnA, Sxx, X均值, SSE} */
    private double[] solve(double[][] pts) {
        int n = pts.length;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = Math.log(pts[i][0]);
            y[i] = Math.log(pts[i][1]);
        }

        double xb = 0, yb = 0;
        for (int i = 0; i < n; i++) {
            xb += x[i];
            yb += y[i];
        }
        xb /= n;
        yb /= n;

        double sxx = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            sxx += (x[i] - xb) * (x[i] - xb);
            sxy += (x[i] - xb) * (y[i] - yb);
        }
        double b = sxy / sxx;
        double lnA = yb - b * xb;

        double sse = 0;
        for (int i = 0; i < n; i++) {
            sse += Math.pow(y[i] - (lnA + b * x[i]), 2);
        }
        return new double[]{Math.exp(lnA), b, Math.sqrt(sse / (n - 2)), lnA, sxx, xb, sse};
    }
}
