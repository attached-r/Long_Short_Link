package rj.highlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import rj.highlink.entity.po.ShortLinkPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLinkPo> {
}

/**
 * mapper 接口 用于编写sql语句
 * 继承 BaseMapper 后，无需编写任何 SQL，就可以使用 CRUD 的常用方法，如：
 * insert(T entity) - 插入
 * deleteById(Serializable id) - 根据 ID 删除
 * updateById(T entity) - 根据 ID 更新
 * selectById(Serializable id) - 根据 ID 查询
 * selectList(Wrapper<T> queryWrapper) - 条件查询
 */