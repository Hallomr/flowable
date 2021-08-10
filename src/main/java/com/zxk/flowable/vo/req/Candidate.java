package com.zxk.flowable.vo.req;

import lombok.Data;

@Data
public class Candidate {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    private String groupId;
}
