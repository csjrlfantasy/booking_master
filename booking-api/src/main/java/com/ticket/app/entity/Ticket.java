package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 票务实体类
 */
@Data
@TableName("ticket")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 票务ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 演出/赛事名称
     */
    private String name;

    /**
     * 副标题
     */
    private String subName;

    /**
     * 票务分类ID
     */
    private Integer categoryId;

    /**
     * 票务分类名称
     */
    private String categoryName;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 海报图
     */
    private String poster;

    /**
     * 图片集
     */
    private String images;

    /**
     * 详情描述
     */
    private String description;

    /**
     * 亮点
     */
    private String highlights;

    /**
     * 注意事项
     */
    private String attentions;

    /**
     * 演出时间类型: 1-指定时间, 2-时间区间
     */
    private Integer showTimeType;

    /**
     * 演出开始日期
     */
    private LocalDate showStartDate;

    /**
     * 演出结束日期
     */
    private LocalDate showEndDate;

    /**
     * 演出开始时间
     */
    private LocalTime showStartTime;

    /**
     * 演出结束时间
     */
    private LocalTime showEndTime;

    /**
     * 演出时长(分钟)
     */
    private Integer duration;

    /**
     * 年龄限制
     */
    private String ageLimit;

    /**
     * 艺人信息
     */
    private String artistInfo;

    /**
     * 最低价格
     */
    private BigDecimal minimumPrice;

    /**
     * 最高价格
     */
    private BigDecimal maximumPrice;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 每人限购数量
     */
    private Integer perMaxBuy;

    /**
     * 预订开始时间
     */
    private LocalDateTime bookStartTime;

    /**
     * 预订结束时间
     */
    private LocalDateTime bookEndTime;

    /**
     * 是否热门
     */
    private Integer isHot;

    /**
     * 是否推荐
     */
    private Integer isRecommend;

    /**
     * 状态: 0-下架, 1-待开售, 2-售票中, 3-已售罄, 4-演出结束
     */
    private Integer status;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 已售数量
     */
    private Integer saleCount;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 标签
     */
    private String tags;

    /**
     * 扩展信息
     */
    private String extInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 状态常量
     */
    public static final Integer STATUS_OFFLINE = 0;
    public static final Integer STATUS_PENDING = 1;
    public static final Integer STATUS_ON_SALE = 2;
    public static final Integer STATUS_SOLD_OUT = 3;
    public static final Integer STATUS_ENDED = 4;
}