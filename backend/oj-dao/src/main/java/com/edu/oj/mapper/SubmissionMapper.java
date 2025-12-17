package com.edu.oj.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.edu.oj.entity.Submission;
import com.edu.oj.entity.Status;

@Mapper
public interface SubmissionMapper {
    @Select("""
            SELECT COUNT(*) 
            FROM submissions
            """)
    int submissionCount();

    @Select("""
            SELECT * 
            FROM submissions 
            WHERE id = #{submissionId}
            """)
    Submission findSubmissionById(Long submissionId);

    @Select("""
            <script>
                SELECT * FROM submissions
                <where>
                    <if test="userId != null">
                        AND user_id = #{userId}
                    </if>
                    <if test="problemId != null">
                        AND problem_id = #{problemId}
                    </if>
                </where>
                ORDER BY id DESC
                LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    Submission[] getSubmissions(Long userId, Long problemId, int limit, int offset);

    @Update("""
            UPDATE submissions 
            SET status = #{status} 
            WHERE id = #{submissionId}
            """)
    void updateSubmissionStatusById(Long submissionId, Status status);

    @Update("""
            UPDATE submissions 
            SET score = #{newScore} 
            WHERE id = #{submissionId}
            """)
    void updateSubmissionScoreById(Long submissionId, Short newScore);

    @Insert("""
            INSERT INTO submissions (submission_time, status, user_id, problem_id, language, score) 
            VALUES (#{submissionTime}, #{status}, #{userId}, #{problemId}, #{language}, #{score})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSubmission(Submission submission);
}
