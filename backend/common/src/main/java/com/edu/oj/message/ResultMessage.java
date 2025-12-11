package com.edu.oj.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultMessage {
    private Long submissionId;
    private Long problemId;
    
    private Long testCaseId; // 编号，id=0 在 message 存储编译信息等，指示评测开始
    private Long numCases; // 总评测用例
    private Long score;
    private Long timeUsed;
    private Long memoryUsed;
    private Long status;

    private String input;
    private String expectedOutput;
    private String userOutput;
    private String message;

    private Boolean correct; // 当前评测是否完整，若为 false 应当呼出 system_error
    private Boolean isOver; // 若为真则评测结束，忽略后续的数据点
}