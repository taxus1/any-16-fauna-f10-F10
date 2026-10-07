package com.somepro.interfaces.rest.sample.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采样送检与检测对外返回对象（VO，用户接口层）—— 不可变 record。样本编号 sampleNo 必带，
 * 跟送检单对得上号。
 */
public record SampleVO(Long id, String sampleNo, Long reportId, String sampleType,
                       LocalDateTime sentAt, String labName, String testItem, String result,
                       LocalDateTime testedAt, LocalDateTime createTime) implements Serializable {
}
