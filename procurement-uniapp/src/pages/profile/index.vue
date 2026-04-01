<template>
  <view class="page-profile">
    <!-- 1. 尊享级 Hero 个人名片 -->
    <view class="hero-header animate-fade-up">
      <view class="hero-main" @tap="handleAvatarTap">
        <view class="hero-avatar">
          <!-- 已登录：用 chooseAvatar 按钮包裹头像，点击可更换 -->
          <button v-if="isLoggedIn" class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
            <image v-if="userInfo.avatarUrl" :src="$fileUrl(userInfo.avatarUrl)" class="avatar-img" mode="aspectFill" />
            <view v-else class="avatar-fallback"><text class="avatar-text">{{ userInfo.nickName ? userInfo.nickName[0] : '?' }}</text></view>
          </button>
          <!-- 未登录：原始占位符 -->
          <template v-else>
            <view class="avatar-fallback"><text class="avatar-text">?</text></view>
          </template>
        </view>
        <view class="hero-info" @tap.stop="isLoggedIn ? null : handleAvatarTap()">
          <text class="hero-name">{{ isLoggedIn ? (userInfo.nickName || '已登录') : '未登录' }}</text>
          <view v-if="isLoggedIn" class="hero-tags">
            <text class="tag-glass">{{ roleText }}</text>
            <view v-if="enterpriseName" class="tag-glass">
              <view class="svg-icon-mini svg-icon-building"></view>
              <text>{{ enterpriseName }}</text>
            </view>
          </view>
          <text v-else class="hero-hint">点击登录微信账号获取完整体验</text>
        </view>
        <view v-if="isLoggedIn" class="hero-edit" @tap.stop="goEditProfile">
          <text class="hero-edit-text">编辑资料</text>
          <text class="hero-edit-arrow">›</text>
        </view>
        <text v-else class="hero-arrow">›</text>
      </view>
    </view>

    <!-- 库存预警通知引导 Banner -->
    <!-- #ifdef MP-WEIXIN -->
    <view v-if="isLoggedIn && hasEnterprise && !notifyEnabled" class="notify-banner animate-fade-up" style="animation-delay: 0.1s;">
      <view class="notify-banner__left">
        <text class="notify-icon">🔔</text>
        <text class="notify-banner__text">开启库存预警通知，第一时间了解库存状况</text>
      </view>
      <button class="notify-banner__btn" hover-class="btn-hover" @tap="handleEnableNotify">开启</button>
    </view>
    <!-- #endif -->

    <view class="menu-container animate-fade-up" style="animation-delay: 0.2s;">
      <!-- 企业信息区 -->
      <view class="saas-card menu-group">
        <view class="menu-cell" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100" v-if="hasEnterprise" @tap="goEnterprise">
          <view class="menu-cell__left">
            <view class="menu-icon-bg bg-blue"><view class="svg-icon svg-icon-building"></view></view>
            <text class="menu-text">我的企业</text>
          </view>
          <view class="menu-arrow-icon svg-icon-chevron"></view>
        </view>
        <view class="menu-cell" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100" v-else @tap="goCreateEnterprise">
          <view class="menu-cell__left">
            <view class="menu-icon-bg bg-blue"><view class="svg-icon svg-icon-building"></view></view>
            <text class="menu-text">创建企业</text>
          </view>
          <view class="menu-arrow-icon svg-icon-chevron"></view>
        </view>
      </view>

      <!-- 系统功能区 -->
      <view class="saas-card menu-group">
        <view class="menu-cell" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100" v-if="canManageTeam" @tap="goTeam">
          <view class="menu-cell__left">
            <view class="menu-icon-bg bg-purple"><view class="svg-icon svg-icon-team"></view></view>
            <text class="menu-text">团队管理</text>
          </view>
          <view class="menu-arrow-icon svg-icon-chevron"></view>
        </view>
        
        <view class="menu-divider" v-if="canManageTeam && hasEnterprise"></view>
        
        <view class="menu-cell" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100" v-if="hasEnterprise" @tap="goPrinter">
          <view class="menu-cell__left">
            <view class="menu-icon-bg bg-orange"><view class="svg-icon svg-icon-printer"></view></view>
            <text class="menu-text">打印机管理</text>
          </view>
          <view class="menu-arrow-icon svg-icon-chevron"></view>
        </view>
        
        <view class="menu-divider" v-if="hasEnterprise && isSeller"></view>
        
        <view class="menu-cell" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100" v-if="isSeller" @tap="goBackup">
          <view class="menu-cell__left">
            <view class="menu-icon-bg bg-green"><view class="svg-icon svg-icon-backup"></view></view>
            <text class="menu-text">数据备份</text>
          </view>
          <view class="menu-arrow-icon svg-icon-chevron"></view>
        </view>
      </view>

      <!-- 退出登录区 -->
      <view class="logout-wrapper animate-fade-up" style="animation-delay: 0.3s;" v-if="isLoggedIn">
        <view class="logout-btn" hover-class="logout-btn--hover" :hover-start-time="0" :hover-stay-time="100" @tap="handleLogout">
          <text class="logout-text">退出登录</text>
        </view>
      </view>

      <view class="safe-bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/user'
import { hasAccess } from '@/utils/permission'
import { uploadAvatar } from '@/api/auth'
import { enableNotify } from '@/api/subscribe'
import { WX_STOCK_WARNING_TEMPLATE_ID } from '@/config/index'

export default {
  data() {
    return {
      store: null
    }
  },
  computed: {
    userInfo() {
      return this.store ? this.store.userInfo : {}
    },
    isLoggedIn() {
      return this.store ? this.store.isLoggedIn : false
    },
    hasEnterprise() {
      return this.store ? this.store.hasEnterprise : false
    },
    enterpriseName() {
      return this.store ? this.store.userInfo.enterpriseName : ''
    },
    roleText() {
      if (!this.isLoggedIn) return ''
      if (this.userInfo.role === 'SELLER' && this.userInfo.enterpriseId) return '商家（店主）'
      if (this.userInfo.enterpriseId) {
        const memberMap = { ADMIN: '管理员', SALES: '销售员', WAREHOUSE: '仓管员' }
        return memberMap[this.userInfo.memberRole] || '团队成员'
      }
      return '新用户'
    },
    canManageTeam() {
      return hasAccess('team')
    },
    isSeller() {
      return this.store ? this.store.isSeller : false
    },
    notifyEnabled() {
      return this.store ? this.store.notifyEnabled : false
    }
  },
  mounted() {
    this.store = useUserStore()
  },
  async onShow() {
    // 双重保险
    if (!this.store) {
      this.store = useUserStore()
    }
    // 每次切到「我的」Tab 时拉取最新用户信息，确保身份/权限显示不滞后
    // （店主可能已在团队管理中变更了该员工的角色）
    if (this.store.isLoggedIn) {
      this.store.fetchProfile().catch(() => {})
    }
  },
  methods: {
    /** 点击头像区域 — 未登录则触发微信登录 */
    async handleAvatarTap() {
      if (!this.store) this.store = useUserStore()
      if (this.isLoggedIn) return
      try {
        // 1. wx.login 获取 code
        const loginRes = await uni.login({ provider: 'weixin' })
        if (!loginRes?.code) {
          uni.showToast({ title: '微信登录失败', icon: 'none' })
          return
        }
        // 2. 尝试获取用户头像昵称（微信基础库 2.27.1+）
        let nickName = ''
        let avatarUrl = ''
        try {
          const profileRes = await uni.getUserProfile({ desc: '用于完善用户资料' })
          if (profileRes?.userInfo) {
            nickName = profileRes.userInfo.nickName || ''
            avatarUrl = profileRes.userInfo.avatarUrl || ''
          }
        } catch (e) {
          // getUserProfile 被用户拒绝或不支持，继续静默登录
        }
        // 3. 调用后端微信登录
        await this.store.wxLogin({
          code: loginRes.code,
          nickName: nickName || undefined,
          avatarUrl: avatarUrl || undefined
        })
        // 4. 拉取最新用户信息（确保头像、昵称同步）
        await this.store.fetchProfile()
        uni.showToast({ title: '登录成功', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '登录失败', icon: 'none' })
      }
    },

    /** 用户选择微信头像后上传 */
    async onChooseAvatar(e) {
      const tempUrl = e.detail.avatarUrl
      if (!tempUrl) return
      try {
        uni.showLoading({ title: '上传中...', mask: true })
        const res = await uploadAvatar(tempUrl)
        // 更新 store 中的头像
        if (!this.store) this.store = useUserStore()
        this.store.userInfo.avatarUrl = res.avatarUrl
        uni.setStorageSync('userInfo', JSON.stringify(this.store.userInfo))
        uni.hideLoading()
        uni.showToast({ title: '头像更新成功', icon: 'success' })
      } catch (err) {
        uni.hideLoading()
        uni.showToast({ title: '头像上传失败', icon: 'none' })
      }
    },

    goEnterprise() {
      uni.navigateTo({ url: '/pages/profile/enterprise' })
    },
    goEditProfile() {
      uni.navigateTo({ url: '/pages/profile/edit-profile' })
    },
    goCreateEnterprise() {
      uni.navigateTo({ url: '/pages/profile/create-enterprise' })
    },
    goTeam() {
      uni.navigateTo({ url: '/pages/profile/team' })
    },
    goPrinter() {
      uni.navigateTo({ url: '/pages/profile/printer' })
    },
    goBackup() {
      uni.navigateTo({ url: '/pages/profile/backup' })
    },
    // #ifdef MP-WEIXIN
    handleEnableNotify() {
      const tmplId = WX_STOCK_WARNING_TEMPLATE_ID
      wx.requestSubscribeMessage({
        tmplIds: [tmplId],
        success: async (res) => {
          if (res[tmplId] === 'accept') {
            try {
              await enableNotify()
              useUserStore().setNotifyEnabled(true)
              uni.showToast({ title: '已开启预警通知', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: '开启失败，请重试', icon: 'none' })
            }
          } else {
            uni.showToast({ title: '您已拒绝通知授权', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '授权失败，请重试', icon: 'none' })
        }
      })
    },
    // #endif
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定退出登录？',
        success: (res) => {
          if (res.confirm) {
            if (!this.store) this.store = useUserStore()
            this.store.logout()
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-profile {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 60rpx;
}

/* =======================================
   1. Hero Header (名片舱)
   ======================================= */
.hero-header {
  position: relative;
  background: #ffffff;
  padding: 100rpx 40rpx 60rpx;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.03);
  border-bottom-left-radius: 48rpx;
  border-bottom-right-radius: 48rpx;
  margin-bottom: 32rpx;
}

.hero-main {
  position: relative; z-index: 10;
  display: flex; align-items: center; gap: 32rpx;
}

.hero-avatar {
  position: relative; width: 128rpx; height: 128rpx; flex-shrink: 0;
  border-radius: 50%; border: 4rpx solid rgba(255,255,255,0.2);
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.2);
}
.avatar-btn {
  width: 100%; height: 100%; padding: 0; margin: 0; border: none; background: transparent; border-radius: 50%;
  &::after { border: none; }
}
.avatar-img { width: 100%; height: 100%; border-radius: 50%; }
.avatar-fallback { 
  width: 100%; height: 100%; border-radius: 50%; background: rgba(255,255,255,0.1); backdrop-filter: blur(10px);
  display: flex; align-items: center; justify-content: center;
}
.avatar-text { font-size: 56rpx; color: #fff; font-weight: 700; }

.hero-info { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.hero-name { font-size: 44rpx; font-weight: 800; color: var(--text-primary); margin-bottom: 12rpx; }
.hero-tags { display: flex; flex-wrap: wrap; gap: 16rpx; }
.tag-glass {
  display: inline-flex; align-items: center; gap: 8rpx;
  background: var(--bg-page);
  padding: 8rpx 24rpx; border-radius: 40rpx;
  font-size: 22rpx; font-weight: 600; color: var(--text-secondary);
}
.svg-icon-mini {
  width: 24rpx; height: 24rpx; 
  background-position: center; background-repeat: no-repeat; background-size: contain;
}
.hero-hint { font-size: 24rpx; color: var(--text-tertiary); }

.hero-edit { display: flex; align-items: center; background: var(--bg-page); padding: 12rpx 24rpx; border-radius: 40rpx; }
.hero-edit-text { font-size: 24rpx; color: var(--text-secondary); font-weight: 600; }
.hero-edit-arrow { font-size: 32rpx; color: var(--text-tertiary); margin-left: 4rpx; margin-top: -4rpx; }
.hero-arrow { font-size: 48rpx; color: var(--text-tertiary); }

/* =======================================
   2. 通知横幅 (Notification Banner)
   ======================================= */
.notify-banner {
  margin: 0 32rpx 32rpx; padding: 24rpx 32rpx;
  background: linear-gradient(135deg, rgba(255, 152, 0, 0.1), rgba(255, 152, 0, 0.05));
  border: 1rpx solid rgba(255, 152, 0, 0.3);
  border-radius: var(--radius-lg);
  display: flex; align-items: center; justify-content: space-between; gap: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 152, 0, 0.05);
}
.notify-banner__left { display: flex; align-items: center; gap: 16rpx; flex: 1; }
.notify-icon { font-size: 32rpx; }
.notify-banner__text { font-size: 24rpx; color: #d46b08; line-height: 1.4; font-weight: 600; }
.notify-banner__btn {
  margin: 0; padding: 0 32rpx; height: 56rpx; line-height: 56rpx;
  background: #ff9800; color: #fff; font-size: 24rpx; font-weight: 600; border-radius: 40rpx;
  box-shadow: 0 4rpx 12rpx rgba(255, 152, 0, 0.3); transition: all 0.2s;
  &::after { border: none; }
}
.btn-hover { transform: scale(0.95); opacity: 0.9; }

/* =======================================
   3. 菜单区 (Menu Groups)
   ======================================= */
.menu-group { margin-bottom: 32rpx; padding: 12rpx 0; border-radius: var(--radius-lg); }
.menu-cell {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 32rpx; background: transparent; transition: background 0.2s;
}
.menu-cell__left { display: flex; align-items: center; gap: 24rpx; }
.menu-icon-bg {
  width: 68rpx; height: 68rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  box-shadow: inset 0 2rpx 0 rgba(255,255,255,0.4), 0 8rpx 16rpx rgba(0,0,0,0.03);
}

.svg-icon {
  width: 36rpx; height: 36rpx;
  background-position: center; background-repeat: no-repeat; background-size: contain;
}

/* SVG inlines with matching colors */
.svg-icon-building { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M3 21h18'%3E%3C/path%3E%3Cpath d='M9 8h1'%3E%3C/path%3E%3Cpath d='M9 12h1'%3E%3C/path%3E%3Cpath d='M9 16h1'%3E%3C/path%3E%3Cpath d='M14 8h1'%3E%3C/path%3E%3Cpath d='M14 12h1'%3E%3C/path%3E%3Cpath d='M14 16h1'%3E%3C/path%3E%3Cpath d='M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16'%3E%3C/path%3E%3C/svg%3E"); }
.svg-icon-team { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239c27b0' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'%3E%3C/path%3E%3Ccircle cx='9' cy='7' r='4'%3E%3C/circle%3E%3Cpath d='M23 21v-2a4 4 0 0 0-3-3.87'%3E%3C/path%3E%3Cpath d='M16 3.13a4 4 0 0 1 0 7.75'%3E%3C/path%3E%3C/svg%3E"); }
.svg-icon-printer { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff9800' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 6 2 18 2 18 9'%3E%3C/polyline%3E%3Cpath d='M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2'%3E%3C/path%3E%3Crect x='6' y='14' width='12' height='8'%3E%3C/rect%3E%3C/svg%3E"); }
.svg-icon-backup { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2318bc37' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'%3E%3C/path%3E%3Cpolyline points='17 8 12 3 7 8'%3E%3C/polyline%3E%3Cline x1='12' y1='3' x2='12' y2='15'%3E%3C/line%3E%3C/svg%3E"); }

.svg-icon-building.svg-icon-mini { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M3 21h18'%3E%3C/path%3E%3Cpath d='M9 8h1'%3E%3C/path%3E%3Cpath d='M9 12h1'%3E%3C/path%3E%3Cpath d='M9 16h1'%3E%3C/path%3E%3Cpath d='M14 8h1'%3E%3C/path%3E%3Cpath d='M14 12h1'%3E%3C/path%3E%3Cpath d='M14 16h1'%3E%3C/path%3E%3Cpath d='M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16'%3E%3C/path%3E%3C/svg%3E"); }

.menu-text { font-size: 30rpx; font-weight: 700; color: var(--text-primary); letter-spacing: 1rpx; }

.menu-arrow-icon {
  width: 32rpx; height: 32rpx; opacity: 0.3;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='black' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='9 18 15 12 9 6'%3E%3C/polyline%3E%3C/svg%3E");
  background-position: center; background-repeat: no-repeat; background-size: contain;
}

.menu-divider { height: 1rpx; background: rgba(0,0,0,0.03); margin: 0 32rpx 0 120rpx; }

/* 图标背景色体系 */
.bg-blue { background: rgba(41, 121, 255, 0.1); }
.bg-purple { background: rgba(156, 39, 176, 0.1); }
.bg-orange { background: rgba(255, 152, 0, 0.1); }
.bg-green { background: rgba(24, 188, 55, 0.1); }

/* =======================================
   4. 退出登录按键 (Logout Button)
   ======================================= */
.logout-wrapper { margin-top: 48rpx; display: flex; justify-content: center; padding: 0 16rpx; }
.logout-btn {
  width: 100%; border-radius: 40rpx; background: #ffffff;
  border: 2rpx solid rgba(244, 67, 54, 0.2);
  box-shadow: 0 12rpx 32rpx rgba(244, 67, 54, 0.1), inset 0 0 16rpx rgba(244, 67, 54, 0.03); 
  padding: 28rpx 0;
  display: flex; justify-content: center; align-items: center;
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.2);
}
.logout-text { font-size: 32rpx; font-weight: 700; color: var(--color-danger); letter-spacing: 2rpx; }
.logout-btn--hover { 
  background: rgba(244, 67, 54, 0.05); 
  transform: scale(0.96); 
  border-color: rgba(244, 67, 54, 0.5);
  box-shadow: 0 4rpx 12rpx rgba(244, 67, 54, 0.15), inset 0 0 8rpx rgba(244, 67, 54, 0.05);
}

.safe-bottom-space { width: 100%; height: 80rpx; display: block; content: ''; }
</style>
