package com.aeterna.friendmall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu信息介绍
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 16:35:25
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 */
	@TableId(type = IdType.INPUT)  // 虽然是主键，但表属性不是自增，需要我们输入
	private Long spuId;
	/**
	 * 商品介绍
	 */
	private String decript;

}
