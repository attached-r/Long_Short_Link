package rj.highlink.service;
/*
 * 访问日志服务接口
 */
public interface VisitLogService {
    /**
     * 记录访问日志
     *
     * @param shortCode 短码
     * @param ip 访问 IP
     * @param userAgent 用户代理
     */
    void saveVisitLog(String shortCode, String ip, String userAgent);
}
