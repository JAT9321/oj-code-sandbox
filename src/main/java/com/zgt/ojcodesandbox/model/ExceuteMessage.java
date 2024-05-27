package com.zgt.ojcodesandbox.model;

import lombok.Data;

/**
 * cmd 进程执行信息
 */
@Data
public class ExceuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;


    private Long memory;


}
