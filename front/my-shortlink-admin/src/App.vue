<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createShortLink } from '@/api/shortlink'

const longUrl = ref('')
const expireTime = ref('')
const result = ref(null)
const loading = ref(false)

const handleCreate = async () => {
  if (!longUrl.value.trim()) {
    ElMessage.error('请输入长链接')
    return
  }

  loading.value = true
  try {
    const payload = {
      longUrl: longUrl.value.trim(),
    }
    if (expireTime.value) {
      payload.expireTime = expireTime.value
    }

    const res = await createShortLink(payload)
    if (res.data.code === 200) {
      result.value = res.data.data
      ElMessage.success('创建成功！')
      longUrl.value = ''
      expireTime.value = ''
    } else {
      ElMessage.error(res.data.message || '创建失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '请求失败')
  } finally {
    loading.value = false
  }
}

// 点击按钮时，强制跳转到本地后端重定向接口
const openCorrectRedirect = () => {
  if (result.value?.shortCode) {
    const backendRedirect = `http://localhost:8080/shortlink/redirect/${result.value.shortCode}`
    window.open(backendRedirect, '_blank')
    ElMessage.success('已打开后端重定向链接')
  } else {
    ElMessage.error('没有短码，无法跳转')
  }
}
</script>

<template>
  <div style="padding: 40px; max-width: 700px; margin: 0 auto;">
    <h1>短链接生成器</h1>
    <p style="color: #666; margin-bottom: 32px;">输入长链接，快速生成短链</p>

    <el-form label-width="100px">
      <el-form-item label="长链接" required>
        <el-input
          v-model="longUrl"
          placeholder="https://example.com/very/long/url"
          clearable
        />
      </el-form-item>

      <el-form-item label="过期时间">
        <el-date-picker
          v-model="expireTime"
          type="datetime"
          placeholder="可选，默认7天"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :disabled-date="time => time < Date.now()"
          style="width: 100%;"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleCreate" style="width: 200px;">
          生成短链接
        </el-button>
      </el-form-item>
    </el-form>

    <div v-if="result" style="margin-top: 40px; padding: 20px; background: #f5f7fa; border-radius: 8px;">
      <h3>生成结果</h3>
      <p><strong>短链接（后端返回的域名，可能无法直接用）：</strong> 
        {{ result.fullShortUrl }}
      </p>
      <p>
        <el-button type="primary" @click="openCorrectRedirect">
          点击这里跳转（使用本地后端重定向）
        </el-button>
        <el-button size="small" @click="() => navigator.clipboard.writeText(result.fullShortUrl).then(() => ElMessage.success('已复制'))">
          复制原始链接
        </el-button>
      </p>
      <p style="color: #909399; font-size: 12px;">
        说明：后端返回的域名是 short.url，无法访问。<br>
        请使用上面的“点击这里跳转”按钮，它会调用正确的后端接口进行重定向。
      </p>
      <p><strong>短码：</strong> {{ result.shortCode }}</p>
      <p><strong>原始链接：</strong> {{ result.longUrl }}</p>
      <p><strong>创建时间：</strong> {{ result.createTimeStr }}</p>
      <p><strong>过期时间：</strong> {{ result.expireTimeStr || '永久有效' }}</p>
    </div>
  </div>
</template>

<style scoped>
h1 { text-align: center; margin-bottom: 16px; }
</style>