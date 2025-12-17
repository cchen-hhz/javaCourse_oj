<template>
  <div class="bg-white p-6 rounded-lg shadow-md" v-if="result">
    <h2 class="text-2xl font-bold mb-6">提交详情 #{{ id }}</h2>
    
    <div class="mb-6 flex items-center gap-4">
        <span class="text-gray-600 font-bold">结果:</span>
        <el-tag 
            class="text-lg px-4 py-2 h-auto font-bold" 
            :type="getStatusInfo(result.status).type"
            :color="getStatusInfo(result.status).color"
            :style="getStatusInfo(result.status).style"
            effect="dark"
        >
            {{ getStatusInfo(result.status).text }}
        </el-tag>
    </div>

    <div v-if="code" class="mb-6">
        <h3 class="text-lg font-bold mb-2">代码</h3>
        <pre class="bg-gray-100 p-4 rounded overflow-x-auto text-sm font-mono"><code>{{ code }}</code></pre>
    </div>
    
    <!-- Compile Error -->
    <div v-if="result.status === -1" class="mb-6">
        <h3 class="text-lg font-bold mb-2 text-yellow-600">编译信息</h3>
        <pre class="bg-yellow-50 p-4 rounded text-sm font-mono border border-yellow-200 whitespace-pre-wrap">{{ result.compileMessage }}</pre>
    </div>

    <!-- System Error -->
    <div v-else-if="result.status === -2" class="mb-6">
        <el-alert
            title="系统错误"
            type="error"
            description="评测系统发生内部错误，请联系管理员。"
            show-icon
            :closable="false"
        />
        <div v-if="result.compileMessage" class="mt-2 text-gray-500 text-sm">
            Debug Info: {{ result.compileMessage }}
        </div>
    </div>

    <!-- Test Cases -->
    <div v-else-if="result.testResult && result.testResult.length > 0" class="mb-6">
         <h3 class="text-lg font-bold mb-2">测试点详情</h3>
         <el-collapse>
            <el-collapse-item 
                v-for="item in filterTestCases(result.testResult)" 
                :key="item.caseId" 
                :name="item.caseId"
            >
                <template #title>
                    <div class="flex items-center w-full mr-4">
                        <span class="font-bold w-24">Case #{{ item.caseId }}</span>
                        <el-tag size="small" :type="getStatusInfo(item.status).type" :color="getStatusInfo(item.status).color" :style="getStatusInfo(item.status).style" effect="dark" class="mr-4">
                            {{ getStatusInfo(item.status).text }}
                        </el-tag>
                        <span class="text-gray-500 text-sm mr-4">
                            <el-icon class="align-middle"><Timer /></el-icon> {{ item.time }}ms
                        </span>
                        <span class="text-gray-500 text-sm">
                            <el-icon class="align-middle"><Cpu /></el-icon> {{ item.memory }}KB
                        </span>
                    </div>
                </template>
                
                <div class="p-2">
                    <div v-if="item.message" class="mb-2">
                        <span class="font-bold text-gray-700">Message:</span> {{ item.message }}
                    </div>
                    
                    <div class="grid grid-cols-1 gap-2">
                        <div>
                            <div class="font-bold text-gray-700 mb-1">Input:</div>
                            <pre class="bg-gray-50 p-2 rounded border text-xs font-mono overflow-x-auto max-h-32">{{ item.input }}</pre>
                        </div>
                        <div>
                            <div class="font-bold text-gray-700 mb-1">Output:</div>
                            <pre class="bg-gray-50 p-2 rounded border text-xs font-mono overflow-x-auto max-h-32">{{ item.userOutput }}</pre>
                        </div>
                        <div>
                            <div class="font-bold text-gray-700 mb-1">Expected:</div>
                            <pre class="bg-gray-50 p-2 rounded border text-xs font-mono overflow-x-auto max-h-32">{{ item.expectedOutput }}</pre>
                        </div>
                    </div>
                </div>
            </el-collapse-item>
         </el-collapse>
    </div>

  </div>
  <div v-else-if="loading" class="text-center py-12">
      <el-icon class="is-loading text-4xl text-blue-600"><Loading /></el-icon>
  </div>
  <div v-else class="text-center py-12 text-red-600">
      无法加载提交详情
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../utils/axios';
import { ElMessage } from 'element-plus';
import { Loading, Timer, Cpu } from '@element-plus/icons-vue';

const props = defineProps<{
  id: string;
}>();

const result = ref<any>(null);
const code = ref('');
const loading = ref(true);

const fetchSubmission = async () => {
  loading.value = true;
  try {
    // Fetch result
    const resResult = await axios.get(`/submissions/${props.id}/result`);
    result.value = resResult.data;

    // Fetch code (might fail if not authorized, handled by try-catch/interceptor)
    try {
        const resCode = await axios.get(`/submissions/${props.id}/code`);
        code.value = resCode.data;
    } catch (e) {
        // Ignore code fetch error (e.g. forbidden), just don't show code
    }

  } catch (error) {
    console.error(error);
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const filterTestCases = (details: any[]) => {
    if (!details) return [];
    return details.filter(d => d.caseId > 0);
}

const getStatusInfo = (status: number) => {
    switch (status) {
        case -2: return { text: 'System Error (SE)', color: '#000000', style: { color: '#FFFFFF', borderColor: '#000000' } };
        case -1: return { text: 'Compile Error (CE)', type: 'warning' };
        case 0: return { text: 'Accepted (AC)', type: 'success' };
        case 1: return { text: 'Wrong Answer (WA)', type: 'danger' };
        case 2: return { text: 'Time Limit Exceeded (TLE)', type: 'danger' };
        case 3: return { text: 'Memory Limit Exceeded (MLE)', color: '#800080', style: { color: '#FFFFFF', borderColor: '#800080' } };
        case 4: return { text: 'Runtime Error (RE)', color: '#800080', style: { color: '#FFFFFF', borderColor: '#800080' } };
        case 7: return { text: 'Waiting', type: 'info' };
        default: return { text: `Unknown (${status})`, type: 'info' };
    }
}

onMounted(() => {
  fetchSubmission();
});
</script>
