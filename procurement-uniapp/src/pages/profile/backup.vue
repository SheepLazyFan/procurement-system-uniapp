<template>
  <view class="page-backup container">
    <NavBar title="数据备份" />

    <!-- 创建备份 -->
    <view class="card">
      <button class="btn-primary" @tap="handleCreate" :loading="creating">
        {{ creating ? '备份中...' : '立即备份' }}
      </button>
      <text class="tip-text">备份数据包含商品、订单、客户、供应商等核心数据</text>
    </view>

    <!-- 备份列表 -->
    <view class="card">
      <view class="section-title">历史备份</view>
      <view v-for="item in backupList" :key="item.id" class="backup-item">
        <view class="backup-info">
          <text class="backup-time">{{ formatDateTime(item.createdAt) }}</text>
          <text class="backup-size">{{ formatFileSize(item.fileSize) }}</text>
        </view>
        <text class="restore-btn" @tap="handleRestore(item)">恢复</text>
      </view>
      <EmptyState v-if="!backupList.length && !loading" text="暂无备份记录" icon="💾" />
      <LoadMore v-if="backupList.length" :status="loadStatus" />
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'
import { createBackup, getBackupList, restoreBackup } from '@/api/backup'
import { formatDateTime, formatFileSize } from '@/utils/format'

export default {
  components: { NavBar, EmptyState, LoadMore },
  data() {
    return {
      backupList: [],
      loading: false,
      creating: false,
      loadStatus: 'more'
    }
  },
  onShow() {
    this.loadList()
  },
  methods: {
    formatDateTime,
    formatFileSize,
    async loadList() {
      this.loading = true
      try {
        const res = await getBackupList()
        this.backupList = res.data || []
        this.loadStatus = 'noMore'
      } catch (e) {
        console.error('加载备份列表失败', e)
      } finally {
        this.loading = false
      }
    },
    async handleCreate() {
      this.creating = true
      try {
        await createBackup()
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
        title: '警告',
        content: '恢复备份将覆盖当前数据，确定恢复？',
        confirmColor: '#e43d33',
        success: async (res) => {
          if (res.confirm) {
            try {
              await restoreBackup(item.id)
              uni.showToast({ title: '恢复成功' })
            } catch (e) {
              uni.showToast({ title: e.message || '恢复失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.tip-text {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: #999;
  margin-top: 16rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
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
.backup-time {
  display: block;
  font-size: 28rpx;
  color: #333;
}
.backup-size {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}
.restore-btn {
  font-size: 24rpx;
  color: #ff9900;
  padding: 6rpx 20rpx;
  border: 1rpx solid #ff9900;
  border-radius: 8rpx;
}
</style>
