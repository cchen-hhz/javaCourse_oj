<template>
  <div class="bg-white p-6 rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6">提交记录</h2>

    <div class="mb-4 flex gap-4">
      <el-input v-model="filterUserId" placeholder="用户 ID" style="width: 200px" clearable @clear="handleFilter" />
      <el-input v-model="filterProblemId" placeholder="题目 ID" style="width: 200px" clearable @clear="handleFilter" />
      <el-button type="primary" @click="handleFilter">筛选</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="submissions" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80">
          <template #default="scope">
              <a :href="`/submissions/${scope.row.id}`" class="text-blue-600 hover:underline">{{ scope.row.id }}</a>
          </template>
      </el-table-column>
      <el-table-column prop="userName" label="用户" width="150">
          <template #default="scope">
              <a :href="`/user/${scope.row.userId}`" class="text-blue-600 hover:underline">{{ scope.row.userId }}. {{ scope.row.userName || '' }}</a>
          </template>
      </el-table-column>
      <el-table-column prop="problemTitle" label="题目" min-width="200">
          <template #default="scope">
              <a :href="`/problems/${scope.row.problemId}`" class="text-blue-600 hover:underline">{{ scope.row.problemId }}. {{ scope.row.problemTitle || '' }}</a>
          </template>
      </el-table-column>
      <el-table-column prop="status" label="结果" width="150">
          <template #default="scope">
              <el-tag :type="getResultType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
      </el-table-column>
      <el-table-column prop="submissionTime" label="提交时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.submissionTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
          <template #default="scope">
              <a :href="`/submissions/${scope.row.id}`" class="text-blue-600 hover:underline text-sm">详情</a>
          </template>
      </el-table-column>
    </el-table>
    
    <div class="mt-4 flex justify-center gap-4">
      <el-button :disabled="currentPage === 1" @click="changePage(-1)">上一页</el-button>
      <span class="self-center">第 {{ currentPage }} 页</span>
      <el-button :disabled="submissions.length < pageSize" @click="changePage(1)">下一页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../utils/axios';

interface Submission {
  id: number;
  userId: number;
  userName?: string;
  problemId: number;
  problemTitle?: string;
  status: string; // e.g., PENDING, JUDGING, DONE
  submissionTime: string;
}

const submissions = ref<Submission[]>([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(20);
const filterUserId = ref('');
const filterProblemId = ref('');

const fetchSubmissions = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    };
    if (filterUserId.value) params.userId = filterUserId.value;
    if (filterProblemId.value) params.problemId = filterProblemId.value;

    const response = await axios.get('/submissions/', { params });
    submissions.value = response.data;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleFilter = () => {
  currentPage.value = 1;
  fetchSubmissions();
};

const resetFilter = () => {
  filterUserId.value = '';
  filterProblemId.value = '';
  handleFilter();
};

const changePage = (delta: number) => {
  currentPage.value += delta;
  fetchSubmissions();
};

const getResultType = (status: string) => {
    if (status === 'DONE') return 'success';
    if (status === 'PENDING') return 'info';
    if (status === 'JUDGING') return 'warning';
    return 'info';
}

const formatTime = (time: string) => {
  if (!time) return '';
  return new Date(time).toLocaleString();
}

onMounted(() => {
  fetchSubmissions();
});
</script>
