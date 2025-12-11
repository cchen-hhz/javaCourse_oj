<template>
  <div class="user-profile">
    <el-card v-if="user">
      <template #header>
        <div class="card-header">
          <span class="username">{{ user.username }}</span>
          <el-tag :type="user.role === 'ADMIN' ? 'danger' : 'info'">{{ user.role }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ user.id }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(user.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="user.enabled ? 'success' : 'danger'">
            {{ user.enabled ? '正常' : '已封禁' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div class="description-section">
        <h3>个人简介</h3>
        <div class="markdown-content" v-html="renderedDescription"></div>
      </div>

      <div v-if="canManage" class="actions">
        <el-button 
          v-if="user.enabled" 
          type="danger" 
          @click="handleBan"
        >
          封禁用户
        </el-button>
        <el-button 
          v-else 
          type="success" 
          @click="handleUnban"
        >
          解封用户
        </el-button>
      </div>
    </el-card>
    <el-empty v-else description="用户不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useUserStore } from '../../stores/user';
import axios from '../../utils/axios';
import { ElMessage } from 'element-plus';
import MarkdownIt from 'markdown-it';

const props = defineProps<{
  userId: string;
}>();

interface User {
  id: number;
  username: string;
  description: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

const user = ref<User | null>(null);
const userStore = useUserStore();
const md = new MarkdownIt();

const renderedDescription = computed(() => {
  return user.value?.description ? md.render(user.value.description) : '<p>暂无简介</p>';
});

const canManage = computed(() => {
  const currentUserRole = userStore.user?.role;
  const targetUserRole = user.value?.role;
  
  if (!currentUserRole || !targetUserRole) return false;
  
  // ROOT can manage everyone except themselves (though logic below handles self-check)
  if (currentUserRole === 'ROOT') {
      return userStore.user?.id !== user.value?.id;
  }

  // ADMIN can manage USER, but not other ADMINs or ROOT
  if (currentUserRole === 'ADMIN') {
      return targetUserRole === 'USER';
  }

  return false;
});

const fetchUser = async () => {
  try {
    const response = await axios.get(`/users/${props.userId}`);
    user.value = response.data;
  } catch (error) {
    console.error('Failed to fetch user', error);
  }
};

const handleBan = async () => {
  try {
    await axios.post(`/users/${props.userId}/ban`);
    ElMessage.success('用户已封禁');
    if (user.value) user.value.enabled = false;
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const handleUnban = async () => {
  try {
    await axios.post(`/users/${props.userId}/unban`);
    ElMessage.success('用户已解封');
    if (user.value) user.value.enabled = true;
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString();
};

onMounted(() => {
  fetchUser();
});
</script>

<style scoped>
.user-profile {
  max-width: 800px;
  margin: 20px auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.username {
  font-size: 1.2em;
  font-weight: bold;
}
.actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.description-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.description-section h3 {
  margin-bottom: 16px;
  font-size: 18px;
  color: #303133;
}

.markdown-content {
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
  min-height: 100px;
}
</style>