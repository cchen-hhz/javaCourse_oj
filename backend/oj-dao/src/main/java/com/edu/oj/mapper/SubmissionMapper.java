package com.edu.oj.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
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
            SELECT * FROM submissions
            WHERE user_id = #{userId}
            """)
    Submission[] findSubmissionsByUserId(Long userId);

    @Select("""
            SELECT * FROM submissions
            WHERE problem_id = #{problemId}
            """)
    Submission[] findSubmissionsByProblemId(Long problemId);

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
    int insertSubmission(Submission submission);
}
