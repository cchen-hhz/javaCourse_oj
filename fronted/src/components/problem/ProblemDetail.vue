<template>
  <div class="bg-white p-6 rounded-lg shadow-md" v-if="problem">
    <div class="flex justify-between items-center mb-6">
        <h1 class="text-3xl font-bold text-gray-900">{{ problem.id }}. {{ problem.title }}</h1>
        <div class="space-x-2">
             <!-- File download links if available -->
             <!-- Example: /api/problems/{id}/file/{fileName} -->
        </div>
    </div>

    <div v-if="config" class="mb-6 flex space-x-6 text-sm text-gray-600 bg-gray-50 p-3 rounded border border-gray-200">
        <div><span class="font-bold">时间限制:</span> {{ config.timeLimit }} ms</div>
        <div><span class="font-bold">空间限制:</span> {{ config.memoryLimit }} MB</div>
        <div><span class="font-bold">测试点数量:</span> {{ config.numberCount }}</div>
    </div>

    <div class="prose max-w-none mb-8 p-6 border border-gray-300 rounded-lg shadow-sm mx-auto">
        <!-- Assuming description is markdown or html. If it's a file, we might need to fetch it separately. -->
        <!-- Based on controller, we have getProblemFile. The Problem entity might just have metadata. -->
        <!-- Let's assume we need to fetch description.md or similar if not in entity. -->
        <!-- For now, display what's in entity or a placeholder -->
        <div v-if="problem.description" v-html="renderMarkdown(problem.description)"></div>
        <div v-else class="text-gray-500 italic text-center">没有描述</div>
    </div>

    <div class="border-t pt-6">
        <h3 class="text-xl font-bold mb-4">提交代码</h3>
        <div class="mb-4">
            <el-select v-model="language" placeholder="选择语言">
                <el-option label="C++" value="cpp" />
                <el-option label="Python" value="python" />
            </el-select>
        </div>
        
        <el-input
            v-model="code"
            type="textarea"
            :rows="15"
            placeholder="在此输入代码..."
            class="font-mono mb-4"
            @keydown.tab.prevent="handleTab"
        />
        
        <el-button type="primary" @click="submitCode" :loading="submitting">提交</el-button>
    </div>
  </div>
  <div v-else-if="loading" class="text-center py-12">
      <el-icon class="is-loading text-4xl text-blue-600"><Loading /></el-icon>
  </div>
  <div v-else class="text-center py-12 text-red-600">
      题目加载失败
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../utils/axios';
import { ElMessage } from 'element-plus';
import MarkdownIt from 'markdown-it';
import mk from 'markdown-it-katex';
import mark from 'markdown-it-mark';
import { Loading } from '@element-plus/icons-vue';
import 'katex/dist/katex.min.css';

const props = defineProps<{
  id: string;
}>();

interface Problem {
  id: number;
  title: string;
  description?: string; // Optional
}

interface ProblemConfig {
  timeLimit: number;
  memoryLimit: number;
  numberCount: number;
}

const problem = ref<Problem | null>(null);
const config = ref<ProblemConfig | null>(null);
const loading = ref(true);
const code = ref('');
const language = ref('cpp');
const submitting = ref(false);
const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true
}).use(mk).use(mark);

const renderMarkdown = (text: string) => {
    return md.render(text);
}

const handleTab = (e: KeyboardEvent) => {
    const textarea = e.target as HTMLTextAreaElement;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;

    // Insert 4 spaces
    code.value = code.value.substring(0, start) + "    " + code.value.substring(end);

    // Move cursor
    setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 4;
    }, 0);
};

const parseConfig = (yamlStr: string): ProblemConfig => {
    const config: any = {};
    // Handle potential Windows line endings
    const lines = yamlStr.split(/\r?\n/);
    for (const line of lines) {
        const parts = line.split(':');
        if (parts.length >= 2) {
            const key = parts[0].trim();
            const value = parts.slice(1).join(':').trim();
            if (key === 'time_limit') config.timeLimit = parseInt(value);
            if (key === 'memory_limit') config.memoryLimit = parseInt(value);
            if (key === 'number_count') config.numberCount = parseInt(value);
        }
    }
    return config as ProblemConfig;
}

const fetchProblem = async () => {
  loading.value = true;
  try {
    const response = await axios.get(`/problems/${props.id}`);
    problem.value = response.data;

    // Try to fetch description from file
    try {
      const descResponse = await axios.get(`/problems/${props.id}/file/statement.md`, { responseType: 'text' });
      if (problem.value) {
        problem.value.description = descResponse.data;
      }
    } catch (err) {
      console.log('No statement.md found or failed to fetch');
    }

    // Try to fetch config
    try {
      const configResponse = await axios.get(`/problems/${props.id}/file/config.yml`, { responseType: 'text' });
      config.value = parseConfig(configResponse.data);
    } catch (err) {
      console.log('Failed to fetch config.yml');
      // Try config.yaml as fallback
      try {
          const configResponse = await axios.get(`/problems/${props.id}/file/config.yaml`, { responseType: 'text' });
          config.value = parseConfig(configResponse.data);
      } catch (e) {
          console.log('Failed to fetch config.yaml');
      }
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('无法加载题目详情');
  } finally {
    loading.value = false;
  }
};

const submitCode = async () => {
    if (!code.value.trim()) {
        ElMessage.warning('代码不能为空');
        return;
    }
    
    submitting.value = true;
    try {
        const response = await axios.post('/submissions', {
            problemId: Number(props.id),
            language: language.value,
            code: code.value
        });
        
        if (response.status === 200) {
            ElMessage.success('提交成功');
            // Redirect to submission detail or list
            // window.location.href = `/submissions/${response.data}`; // Assuming returns submission ID
            window.location.href = '/submissions';
        }
    } catch (error) {
        console.error(error);
        // Error handled by interceptor mostly, but can add specific handling here
    } finally {
        submitting.value = false;
    }
}

onMounted(() => {
  fetchProblem();
});
</script>
