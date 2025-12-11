<template>
  <div class="bg-white p-6 rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6">题目列表</h2>
    <el-table :data="problems" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="标题">
        <template #default="scope">
          <a :href="`/problems/${scope.row.id}`" class="text-blue-600 hover:underline font-medium">
            {{ scope.row.title }}
          </a>
        </template>
      </el-table-column>
      <!-- Add more columns as needed, e.g., difficulty, acceptance rate if available in API -->
    </el-table>
    
    <div class="mt-4 flex justify-center">
        <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            @current-change="handlePageChange"
        />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../utils/axios';

interface Problem {
  id: number;
  title: string;
  // Add other fields based on backend entity
}

const problems = ref<Problem[]>([]);
const loading = ref(false);
const total = ref(0);
const pageSize = ref(20); // Default page size
const currentPage = ref(1);

const fetchProblems = async (page: number) => {
  loading.value = true;
  try {
    // API: /api/problems/?pageSize=...&pageNum=...
    const response = await axios.get('/problems/', {
        params: {
            pageSize: pageSize.value,
            pageNum: page
        }
    });
    // Assuming the API returns an array directly based on controller code: public Problem[] getProblems(...)
    // If it returns a Page object, we need to adjust. 
    // Based on `return problemService.getProblems(pageSize, pageNum);` returning `Problem[]`, 
    // we might not get total count for pagination from this specific endpoint easily without a wrapper.
    // For now, we'll just display what we get. If pagination metadata is missing, we might need to adjust backend or assume infinite scroll/simple next/prev.
    // Let's assume for now it returns just the list.
    
    problems.value = response.data;
    
    // Mock total for now since API doesn't seem to return Page wrapper with total elements
    // Or maybe we just hide pagination if we can't know total.
    // For better UX, let's assume a large number or try to fetch count if possible.
    total.value = 100; 
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page: number) => {
    currentPage.value = page;
    fetchProblems(page);
}

onMounted(() => {
  fetchProblems(1);
});
</script>
