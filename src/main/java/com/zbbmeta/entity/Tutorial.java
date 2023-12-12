package com.zbbmeta.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * @TableName tb_tutorial
 */
@TableName(value ="tb_tutorial")
@Data
public class Tutorial implements Serializable {
    private Long id;

    private String title;

    private String description;

    private Integer published;

    private static final long serialVersionUID = 1L;
}