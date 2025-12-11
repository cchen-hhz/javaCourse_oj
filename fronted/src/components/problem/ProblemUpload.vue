<template>
  <el-card class="problem-upload">
    <template #header>
      <div class="card-header">
        <span>上传题目</span>
      </div>
    </template>
    <el-form :model="form" label-width="120px">
      <el-form-item label="题目ID">
        <el-input-number v-model="form.id" :min="1" />
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="题目文件 (ZIP)">
        <el-upload
          class="upload-demo"
          drag
          action=""
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept=".zip"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传 zip 文件
            </div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitUpload" :loading="uploading">上传</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import axios from '../../utils/axios';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadFile } from 'element-plus';

const form = ref({
  id: 1000,
  title: '',
  file: null as File | null,
});

const uploading = ref(false);

const handleFileChange = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    form.value.file = uploadFile.raw;
  }
};

const submitUpload = async () => {
  if (!form.value.file || !form.value.title) {
    ElMessage.warning('请填写完整信息');
    return;
  }

  uploading.value = true;
  const formData = new FormData();
  formData.append('title', form.value.title);
  formData.append('file', form.value.file);

  try {
    await axios.post(`/problems/${form.value.id}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    ElMessage.success('题目上传成功');
  } catch (error) {
    ElMessage.error('上传失败');
    console.error(error);
  } finally {
    uploading.value = false;
  }
};
</script>

<style scoped>
.problem-upload {
  max-width: 600px;
  margin: 20px auto;
}
</style>