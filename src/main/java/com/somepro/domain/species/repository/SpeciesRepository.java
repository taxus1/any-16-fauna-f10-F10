package com.somepro.domain.species.repository;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.species.model.Species;
import reactor.core.publisher.Mono;

/**
 * 物种名录聚合的仓储端口：由领域层定义，基础设施层实现。
 */
public interface SpeciesRepository {

    /** 新建落库；speciesCode 为空时由实现侧按 SP-NNNN 生成，非空时按指定编码落库（撞码转业务异常）。 */
    Mono<Species> create(Species species);

    /** 按 id 更新资料（编码不改）。 */
    Mono<Species> update(Species species);

    Mono<Species> findById(Long id);

    /**
     * 按物种编码查在册物种（del_flag=0）。观测录入照编码验明正身用：
     * 查不到（编码不存在/停用后被删）即拒绝，不允许拿别的编码糊弄。
     */
    Mono<Species> findByCode(String speciesCode);

    /** 条件分页：条件全空时返回整份名册。 */
    Mono<PageResult<Species>> page(int pageNum, int pageSize,
                                   String name, String protectionLevel, String status);
}
