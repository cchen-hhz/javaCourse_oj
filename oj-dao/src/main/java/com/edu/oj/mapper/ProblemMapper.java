package com.edu.oj.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.edu.oj.entity.Problem;

@Mapper
public interface ProblemMapper {
    @Select("""
            SELECT COUNT(*) 
            FROM problems
            """)
    int problemCount();

    @Select("""
            SELECT * FROM problems
            """)
    Problem[] getAllProblems();

    @Select("""
            SELECT * FROM problems
            WHERE id = #{problemId}
            """)
    Problem findProblemById(Long problemId);

    @Insert("""
            INSERT INTO problems (id, title)
            VALUES (#{id}, #{title})
            """)
    int insertProblem(Problem problem);
}
