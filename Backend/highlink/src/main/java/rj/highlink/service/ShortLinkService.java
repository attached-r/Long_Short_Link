package rj.highlink.service;

import rj.highlink.entity.dto.CreateShortLinkDTO;
import rj.highlink.entity.vo.ShortLinkVO;
/*
 * 短链接服务接口
 */
public interface ShortLinkService {
    /**
     * 一：创建短链接
     *
     * @param dto 创建短链接的参数
     * @return 短链接
     */
    String create(CreateShortLinkDTO dto);

    /**
     * 二：获取短链接信息
     *
     * @param shortCode 短码
     * @return 短链接信息
     */
    ShortLinkVO getInfo(String shortCode);

    /**
     * 三：禁用短链接
     *
     * @param shortCode 短码
     * @return 是否禁用成功
     */
    boolean disable(String shortCode);
}
