<template>
  <view class="page-edit-profile">
    <NavBar title="编辑资料" />

    <!-- 沉浸式头像编辑区 -->
    <view class="avatar-hero animate-fade-up">
      <button class="avatar-edit-btn" :class="{ 'is-uploading': uploading }" open-type="chooseAvatar" @chooseavatar="onChooseAvatar" hover-class="avatar-hover">
        <view class="avatar-wrapper">
          <image v-if="userInfo.avatarUrl" :src="$fileUrl(userInfo.avatarUrl)" class="avatar-img" mode="aspectFill" />
          <view v-else class="avatar-empty">
            <text class="avatar-letter">{{ userInfo.nickName ? userInfo.nickName[0] : '?' }}</text>
          </view>
          <!-- 照相机遮罩 -->
          <view class="avatar-mask">
            <text class="camera-icon">📷</text>
          </view>
        </view>
        <text class="avatar-hint">点击更换头像</text>
      </button>
    </view>

    <!-- 卡片化表单 -->
    <view class="saas-card form-container animate-fade-up" style="animation-delay: 0.1s;">
      <view class="form-header">
        <text class="form-title">基础信息</text>
      </view>
      
      <!-- 昵称输入 -->
      <view class="input-group">
        <text class="input-label">微信昵称</text>
        <view class="saas-input" :class="{ 'saas-input--focus': isFocus }">
          <input
            class="input-control"
            type="nickname"
            v-model="nickName"
            placeholder="请输入或获取微信昵称"
            placeholder-class="input-placeholder"
            @focus="isFocus = true"
            @blur="handleNickNameBlur"
          />
        </view>
      </view>

      <!-- 角色与企业信息 (只读凭证态) -->
      <view class="info-group">
        <view class="info-row">
          <text class="info-label">系统身份</text>
          <view class="info-badge bg-blue"><text class="info-value text-blue">{{ roleText }}</text></view>
        </view>
        <view class="info-divider"></view>
        <view class="info-row" v-if="enterpriseName">
          <text class="info-label">所属企业</text>
          <text class="info-value-text">{{ enterpriseName }}</text>
        </view>
        <view class="info-divider" v-if="enterpriseName"></view>
        <view class="info-row">
          <text class="info-label">系统 ID</text>
          <text class="info-value-text num-font">{{ userInfo.id }}</text>
        </view>
      </view>
    </view>

    <view class="safe-bottom-space"></view>

    <!-- 全局悬浮操作舱 -->
    <view class="bottom-action-bar animate-fade-up" style="animation-delay: 0.2s;">
      <button class="action-btn action-btn--primary" hover-class="action-btn--active" @tap="handleSave" :disabled="saving">
        <text v-if="saving" class="btn-icon animate-spin">⏳</text>
        <text class="btn-text">{{ saving ? '保存中...' : '保存更改' }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import { useUserStore } from '@/store/user'
import { updateNickName, uploadAvatar } from '@/api/auth'

export default {
  components: { NavBar },
  data() {
    return {
      store: null,
      nickName: '',
      saving: false,
      isFocus: false,
      uploading: false
    }
  },
  computed: {
    userInfo() {
      return this.store ? this.store.userInfo : {}
    },
    roleText() {
      if (!this.store || !this.store.isLoggedIn) return ''
      if (this.userInfo.role === 'SELLER') return '商家（店主）'
      const map = { ADMIN: '管理员', SALES: '销售员', WAREHOUSE: '仓管员' }
      return map[this.userInfo.memberRole] || '团队成员'
    },
    enterpriseName() {
      return this.store ? this.store.userInfo.enterpriseName : ''
    }
  },
  onLoad() {
    this.store = useUserStore()
    this.nickName = this.store.userInfo.nickName || ''
  },
  methods: {
    handleNickNameBlur(e) {
      if (e.detail.value) {
        this.nickName = e.detail.value
      }
      this.isFocus = false
    },
    async onChooseAvatar(e) {
      const tempUrl = e.detail.avatarUrl
      if (!tempUrl) return
      try {
        this.uploading = true
        uni.showLoading({ title: '上传中...', mask: true })
        const res = await uploadAvatar(tempUrl)
        this.store.userInfo.avatarUrl = res.avatarUrl
        uni.setStorageSync('userInfo', JSON.stringify(this.store.userInfo))
        uni.hideLoading()
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (err) {
        uni.hideLoading()
        uni.showToast({ title: '头像上传失败', icon: 'none' })
      } finally {
        this.uploading = false
      }
    },
    async handleSave() {
      const name = this.nickName.trim()
      if (!name) {
        uni.showToast({ title: '昵称不能为空', icon: 'none' })
        return
      }
      if (name === this.store.userInfo.nickName) {
        uni.navigateBack()
        return
      }
      this.saving = true
      try {
        await updateNickName(name)
        this.store.userInfo.nickName = name
        uni.setStorageSync('userInfo', JSON.stringify(this.store.userInfo))
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (err) {
        uni.showToast({ title: err.message || '保存失败', icon: 'none' })
      } finally {
        this.saving = false
      }
    }
  }
}
</script>


<style lang="scss" scoped>
.page-edit-profile {
  min-height: 100vh;
  background: var(--bg-page);
  padding: 24rpx;
  padding-bottom: 200rpx; /* 留出底部操作栏空间 */
}

/* =======================================
   1. 沉浸式头像编辑区
   ======================================= */
.avatar-hero {
  display: flex; justify-content: center; align-items: center;
  padding: 60rpx 0;
}
.avatar-edit-btn {
  background: transparent; border: none; padding: 0; margin: 0;
  display: flex; flex-direction: column; align-items: center;
  &::after { border: none; }
}
.avatar-hover .avatar-wrapper { transform: scale(0.95); box-shadow: 0 0 0 8rpx rgba(41, 121, 255, 0.2); }
.is-uploading .avatar-wrapper { animation: pulse-ring 1.5s infinite; }
@keyframes pulse-ring { 0% { box-shadow: 0 0 0 0 rgba(41, 121, 255, 0.6); } 70% { box-shadow: 0 0 0 24rpx rgba(41, 121, 255, 0); } 100% { box-shadow: 0 0 0 0 rgba(41, 121, 255, 0); } }

.avatar-wrapper {
  position: relative; width: 220rpx; height: 220rpx; border-radius: 50%;
  box-shadow: 0 16rpx 40rpx rgba(0,0,0,0.1); overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 1.25, 0.2, 1);
  margin-bottom: 24rpx; background: var(--bg-card);
}
.avatar-img, .avatar-empty { width: 100%; height: 100%; }
.avatar-empty { display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #113285 0%, #2979ff 100%); }
.avatar-letter { font-size: 80rpx; font-weight: 700; color: #fff; }
.avatar-mask {
  position: absolute; left: 0; right: 0; bottom: 0; height: 60rpx;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
}
.avatar-wrapper:hover .avatar-mask, .avatar-hover .avatar-mask { height: 100%; background: rgba(0,0,0,0.5); }
.camera-icon { font-size: 32rpx; opacity: 0.9; }
.avatar-hint { font-size: 24rpx; color: var(--text-secondary); }

/* =======================================
   2. 卡片化表单
   ======================================= */
.form-container { padding: 32rpx; margin-bottom: 40rpx; }
.form-header { margin-bottom: 32rpx; }
.form-title { font-size: 32rpx; font-weight: 700; color: var(--text-primary); }

.input-group { margin-bottom: 48rpx; }
.input-label { display: block; font-size: 26rpx; font-weight: 600; color: var(--text-secondary); margin-bottom: 16rpx; }

.saas-input {
  width: 100%; height: 96rpx; background: #f8f9fc; border-radius: 20rpx;
  display: flex; align-items: center; padding: 0 24rpx;
  border: 2rpx solid transparent; transition: all 0.3s cubic-bezier(0.25, 1.25, 0.2, 1);
}
.saas-input--focus { background: #ffffff; border-color: var(--brand-primary); box-shadow: 0 0 0 6rpx rgba(41, 121, 255, 0.15), inset 0 4rpx 12rpx rgba(0,0,0,0.02); }
.input-control { width: 100%; height: 100%; font-size: 30rpx; font-weight: 600; color: var(--text-primary); }
.input-placeholder { color: var(--text-tertiary); font-size: 28rpx; font-weight: 400; }

/* 只读凭证卡片区 */
.info-group { background: #f8f9fc; border-radius: var(--radius-lg); padding: 12rpx 32rpx; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 0; }
.info-label { font-size: 26rpx; color: var(--text-secondary); }
.info-value-text { font-size: 28rpx; font-weight: 600; color: var(--text-primary); }
.info-badge { padding: 4rpx 16rpx; border-radius: 40rpx; }
.bg-blue { background: rgba(41, 121, 255, 0.1); }
.text-blue { font-size: 24rpx; font-weight: 700; color: var(--brand-primary); }
.info-divider { height: 1rpx; background: rgba(0,0,0,0.04); }

/* =======================================
   3. 底部悬浮操作舱
   ======================================= */
.bottom-action-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 32rpx 40rpx calc(32rpx + env(safe-area-inset-bottom));
  background: rgba(255,255,255,0.8); backdrop-filter: blur(24px);
  border-top: 1rpx solid rgba(0,0,0,0.05);
  box-shadow: 0 -8rpx 32rpx rgba(0,0,0,0.03);
  z-index: 100;
}
.action-btn {
  height: 96rpx; line-height: 96rpx; border-radius: 48rpx;
  background: linear-gradient(135deg, #1c5ff8 0%, #2979ff 100%);
  color: #fff; font-size: 32rpx; font-weight: 700;
  box-shadow: 0 16rpx 32rpx rgba(41, 121, 255, 0.3);
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.2);
  display: flex; justify-content: center; align-items: center; gap: 12rpx;
  &::after { border: none; }
}
.action-btn--active { transform: scale(0.96); box-shadow: 0 8rpx 16rpx rgba(41, 121, 255, 0.2); }

</style>
