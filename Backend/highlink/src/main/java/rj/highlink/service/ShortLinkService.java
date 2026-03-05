package rj.highlink.service;

import rj.highlink.entity.dto.CreateShortLinkDTO;

public interface ShortLinkService {
    /*
     * 创建短链接
     *
     * @param dto 创建短链接的参数
     * @return 短链接
     */
    String create(CreateShortLinkDTO dto);
}
