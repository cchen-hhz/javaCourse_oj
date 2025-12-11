import os
import sys
import time
import signal
import resource
import threading
import subprocess

# ==================== 1. 工具类：截断读取 ====================
class FileTool:
    @staticmethod
    def read_truncated(file_path, max_len=100):
        """读取文件前 max_len 个字节，用于日志展示"""
        if not os.path.exists(file_path):
            return "[File Not Found]"
        try:
            # errors='replace' 防止二进制或非 UTF-8 字符导致报错
            with open(file_path, 'r', encoding='utf-8', errors='replace') as f:
                content = f.read(max_len + 10) # 多读一点
                if len(content) > max_len:
                    return content[:max_len] + "..."
                return content
        except Exception as e:
            return f"[Read Error: {str(e)}]"

# ==================== 2. 校验器：Diff -bw 逻辑 ====================
class Checker:
    def check(self, user_out_path, std_out_path):
        """
        对比用户输出和标准答案
        返回: "AC" 或 "WA"
        逻辑: 忽略行末空格、忽略空行、将连续空白视为一个空格 (等同于 diff -bw)
        """
        if not os.path.exists(user_out_path):
            return "WA" # 没有输出文件

        try:
            with open(user_out_path, 'r', errors='replace') as f1, \
                 open(std_out_path, 'r', errors='replace') as f2:
                
                # split() 不带参数时，会自动去除所有空白符(\n, \t, space)并按内容分割
                # 这是实现忽略空白对比的最快方法
                user_tokens = f1.read().split()
                std_tokens = f2.read().split()
                
                return "AC" if user_tokens == std_tokens else "WA"
        except Exception:
            return "WA"

# ==================== 3. 核心沙箱：进程隔离与监控 ====================
class Sandbox:
    def run(self, cmd_list, input_path, output_path, time_limit_sec, memory_limit_mb):
        """
        执行命令，并进行资源限制
        """
        # 真实时间限制 (Wall Clock) 通常给 CPU 时间的 2 倍，防止 sleep 卡死
        real_time_limit = time_limit_sec * 2 + 1
        
        # 准备父子进程共享的状态变量
        status_result = {
            "result": "AC",
            "cpu_time": 0,    # ms
            "memory": 0,      # KB
            "exit_code": 0,
            "signal": 0
        }

        # Fork 子进程
        pid = os.fork()

        if pid == 0:
            # ================= 子进程 (用户程序) =================
            try:
                # 1. 重定向输入输出
                if input_path:
                    fd_in = os.open(input_path, os.O_RDONLY)
                    os.dup2(fd_in, 0)
                
                fd_out = os.open(output_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC)
                os.dup2(fd_out, 1)
                
                # 2. 设置资源限制 (rlimit)
                
                # CPU 时间: 软限制触发 SIGXCPU，硬限制强制 Kill
                # 向上取整，给一点缓冲
                cpu_sec = int(time_limit_sec) + 1
                resource.setrlimit(resource.RLIMIT_CPU, (cpu_sec, cpu_sec + 1))
                
                # 内存限制: 限制虚拟内存 (RLIMIT_AS)
                mem_bytes = int(memory_limit_mb * 1024 * 1024)
                resource.setrlimit(resource.RLIMIT_AS, (mem_bytes, mem_bytes))
                
                # 栈空间: 稍微给大点，防止递归爆栈 (一般 OJ 也就是内存限制)
                resource.setrlimit(resource.RLIMIT_STACK, (mem_bytes, mem_bytes))
                
                # 安全: 禁止生成 Core Dump
                resource.setrlimit(resource.RLIMIT_CORE, (0, 0))

                # 3. 替换当前进程映像并执行
                # cmd_list 例如 ["./main"]
                os.execve(cmd_list[0], cmd_list, os.environ)
                
            except Exception as e:
                # 极少情况，例如找不到文件
                sys.stderr.write(str(e))
                os._exit(1)

        else:
            # ================= 父进程 (监控者) =================
            start_time = time.time()
            monitor_flag = {"finished": False, "killed": False}

            # --- 守护线程: 监控真实时间 (防 sleep) ---
            def monitor_wall_clock():
                time.sleep(real_time_limit)
                if not monitor_flag["finished"]:
                    monitor_flag["killed"] = True
                    try:
                        os.kill(pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
            
            t = threading.Thread(target=monitor_wall_clock, daemon=True)
            t.start()

            try:
                # 阻塞等待子进程结束，并获取资源统计
                _, status, rusage = os.wait4(pid, 0)
                
                monitor_flag["finished"] = True # 告诉监控线程可以休息了
                
                # 统计数据
                status_result["cpu_time"] = int((rusage.ru_utime + rusage.ru_stime) * 1000) # s -> ms
                status_result["memory"] = int(rusage.ru_maxrss / 1024) # KB (Linux下 maxrss 单位是 KB，macOS是B)
                status_result["exit_code"] = os.waitstatus_to_exitcode(status)
                
                # 获取信号
                signal_num = status & 0x7F
                status_result["signal"] = signal_num

                # --- 状态判定逻辑 ---
                if monitor_flag["killed"] or (signal_num == signal.SIGKILL and status_result["cpu_time"] < time_limit_sec * 1000):
                    # 如果是被监控线程杀的，或者是被 kill 且 CPU 时间并不高 -> Wall Time TLE
                    status_result["result"] = "TLE"
                
                elif signal_num == signal.SIGXCPU:
                    status_result["result"] = "TLE"
                
                elif signal_num == signal.SIGSEGV:
                    # 段错误，可能是内存访问越界，也可能是申请内存被拒绝(malloc return NULL)后写入
                    if status_result["memory"] > memory_limit_mb * 1024: 
                        status_result["result"] = "MLE"
                    else:
                        status_result["result"] = "RE"
                
                elif signal_num != 0:
                    status_result["result"] = f"RE (Signal {signal_num})"
                
                elif status_result["cpu_time"] > time_limit_sec * 1000:
                    # 二次检查 CPU 时间
                    status_result["result"] = "TLE"
                    
                else:
                    # 正常退出
                    if status_result["exit_code"] != 0:
                        status_result["result"] = f"RE (Exit {status_result['exit_code']})"
                    else:
                        status_result["result"] = "AC" # 这里的 AC 仅代表程序运行正常，还没对比答案

            except Exception as e:
                status_result["result"] = "SE" # System Error

            return status_result

# ==================== 4. 整合入口 ====================
def judge_one_case(exe_path, input_path, std_out_path, user_out_path, time_limit=1.0, memory_limit=128):
    """
    单点判题主函数
    """
    sandbox = Sandbox()
    checker = Checker()
    
    # 1. 运行沙箱
    run_res = sandbox.run(
        cmd_list=[exe_path], 
        input_path=input_path, 
        output_path=user_out_path, 
        time_limit_sec=time_limit, 
        memory_limit_mb=memory_limit
    )

    final_res = {
        "status": run_res["result"],
        "time_ms": run_res["cpu_time"],
        "memory_kb": run_res["memory"],
        "input_sample": FileTool.read_truncated(input_path),
        "user_out_sample": "",
        "std_out_sample": FileTool.read_truncated(std_out_path)
    }

    # 2. 如果运行状态是 AC (没挂没超时)，则进行答案对比
    if run_res["result"] == "AC":
        check_status = checker.check(user_out_path, std_out_path)
        final_res["status"] = check_status # 变为 AC 或 WA
    
    # 3. 读取用户输出截断 (无论是否 AC 都读取，方便调试)
    final_res["user_out_sample"] = FileTool.read_truncated(user_out_path)

    return final_res

# ==================== 演示 Demo ====================
if __name__ == "__main__":
    import shutil

    # --- 1. 准备测试数据 ---
    work_dir = "judge_demo_temp"
    if os.path.exists(work_dir): shutil.rmtree(work_dir)
    os.makedirs(work_dir)

    print(f"正在准备测试环境: {work_dir} ...")

    # 源代码 (C语言，模拟正常的加法)
    src_path = os.path.join(work_dir, "main.c")
    exe_path = os.path.join(work_dir, "main")
    with open(src_path, "w") as f:
        f.write(r"""
        #include <stdio.h>
        int main() {
            int a, b;
            scanf("%d %d", &a, &b);
            printf("%d\n", a + b);
            return 0;
        }
        """)
    
    # 编译
    subprocess.run(["gcc", src_path, "-o", exe_path])

    # 输入文件
    input_path = os.path.join(work_dir, "1.in")
    with open(input_path, "w") as f:
        f.write("10 20")

    # 标准答案
    std_path = os.path.join(work_dir, "1.std")
    with open(std_path, "w") as f:
        f.write("30\n")

    # 用户输出路径
    user_out_path = os.path.join(work_dir, "1.user")

    # --- 2. 运行判题 ---
    print("开始判题...")
    result = judge_one_case(
        exe_path=exe_path,
        input_path=input_path,
        std_out_path=std_path,
        user_out_path=user_out_path,
        time_limit=1.0,  # 1秒
        memory_limit=128 # 128MB
    )

    # --- 3. 输出结果 ---
    import json
    print("\n=== 判题结果 ===")
    print(json.dumps(result, indent=4, ensure_ascii=False))

    # --- 4. 模拟一个 WA 的情况 ---
    print("\n模拟错误答案 (修改标准答案为 999)...")
    with open(std_path, "w") as f: f.write("999")
    result_wa = judge_one_case(exe_path, input_path, std_path, user_out_path)
    print(f"状态: {result_wa['status']}")
    print(f"用户输出: {result_wa['user_out_sample']}")
    print(f"标准答案: {result_wa['std_out_sample']}")

    # 清理
    # shutil.rmtree(work_dir)