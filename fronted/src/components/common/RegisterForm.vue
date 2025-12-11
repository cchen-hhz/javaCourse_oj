<template>
  <div class="max-w-md mx-auto bg-white p-8 rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6 text-center">注册</h2>
    <el-form :model="form" label-width="0" @submit.prevent="handleRegister">
      <el-form-item>
        <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-radio-group v-model="form.role">
          <el-radio label="USER">普通用户</el-radio>
          <el-radio label="ADMIN">管理员</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-input 
          v-model="form.description" 
          type="textarea" 
          :rows="3" 
          placeholder="个人简介" 
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleRegister">注册</el-button>
      </el-form-item>
    </el-form>
    <div class="text-center mt-4">
      <a href="/login" class="text-blue-600 hover:underline">已有账号？立即登录</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import axios from '../../utils/axios';
import { User, Lock, Edit } from '@element-plus/icons-vue';

const form = ref({
  username: '',
  description: '',
  password: '',
  confirmPassword: '',
  role: 'USER',
});

const loading = ref(false);

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  loading.value = true;
  try {
    const response = await axios.post('/log/register', {
      username: form.value.username,
      description: form.value.description,
      password: form.value.password,
      role: form.value.role,
    });

    if (response.data && (response.data.user || response.data.message === 'Register successful')) {
        ElMessage.success('注册成功，请登录');
        window.location.href = '/login';
    } else {
         ElMessage.error(response.data?.message || '注册失败');
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};
</script>
