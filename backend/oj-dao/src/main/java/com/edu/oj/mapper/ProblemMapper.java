package com.edu.oj.mapper;

import org.apache.ibatis.annotations.Delete;
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
            <script>
                SELECT * FROM problems
                <if test="pageSize != null and pageNum != null">
                    <bind name="offset" value="(pageNum - 1) * pageSize" />
                    LIMIT #{pageSize} OFFSET #{offset}
                </if>
            </script>
            """)
    Problem[] getProblems(Long pageSize, Long pageNum);

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

    @Delete("""
            DELETE FROM problems
            WHERE id = #{problemId}
            """)
    int deleteProblemById(Long problemId);
}
