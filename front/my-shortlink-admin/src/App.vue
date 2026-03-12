<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createShortLink } from '@/api/shortlink'

const longUrl = ref('')
const expireTime = ref('')
const result = ref(null)
const loading = ref(false)

// 校验长链接格式（避免输入非法链接）
const validateLongUrl = (url) => {
  if (!url) return false
  const reg = /^https?:\/\/.+$/i
  return reg.test(url)
}

const handleCreate = async () => {
  // 1. 增强长链接校验
  const url = longUrl.value.trim()
  if (!url) {
    ElMessage.error('请输入长链接')
    return
  }
  if (!validateLongUrl(url)) {
    ElMessage.error('请输入合法的长链接（必须以http/https开头）')
    return
  }

  loading.value = true
  try {
    const payload = {
      longUrl: url,
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
    ElMessage.error(err.response?.data?.message || '请求失败，请检查网络或后端服务')
  } finally {
    loading.value = false
  }
}

// 优化跳转逻辑：支持新窗口/当前窗口跳转，增加异常提示
const openCorrectRedirect = async (openType = '_blank') => {
  if (!result.value?.shortCode) {
    ElMessage.error('没有短码，无法跳转')
    return
  }

  const backendRedirect = `http://localhost:8080/shortlink/redirect/${result.value.shortCode}`
  
  // 方式1：直接打开（浏览器自动处理302重定向，推荐）
  try {
    // 可选：跳转前提示用户
    await ElMessageBox.confirm(
      '即将跳转到目标链接，请注意辨别链接安全性！',
      '跳转确认',
      {
        confirmButtonText: '确认跳转',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 根据openType决定新窗口/当前窗口跳转
    if (openType === '_self') {
      window.location.href = backendRedirect
    } else {
      window.open(backendRedirect, '_blank')
      ElMessage.success('已在新窗口打开链接')
    }
  } catch (err) {
    // 用户取消跳转
    if (err !== 'cancel') {
      ElMessage.error('跳转失败：' + (err.message || '未知错误'))
    }
  }
}

// 复制优化：支持复制正确的重定向链接
const copyRedirectLink = () => {
  if (!result.value?.shortCode) {
    ElMessage.error('没有短码，无法复制')
    return
  }
  const backendRedirect = `http://localhost:8080/shortlink/redirect/${result.value.shortCode}`
  navigator.clipboard.writeText(backendRedirect)
    .then(() => ElMessage.success('已复制重定向链接'))
    .catch(() => ElMessage.error('复制失败，请手动复制'))
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
          :status="longUrl.trim() && !validateLongUrl(longUrl.trim()) ? 'error' : ''"
        />
        <el-form-item-error v-if="longUrl.trim() && !validateLongUrl(longUrl.trim())">
          请输入以http/https开头的合法链接
        </el-form-item-error>
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
      <div style="margin-bottom: 16px;">
        <strong>原始短链接（仅参考）：</strong> 
        <span style="word-break: break-all;">{{ result.fullShortUrl }}</span>
      </div>
      <div style="margin-bottom: 16px;">
        <strong>可访问的重定向链接：</strong> 
        <span style="word-break: break-all; color: #409eff;">
          {{ `http://localhost:8080/shortlink/redirect/${result.shortCode}` }}
        </span>
      </div>
      <div style="margin-bottom: 16px;">
        <el-button type="primary" @click="openCorrectRedirect('_blank')" style="margin-right: 8px;">
          新窗口跳转
        </el-button>
        <el-button type="success" @click="openCorrectRedirect('_self')" style="margin-right: 8px;">
          当前窗口跳转
        </el-button>
        <el-button size="small" @click="copyRedirectLink">
          复制重定向链接
        </el-button>
        <el-button size="small" @click="() => navigator.clipboard.writeText(result.fullShortUrl).then(() => ElMessage.success('已复制原始链接'))">
          复制原始链接
        </el-button>
      </div>
      <p style="color: #909399; font-size: 12px; line-height: 1.5;">
        说明：后端返回的原始短链接域名（short.url）无法直接访问<br>
        请使用「重定向链接」访问，它会调用本地后端接口完成跳转
      </p>
      <div style="margin-top: 16px; border-top: 1px solid #e6e6e6; padding-top: 16px;">
        <p><strong>短码：</strong> {{ result.shortCode }}</p>
        <p><strong>原始长链接：</strong> <span style="word-break: break-all;">{{ result.longUrl }}</span></p>
        <p><strong>创建时间：</strong> {{ result.createTimeStr }}</p>
        <p><strong>过期时间：</strong> {{ result.expireTimeStr || '永久有效' }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
h1 { text-align: center; margin-bottom: 16px; }
.el-form-item-error {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
}
</style>