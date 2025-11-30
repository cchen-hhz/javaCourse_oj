package com.edu.oj.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.edu.oj.entity.Problem;

@Mapper
public interface ProblemMapper {
    @Select("""
            SELECT COUNT(*) 
            FORM problems
            """)
    int problemCount();

    @Select("""
            SELECT * FROM problems
            WHERE id = #{problemId}
            """)
    Problem findProblemById(Long problemId);

    @Insert("""
            INSERT INTO problems (title)
            VALUES (#{title})
            """)
    int insertProblem(Problem problem);
}
