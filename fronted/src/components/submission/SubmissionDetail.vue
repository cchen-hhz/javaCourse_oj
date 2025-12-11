<template>
  <div class="bg-white p-6 rounded-lg shadow-md" v-if="result">
    <h2 class="text-2xl font-bold mb-6">提交详情 #{{ id }}</h2>
    
    <div class="mb-6 grid grid-cols-2 gap-4">
        <div>
            <span class="text-gray-600">结果:</span>
            <el-tag 
                class="ml-2 text-lg px-4 py-2 h-auto font-bold" 
                :type="getStatusInfo(result.status).type"
                :color="getStatusInfo(result.status).color"
                :style="getStatusInfo(result.status).style"
                effect="dark"
            >
                {{ getStatusInfo(result.status).text }}
            </el-tag>
        </div>
        <!-- Add more details like time, memory usage if available in SubmissionConfig/Result -->
    </div>

    <div v-if="code" class="mb-6">
        <h3 class="text-lg font-bold mb-2">代码</h3>
        <pre class="bg-gray-100 p-4 rounded overflow-x-auto"><code>{{ code }}</code></pre>
    </div>
    
    <div v-if="result.details" class="mb-6">
         <h3 class="text-lg font-bold mb-2">评测详情</h3>
         <!-- Render detailed test case results if available -->
         <pre class="bg-gray-50 p-4 rounded text-sm">{{ JSON.stringify(result.details, null, 2) }}</pre>
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
import { Loading } from '@element-plus/icons-vue';

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
