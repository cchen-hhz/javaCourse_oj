package com.edu.oj.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.edu.oj.entity.User;
import com.edu.oj.entity.Role;

/**
 * UserDAO 管理层接口
 */
@Mapper
public interface UserMapper {
    @Select("""
            SELECT COUNT(*) 
            FROM users
            """)
    int userCount(); 

    @Select("""
            SELECT * 
            FROM users 
            WHERE id = #{userId}
            """)
    User findUserById(Long userId);

    @Select("""
            SELECT * 
            FROM users 
            WHERE username = #{username}
            """)
    User findUserByUsername(String username);


    @Insert("""
            INSERT INTO users (username, password, description, role, created_at, enabled) 
            VALUES (#{username}, #{password}, #{description}, #{role}, #{createdAt}, #{enabled})
            """)
    int insertUser(User user);

    
    @Update("""
            UPDATE users 
            SET role = #{role} 
            WHERE id = #{userId}
            """)
    void updateUserRoleById(Long userId, Role role);

    @Update("""
            UPDATE users 
            SET password = #{newPassword} 
            WHERE id = #{userId}
            """)
    void updateUserPasswordById(Long userId, String newPassword);

    @Update("""
            UPDATE users 
            SET description = #{newDescription} 
            WHERE id = #{userId}
            """)
    void updateUserDescriptionById(Long userId, String newDescription);
}
