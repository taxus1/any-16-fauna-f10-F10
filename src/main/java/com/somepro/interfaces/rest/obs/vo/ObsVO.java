package com.somepro.interfaces.rest.obs.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 野生动物观测对外返回对象（VO，用户接口层）—— 不可变 record。观测编号 obsNo 必带。
 *
 * protectionLevel 是观测当时的保护级别快照：不随名录后续调整变动，对账照这份。
 */
public record ObsVO(Long id, String obsNo, Long taskId, Long siteId, String speciesCode,
                    String protectionLevel, Integer individualCount, String healthStatus,
                    LocalDateTime observedAt, String recorder,
                    LocalDateTime createTime) implements Serializable {
}
