package com.zxk.flowable.vo.req;

import lombok.Data;

@Data
public class Leave {
    private String employee;
    private Integer holidays;
    private String description;
}
