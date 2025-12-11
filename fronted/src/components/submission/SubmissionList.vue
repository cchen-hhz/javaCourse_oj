<template>
  <div class="bg-white p-6 rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6">提交记录</h2>
    <el-table :data="submissions" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="100">
          <template #default="scope">
              <a :href="`/users/${scope.row.userId}`" class="text-blue-600 hover:underline">{{ scope.row.userId }}</a>
          </template>
      </el-table-column>
      <el-table-column prop="problemId" label="题目ID" width="100">
          <template #default="scope">
              <a :href="`/problems/${scope.row.problemId}`" class="text-blue-600 hover:underline">{{ scope.row.problemId }}</a>
          </template>
      </el-table-column>
      <el-table-column prop="status" label="结果">
          <template #default="scope">
              <el-tag :type="getResultType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
      </el-table-column>
      <el-table-column prop="submissionTime" label="提交时间">
        <template #default="scope">
          {{ formatTime(scope.row.submissionTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
          <template #default="scope">
              <a :href="`/submissions/${scope.row.id}`" class="text-blue-600 hover:underline text-sm">详情</a>
          </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../utils/axios';

interface Submission {
  id: number;
  userId: number;
  problemId: number;
  status: string; // e.g., PENDING, JUDGING, DONE
  submissionTime: string;
}

const submissions = ref<Submission[]>([]);
const loading = ref(false);

const fetchSubmissions = async () => {
  loading.value = true;
  try {
    const response = await axios.get('/submissions/');
    submissions.value = response.data;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
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
