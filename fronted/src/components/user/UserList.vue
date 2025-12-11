<template>
  <div class="user-list-container">
    <h2 class="text-2xl font-bold mb-6">用户列表</h2>
    <el-table :data="users" style="width: 100%" v-loading="loading" @row-click="handleRowClick" class="cursor-pointer">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'info'">{{ scope.row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="简介" />
      <el-table-column prop="createdAt" label="注册时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'danger'">
            {{ scope.row.enabled ? '正常' : '封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" v-if="userStore.user && ['ADMIN', 'ROOT'].includes(userStore.user.role)">
        <template #default="scope">
          <div @click.stop>
            <el-button 
              v-if="scope.row.enabled && canManage(scope.row)" 
              type="danger" 
              size="small" 
              @click="handleBan(scope.row)"
            >
              封禁
            </el-button>
            <el-button 
              v-else-if="!scope.row.enabled && canManage(scope.row)" 
              type="success" 
              size="small" 
              @click="handleUnban(scope.row)"
            >
              解封
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../utils/axios';
import { useUserStore } from '../../stores/user';
import { ElMessage, ElMessageBox } from 'element-plus';

interface User {
  id: number;
  username: string;
  role: string;
  description: string;
  createdAt: string;
  enabled: boolean;
}

const users = ref<User[]>([]);
const loading = ref(false);
const userStore = useUserStore();

const fetchUsers = async () => {
  loading.value = true;
  try {
    const response = await axios.get('/users/');
    users.value = response.data;
  } catch (error) {
    console.error('Failed to fetch users', error);
  } finally {
    loading.value = false;
  }
};

const handleRowClick = (row: User) => {
  window.location.href = `/user/${row.id}`;
};

const canManage = (targetUser: User) => {
  const currentUserRole = userStore.user?.role;
  const targetUserRole = targetUser.role;
  
  if (!currentUserRole || !targetUserRole) return false;
  
  if (currentUserRole === 'ROOT') {
      return userStore.user?.id !== targetUser.id;
  }

  if (currentUserRole === 'ADMIN') {
      return targetUserRole === 'USER';
  }

  return false;
};

const handleBan = async (row: User) => {
  try {
    await ElMessageBox.confirm(`确定要封禁用户 ${row.username} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    
    await axios.post(`/users/${row.id}/ban`);
    ElMessage.success('用户已封禁');
    row.enabled = false;
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败');
    }
  }
};

const handleUnban = async (row: User) => {
  try {
    await axios.post(`/users/${row.id}/unban`);
    ElMessage.success('用户已解封');
    row.enabled = true;
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString();
};

onMounted(() => {
  fetchUsers();
  userStore.fetchUser();
});
</script>

<style scoped>
.user-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.cursor-pointer {
  cursor: pointer;
}
</style>