<template>
  <div class="max-w-md mx-auto bg-white p-8 rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6 text-center">登录</h2>
    <el-form :model="form" label-width="0" @submit.prevent="handleLogin">
      <el-form-item>
        <el-input v-model="form.username" placeholder="用户名/邮箱" :prefix-icon="User" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form-item>
    </el-form>
    <div class="text-center mt-4">
      <a href="/register" class="text-blue-600 hover:underline">没有账号？立即注册</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import axios from '../../utils/axios';
import { User, Lock } from '@element-plus/icons-vue';

const form = ref({
  username: '',
  password: '',
});

const loading = ref(false);

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }

  loading.value = true;
  try {
    const response = await axios.post('/log/login', {
      username: form.value.username,
      password: form.value.password,
    });
    
    if (response.data && (response.data.user || response.data.message === 'Login successful')) {
        ElMessage.success('登录成功');
        localStorage.setItem('isLoggedIn', 'true');
        window.location.href = '/';
    } else {
        ElMessage.error(response.data?.message || '登录失败');
    }
  } catch (error: any) {
    console.error(error);
    if (error.response && error.response.data && error.response.data.message) {
        ElMessage.error(error.response.data.message);
    } else {
        ElMessage.error('登录失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
};
</script>
