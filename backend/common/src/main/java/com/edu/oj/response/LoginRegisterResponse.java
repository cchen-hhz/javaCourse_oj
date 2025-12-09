package com.edu.oj.response;

import com.edu.oj.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRegisterResponse {
    private User user;
    private String message;
}