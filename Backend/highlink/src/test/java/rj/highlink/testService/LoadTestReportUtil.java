package rj.highlink.testService;

import lombok.Builder;
import lombok.Data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 压测报告生成工具类
 */
public class LoadTestReportUtil {

    @Data
    @Builder
    public static class TestMetrics {
        private String testName;
        private int totalRequests;
        private int successCount;
        private int failCount;
        private long totalDuration;
        private long avgResponseTime;
        private double qps;
        private double successRate;
        private int threadCount;
        private String 备注;
    }

    /**
     * 生成文本格式压测报告
     */
    public static void generateTextReport(String filePath, List<TestMetrics> metricsList) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("=".repeat(100));
            writer.println("短链接系统压测报告");
            writer.println("=".repeat(100));
            writer.println("生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();

            for (TestMetrics metrics : metricsList) {
                writer.println("-".repeat(100));
                writer.println("测试场景：" + metrics.getTestName());
                writer.println("-".repeat(100));
                writer.println("  总请求数:      " + metrics.getTotalRequests());
                writer.println("  成功数:        " + metrics.getSuccessCount());
                writer.println("  失败数:        " + metrics.getFailCount());
                writer.println("  成功率:        " + String.format("%.2f%%", metrics.getSuccessRate()));
                writer.println("  线程数:        " + metrics.getThreadCount());
                writer.println("  总耗时:        " + metrics.getTotalDuration() + "ms");
                writer.println("  平均响应时间:  " + metrics.getAvgResponseTime() + "ms");
                writer.println("  QPS:           " + String.format("%.2f", metrics.getQps()));
                if (metrics.get备注() != null && !metrics.get备注().isEmpty()) {
                    writer.println("  备注：" + metrics.get备注 ());
                }
                writer.println();
            }

            writer.println("=".repeat(100));
            writer.println("总结");
            writer.println("=".repeat(100));

            double avgQPS = metricsList.stream()
                    .mapToDouble(TestMetrics::getQps)
                    .average()
                    .orElse(0.0);

            double avgSuccessRate = metricsList.stream()
                    .mapToDouble(TestMetrics::getSuccessRate)
                    .average()
                    .orElse(0.0);

            writer.println("  平均 QPS:        " + String.format("%.2f", avgQPS));
            writer.println("  平均成功率:      " + String.format("%.2f%%", avgSuccessRate));
            writer.println("  测试场景数:      " + metricsList.size());
            writer.println("=".repeat(100));
        }
    }

    /**
     * 生成 HTML 格式压测报告
     */
    public static void generateHtmlReport(String filePath, List<TestMetrics> metricsList) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"zh-CN\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <title>短链接系统压测报告</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 20px; }");
            writer.println("        table { border-collapse: collapse; width: 100%; margin: 20px 0; }");
            writer.println("        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
            writer.println("        th { background-color: #4CAF50; color: white; }");
            writer.println("        tr:nth-child(even) { background-color: #f2f2f2; }");
            writer.println("        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
            writer.println("        .summary { background-color: #f9f9f9; padding: 15px; margin: 20px 0; border-left: 4px solid #4CAF50; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class=\"header\">");
            writer.println("        <h1>短链接系统压测报告</h1>");
            writer.println("        <p>生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>");
            writer.println("    </div>");

            writer.println("    <table>");
            writer.println("        <thead>");
            writer.println("            <tr>");
            writer.println("                <th>测试场景</th>");
            writer.println("                <th>总请求数</th>");
            writer.println("                <th>成功数</th>");
            writer.println("                <th>失败数</th>");
            writer.println("                <th>成功率</th>");
            writer.println("                <th>QPS</th>");
            writer.println("                <th>平均响应时间 (ms)</th>");
            writer.println("            </tr>");
            writer.println("        </thead>");
            writer.println("        <tbody>");

            for (TestMetrics metrics : metricsList) {
                writer.println("            <tr>");
                writer.println("                <td>" + metrics.getTestName() + "</td>");
                writer.println("                <td>" + metrics.getTotalRequests() + "</td>");
                writer.println("                <td>" + metrics.getSuccessCount() + "</td>");
                writer.println("                <td>" + metrics.getFailCount() + "</td>");
                writer.println("                <td>" + String.format("%.2f%%", metrics.getSuccessRate()) + "</td>");
                writer.println("                <td>" + String.format("%.2f", metrics.getQps()) + "</td>");
                writer.println("                <td>" + metrics.getAvgResponseTime() + "</td>");
                writer.println("            </tr>");
            }

            writer.println("        </tbody>");
            writer.println("    </table>");

            double avgQPS = metricsList.stream()
                    .mapToDouble(TestMetrics::getQps)
                    .average()
                    .orElse(0.0);

            double avgSuccessRate = metricsList.stream()
                    .mapToDouble(TestMetrics::getSuccessRate)
                    .average()
                    .orElse(0.0);

            writer.println("    <div class=\"summary\">");
            writer.println("        <h2>总结</h2>");
            writer.println("        <p><strong>平均 QPS:</strong> " + String.format("%.2f", avgQPS) + "</p>");
            writer.println("        <p><strong>平均成功率:</strong> " + String.format("%.2f%%", avgSuccessRate) + "</p>");
            writer.println("        <p><strong>测试场景数:</strong> " + metricsList.size() + "</p>");
            writer.println("    </div>");

            writer.println("</body>");
            writer.println("</html>");
        }
    }
}
