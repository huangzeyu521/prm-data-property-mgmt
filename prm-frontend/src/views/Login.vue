<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-csg">中国南方电网</div>
        <div class="login-title">数据资产管理平台 · 数据产权管理</div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" size="large" placeholder="用户名" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码"
            @keyup.enter="onLogin">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登 录</el-button>
      </el-form>
      <div class="login-demo">
        演示账号(密码均 <b>Prm@1234</b>):
        <el-button v-for="d in demos" :key="d.u" link type="primary" size="small" @click="fill(d.u)">{{ d.label }}</el-button>
      </div>
      <div class="login-note">生产环境由 4A 统一身份认证接管登录;本页用于本地/演示。</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, saveSession } from '@/api/auth'
import { ROLE_HOME } from '@/lib/roles'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const demos = [
  { u: 'apply', label: '申报人' },
  { u: 'review', label: '审核审批' },
  { u: 'admin', label: '配置管理员' },
  { u: 'viewer', label: '管理层' },
  { u: 'super', label: '超级管理员' },
]
function fill(u) { form.username = u; form.password = 'Prm@1234' }

async function onLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form.username, form.password)
    saveSession(res.token, res.user)
    ElMessage.success('登录成功,欢迎 ' + (res.user.realName || res.user.username))
    const redirect = route.query.redirect
    const home = redirect ? String(redirect) : (ROLE_HOME[res.user.role] || '/dpr/dashboard/overview')
    // 硬跳转(整页加载):让根布局 App.vue 重读登录身份(角色/用户名),菜单与首屏按真实角色生效
    window.location.href = home
  } catch (e) {
    // 错误提示由响应拦截器统一弹出
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a2a66 0%, #1e87f0 100%);
}
.login-card {
  width: 380px;
  background: #fff;
  border-radius: 6px;
  padding: 36px 32px 24px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.login-brand { text-align: center; margin-bottom: 24px; }
.login-csg { color: #1e87f0; font-weight: 700; font-size: 15px; letter-spacing: 1px; }
.login-title { color: #262626; font-size: 18px; font-weight: 600; margin-top: 6px; }
.login-demo { margin-top: 14px; font-size: 12px; color: #8a8a8a; line-height: 1.8; }
.login-note { margin-top: 10px; font-size: 12px; color: #b4b4b4; text-align: center; }
</style>
