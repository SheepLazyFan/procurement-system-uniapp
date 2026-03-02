<template>
  <view class="page-team container">
    <NavBar title="团队管理" />

    <!-- 邀请码 -->
    <view class="card invite-card">
      <text class="invite-title">邀请码</text>
      <text class="invite-code">{{ enterpriseInfo.inviteCode || '—' }}</text>
      <text class="invite-tip">分享邀请码让其他成员加入团队</text>
    </view>

    <!-- 成员列表 -->
    <view class="card">
      <view class="section-title">团队成员</view>
      <view v-for="item in members" :key="item.id" class="member-item">
        <view class="member-info">
          <text class="member-name">{{ item.nickName || item.phone }}</text>
          <text class="member-role">{{ item.role === 'SELLER' ? '店主' : '成员' }}</text>
        </view>
        <view class="member-actions" v-if="item.role !== 'SELLER'">
          <text class="action-btn" @tap="handlePermission(item)">权限</text>
          <text class="action-btn danger" @tap="handleRemove(item)">移除</text>
        </view>
      </view>
      <EmptyState v-if="!members.length" text="暂无团队成员" />
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useEnterpriseStore } from '@/store/enterprise'
import { getTeamMembers, setMemberPermissions, removeMember } from '@/api/team'

export default {
  components: { NavBar, EmptyState },
  data() {
    return {
      members: []
    }
  },
  computed: {
    enterpriseInfo() {
      return useEnterpriseStore().info || {}
    }
  },
  onShow() {
    this.loadMembers()
  },
  methods: {
    async loadMembers() {
      try {
        const res = await getTeamMembers()
        this.members = res.data || []
      } catch (e) {
        console.error('加载成员失败', e)
      }
    },
    handlePermission(member) {
      // Phase 3: 权限设置弹窗
      uni.showToast({ title: 'Phase 3 实现权限设置', icon: 'none' })
    },
    handleRemove(member) {
      uni.showModal({
        title: '提示',
        content: `确定移除 ${member.nickName || member.phone}？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              await removeMember(member.id)
              uni.showToast({ title: '已移除' })
              this.loadMembers()
            } catch (e) {
              uni.showToast({ title: '移除失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.invite-card {
  text-align: center;
}
.invite-title {
  display: block;
  font-size: 26rpx;
  color: #999;
  margin-bottom: 12rpx;
}
.invite-code {
  display: block;
  font-size: 60rpx;
  font-weight: 700;
  color: #2979ff;
  letter-spacing: 8rpx;
  margin-bottom: 12rpx;
}
.invite-tip {
  display: block;
  font-size: 22rpx;
  color: #bbb;
}
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}
.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.member-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.member-name {
  font-size: 28rpx;
  color: #333;
}
.member-role {
  font-size: 22rpx;
  color: #2979ff;
  background: #e8f0fe;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}
.member-actions {
  display: flex;
  gap: 16rpx;
}
.action-btn {
  font-size: 24rpx;
  color: #2979ff;
  padding: 6rpx 16rpx;
  border: 1rpx solid #2979ff;
  border-radius: 8rpx;
  &.danger {
    color: #e43d33;
    border-color: #e43d33;
  }
}
</style>
