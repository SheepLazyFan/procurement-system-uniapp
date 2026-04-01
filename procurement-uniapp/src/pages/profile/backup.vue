<template>
  <view class="page-backup container">
    <NavBar title="数据备份" />

    <!-- 备份核心舱 -->
    <view class="backup-hero animate-fade-up">
      <view class="core-ring" :class="{ 'is-backing-up': creating }">
        <view class="ring-bg"></view>
        <view class="ring-spinner"></view>
        <view class="core-icon-box">
          <view class="svg-icon-cloud" :class="{ 'is-spinning': creating }"></view>
        </view>
      </view>
      
      <view class="last-backup" v-if="backupList.length && !loading">
        <text class="last-label">上一次全量备份</text>
        <view class="flip-clock-group">
          <text class="flip-text num-font">{{ formatDateTime(backupList[0].createdAt) }}</text>
        </view>
      </view>

      <view class="hero-btn-container" hover-class="hero-btn--hover" :class="{ 'is-disabled': creating }" @tap="creating ? null : handleCreate()">
        <text class="btn-text">{{ creating ? '主数据安全同步中...' : '立即启动全量脱机备份' }}</text>
      </view>
      <text class="hero-tip-text">采用硬核加密引擎，涵盖订单流水及所有核心凭证</text>
    </view>

    <!-- 备份列表 -->
    <view class="saas-card history-card animate-fade-up" style="animation-delay: 0.1s;">
      <view class="section-title">历史备份</view>
      <view v-if="loading" class="loading-tip">加载中...</view>
      <template v-else>
        <view v-for="item in backupList" :key="item.id" class="backup-item">
          <view class="backup-info">
            <view class="backup-header">
              <text class="backup-time">{{ formatDateTime(item.createdAt) }}</text>
              <view class="backup-status" :class="'status-' + (item.status || '').toLowerCase()">
                <view class="status-dot"></view>
                <text class="status-txt">{{ statusText(item.status) }}</text>
              </view>
            </view>
            <text class="backup-detail">{{ item.remark || '全量备份' }} · {{ formatFileSize(item.fileSize) }}</text>
          </view>
          <view class="backup-actions" v-if="item.status === 'COMPLETED'">
            <text class="action-text download" @tap="handleDownload(item)">下载</text>
            <text class="action-text restore" @tap="handleRestore(item)">恢复</text>
            <text class="action-text delete" @tap="handleDelete(item)">删除</text>
          </view>
        </view>
        <EmptyState v-if="!backupList.length" text="暂无备份记录" />
      </template>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { createBackup, getBackupList, restoreBackup, deleteBackup, downloadBackup } from '@/api/backup'
import { formatDateTime, formatFileSize } from '@/utils/format'
import { useUserStore } from '@/store/user'

export default {
  components: { NavBar, EmptyState },
  data() {
    return {
      backupList: [],
      loading: false,
      creating: false
    }
  },
  onShow() {
    if (!useUserStore().isSeller) {
      uni.showToast({ title: '仅店主可操作数据备份', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
      return
    }
    this.loadList()
  },
  methods: {
    formatDateTime,
    formatFileSize,
    statusText(status) {
      const map = { PROCESSING: '备份中', COMPLETED: '已完成', FAILED: '失败' }
      return map[status] || status
    },
    async loadList() {
      this.loading = true
      try {
        const res = await getBackupList()
        this.backupList = res || []
      } catch (e) {
        uni.showToast({ title: '加载备份列表失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async handleCreate() {
      this.creating = true
      try {
        await createBackup('FULL')
        uni.showToast({ title: '备份成功' })
        this.loadList()
      } catch (e) {
        uni.showToast({ title: e.message || '备份失败', icon: 'none' })
      } finally {
        this.creating = false
      }
    },
    handleRestore(item) {
      uni.showModal({
        title: '⚠️ 警告',
        content: '恢复备份将覆盖当前全部业务数据（商品、订单、客户、供应商等），此操作不可撤销！确定恢复？',
        confirmColor: '#e43d33',
        confirmText: '确定恢复',
        success: async (res) => {
          if (res.confirm) {
            try {
              await restoreBackup(item.id)
              uni.showToast({ title: '恢复成功' })
              this.loadList()
            } catch (e) {
              uni.showToast({ title: e.message || '恢复失败', icon: 'none' })
            }
          }
        }
      })
    },
    async handleDownload(item) {
      try {
        uni.showLoading({ title: '下载中...', mask: true })
        const tempPath = await downloadBackup(item.id)
        uni.hideLoading()
        // #ifdef MP-WEIXIN
        wx.shareFileMessage({
          filePath: tempPath,
          fileName: `backup_${item.id}.json`,
          success: () => uni.showToast({ title: '文件已发送' }),
          fail: () => uni.showToast({ title: '已取消', icon: 'none' })
        })
        // #endif
        // #ifndef MP-WEIXIN
        uni.showToast({ title: '下载成功' })
        // #endif
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '下载失败', icon: 'none' })
      }
    },
    handleDelete(item) {
      uni.showModal({
        title: '提示',
        content: '确定删除该备份？删除后无法恢复。',
        confirmColor: '#e43d33',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteBackup(item.id)
              uni.showToast({ title: '已删除' })
              this.loadList()
            } catch (e) {
              uni.showToast({ title: e.message || '删除失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-backup { padding: 32rpx; min-height: 100vh; background: var(--bg-page); }

/* =======================================
   1. 硬核安全舱 (Backup Hero)
   ======================================= */
.backup-hero {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 80rpx 40rpx 60rpx; background: #ffffff;
  border-radius: 40rpx; margin-bottom: 32rpx;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.03);
  position: relative; overflow: hidden;
}
.core-ring {
  position: relative; width: 280rpx; height: 280rpx; margin-bottom: 48rpx;
  display: flex; align-items: center; justify-content: center;
}
.ring-bg {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; box-sizing: border-box;
  border-radius: 50%; border: 4rpx solid rgba(0,0,0,0.04);
}
.ring-spinner {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; box-sizing: border-box;
  border-radius: 50%; border: 4rpx solid transparent; 
  border-top-color: var(--brand-primary); border-right-color: rgba(41, 121, 255, 0.5);
  opacity: 0; transition: all 0.4s;
}
.core-ring.is-backing-up .ring-spinner {
  opacity: 1; animation: spin-ring 1.2s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
}
@keyframes spin-ring { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.core-icon-box {
  width: 160rpx; height: 160rpx; border-radius: 50%;
  background: var(--bg-page);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.03);
  border: 4rpx solid #ffffff;
}
.svg-icon-cloud {
  width: 80rpx; height: 80rpx; position: relative; z-index: 2;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z'%3E%3C/path%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
  transition: all 0.4s;
}
.core-ring.is-backing-up .svg-icon-cloud {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8'%3E%3C/path%3E%3Cpath d='M3 3v5h5'%3E%3C/path%3E%3Cpath d='M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16'%3E%3C/path%3E%3Cpath d='M16 21v-5h5'%3E%3C/path%3E%3C/svg%3E");
  animation: svg-spin 1.5s linear infinite;
}
@keyframes svg-spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.last-backup { display: flex; flex-direction: column; align-items: center; margin-bottom: 48rpx; }
.last-label { font-size: 24rpx; font-weight: 700; color: var(--text-tertiary); margin-bottom: 12rpx; letter-spacing: 2rpx; text-transform: uppercase; }
.flip-clock-group {
  background: var(--bg-page); padding: 16rpx 36rpx; border-radius: 20rpx;
  border: 2rpx solid rgba(0,0,0,0.03);
}
.flip-text { font-size: 36rpx; font-weight: 800; color: var(--text-primary); letter-spacing: 2rpx; }

.hero-btn-container {
  width: 100%; height: 96rpx; border-radius: 48rpx;
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  box-shadow: 0 12rpx 32rpx rgba(14, 165, 233, 0.3); margin-bottom: 24rpx;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.2);
  display: flex; justify-content: center; align-items: center;
}
.hero-btn-container.is-disabled { background: rgba(255,255,255,0.1); box-shadow: none; pointer-events: none; }
.hero-btn-container.is-disabled .btn-text { color: rgba(255,255,255,0.4); }
.btn-text { font-size: 30rpx; font-weight: 800; color: #fff; letter-spacing: 1rpx; }
.hero-btn--hover { transform: scale(0.96); box-shadow: 0 8rpx 16rpx rgba(14, 165, 233, 0.2); }

.hero-tip-text { font-size: 22rpx; color: var(--text-tertiary); text-align: center; font-weight: 500; }

/* =======================================
   2. 历史备份列表
   ======================================= */
.history-card { padding: 32rpx; border-radius: var(--radius-lg); background: #fff; }
.section-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 32rpx;
}
.backup-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.backup-info { flex: 1; }
.backup-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 6rpx;
}
.backup-time {
  font-size: 28rpx;
  color: #333;
}
.backup-status { display: flex; align-items: center; gap: 8rpx; }
.status-dot { width: 12rpx; height: 12rpx; border-radius: 50%; }
.status-txt { font-size: 22rpx; font-weight: 600; color: var(--text-secondary); }
.status-completed .status-dot { background: var(--color-success); box-shadow: 0 0 12rpx rgba(24, 188, 55, 0.4); }
.status-completed .status-txt { color: var(--color-success); }
.status-processing .status-dot { background: #ff9800; animation: dot-breathe 2s infinite; }
.status-processing .status-txt { color: #ff9800; }
.status-failed .status-dot { background: var(--color-danger); }
.status-failed .status-txt { color: var(--color-danger); }
@keyframes dot-breathe { 0% { opacity: 0.4; transform: scale(0.8); } 50% { opacity: 1; transform: scale(1.2); } 100% { opacity: 0.4; transform: scale(0.8); } }

.backup-detail {
  display: block; font-size: 22rpx; color: #999; margin-top: 4rpx;
}
.backup-actions {
  display: flex; align-items: center; gap: 24rpx; margin-left: 16rpx;
}
.action-text {
  font-size: 24rpx; font-weight: 700; transition: opacity 0.2s;
  padding: 12rpx 0;
  &.download { color: var(--brand-primary); }
  &.restore { color: #ff9800; }
  &.delete { color: var(--color-danger); }
  &:active { opacity: 0.5; }
}
</style>
