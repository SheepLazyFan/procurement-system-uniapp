<template>
  <view class="page-profile container">
    <!-- 用户信息卡 -->
    <view class="user-card card">
      <view class="user-card__avatar">
        <image v-if="userInfo.avatarUrl" :src="userInfo.avatarUrl" class="avatar-img" mode="aspectFill" />
        <view v-else class="avatar-placeholder">
          <text class="avatar-text">{{ userInfo.nickName ? userInfo.nickName[0] : '?' }}</text>
        </view>
      </view>
      <view class="user-card__info">
        <text class="user-card__name">{{ userInfo.nickName || userInfo.phone || '未登录' }}</text>
        <text class="user-card__role">{{ roleText }}</text>
      </view>
    </view>

    <!-- 企业信息 -->
    <view class="card" v-if="hasEnterprise" @tap="goEnterprise">
      <view class="menu-item">
        <text class="menu-item__text">🏢 我的企业</text>
        <text class="menu-item__arrow">›</text>
      </view>
    </view>
    <view class="card" v-else @tap="goCreateEnterprise">
      <view class="menu-item">
        <text class="menu-item__text">🏢 创建企业</text>
        <text class="menu-item__arrow">›</text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="card">
      <view class="menu-item" @tap="goTeam">
        <text class="menu-item__text">👥 团队管理</text>
        <text class="menu-item__arrow">›</text>
      </view>
      <view class="menu-item" @tap="goPrinter">
        <text class="menu-item__text">🖨️ 打印机管理</text>
        <text class="menu-item__arrow">›</text>
      </view>
      <view class="menu-item" @tap="goBackup">
        <text class="menu-item__text">💾 数据备份</text>
        <text class="menu-item__arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="card" @tap="handleLogout">
      <view class="menu-item">
        <text class="menu-item__text" style="color: #e43d33;">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/user'

export default {
  computed: {
    userInfo() {
      return useUserStore().userInfo
    },
    hasEnterprise() {
      return useUserStore().hasEnterprise
    },
    roleText() {
      const map = { SELLER: '店主', MEMBER: '团队成员', BUYER: '买家' }
      return map[this.userInfo.role] || ''
    }
  },
  methods: {
    goEnterprise() {
      uni.navigateTo({ url: '/pages/profile/enterprise' })
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
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定退出登录？',
        success: (res) => {
          if (res.confirm) {
            useUserStore().logout()
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;

  &__avatar {
    width: 112rpx;
    height: 112rpx;
  }

  &__info {
    flex: 1;
  }

  &__name {
    display: block;
    font-size: 34rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 8rpx;
  }

  &__role {
    font-size: 24rpx;
    color: #2979ff;
    background: #e8f0fe;
    padding: 4rpx 12rpx;
    border-radius: 8rpx;
  }
}

.avatar-img {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
}

.avatar-placeholder {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: #2979ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-size: 48rpx;
  color: #fff;
  font-weight: 600;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &__text {
    font-size: 28rpx;
    color: #333;
  }

  &__arrow {
    font-size: 32rpx;
    color: #ccc;
  }
}
</style>
