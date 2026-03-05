package rj.highlink.service;

public interface RedirectionService {
    // 短链接重定向
    public String redirect(String shortCode);
    // 异步记录访问日志
    public void recordVisitAsync(String shortCode, String ip, String userAgent);
}
