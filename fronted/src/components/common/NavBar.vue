<template>
  <div class="space-x-2 flex items-center">
    <template v-if="userStore.isAuthenticated && userStore.user">
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="el-dropdown-link cursor-pointer text-gray-600 hover:text-blue-600 flex items-center">
          {{ userStore.user.username }}
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item v-if="['ADMIN', 'ROOT'].includes(userStore.user.role)" command="admin-upload">上传题目</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
    <template v-else>
      <a href="/login" class="text-gray-600 hover:text-blue-600">登录</a>
      <a href="/register" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">注册</a>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useUserStore } from '../../stores/user';
import { ArrowDown } from '@element-plus/icons-vue';

const userStore = useUserStore();

onMounted(() => {
  userStore.fetchUser();
});

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout();
  } else if (command === 'profile') {
    if (userStore.user) {
      window.location.href = `/user/${userStore.user.id}`;
    }
  } else if (command === 'admin-upload') {
    window.location.href = '/admin/upload';
  }
};
</script>

<style scoped>
.el-dropdown-link {
  display: flex;
  align-items: center;
}
</style>