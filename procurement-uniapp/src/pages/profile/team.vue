<template>
  <view class="page-team">
    <NavBar title="团队管理" />

    <!-- 顶栏邀请卡片 (店主权限) -->
    <view class="invite-banner animate-fade-up" v-if="isSeller">
      <view class="invite-content">
        <text class="invite-label">内部邀请专属码</text>
        <view class="invite-number-row" @tap="handleCopyCode">
          <text class="invite-number num-font">{{ enterpriseInfo.inviteCode || '—' }}</text>
          <view class="svg-icon-copy"></view>
        </view>
        <text class="invite-desc">长按或点击复制，发送给即将入职的成员</text>
      </view>
      <view class="invite-bg-icon">🤝</view>
    </view>

    <!-- 成员名单 -->
    <view class="team-container animate-fade-up" style="animation-delay: 0.1s;">
      <view class="list-header">
        <text class="list-title">企业成员</text>
        <view class="list-count" v-if="!loading"><text class="count-text">{{ members.length }}</text></view>
      </view>

      <view v-if="loading" class="loading-state">
        <text class="loading-icon animate-spin">⏳</text>
        <text class="loading-text">正在加载骨干成员...</text>
      </view>
      
      <view v-else-if="members.length" class="member-list">
        <view class="saas-card member-card" v-for="(item, index) in members" :key="item.id" 
              hover-class="member-card--hover" :hover-start-time="0" :hover-stay-time="100">
          
          <view class="member-main">
            <!-- 头像区 -->
            <view class="member-avatar">
              <text class="avatar-letter">{{ (item.nickName || item.phone || '?').slice(0, 1) }}</text>
            </view>
            
            <!-- 信息区 -->
            <view class="member-info">
              <view class="info-top">
                <text class="member-name">{{ item.nickName || '未设置昵称' }}</text>
                <text class="role-badge" :class="'role-bg-' + (item.role || '').toLowerCase()">{{ roleLabel(item.role) }}</text>
              </view>
              <text class="member-phone num-font">{{ item.phone || '尚未绑定手机号' }}</text>
            </view>
          </view>

          <!-- 操作区 -->
          <view class="member-actions" v-if="isSeller && item.role !== 'SELLER'">
            <view class="action-btn action-edit" @tap.stop="handlePermission(item)">
              <text class="action-text">调岗</text>
            </view>
            <view class="action-btn action-remove" @tap.stop="handleRemove(item)">
              <text class="action-text text-danger">移除</text>
            </view>
          </view>
          
        </view>
      </view>
      
      <EmptyState v-else text="暂无其他成员" />
    </view>

    <view class="safe-bottom-space"></view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useEnterpriseStore } from '@/store/enterprise'
import { useUserStore } from '@/store/user'
import { getTeamMembers, setMemberPermissions, removeMember } from '@/api/team'

export default {
  components: { NavBar, EmptyState },
  data() {
    return {
      members: [],
      loading: false
    }
  },
  computed: {
    enterpriseInfo() {
      return useEnterpriseStore().info || {}
    },
    isSeller() {
      return useUserStore().isSeller
    }
  },
  onShow() {
    this.loadMembers()
  },
  methods: {
    handleCopyCode() {
      const code = this.enterpriseInfo.inviteCode
      if (!code) return
      uni.setClipboardData({ data: code, success: () => uni.showToast({ title: '已复制邀请码' }) })
    },
    /** 角色名称映射 */
    roleLabel(role) {
      const map = { SELLER: '店主', ADMIN: '管理员', SALES: '销售员', WAREHOUSE: '仓管员' }
      return map[role] || '成员'
    },
    async loadMembers() {
      this.loading = true
      try {
        const res = await getTeamMembers()
        this.members = res || []
      } catch (e) {
        console.error('加载成员失败', e)
      } finally {
        this.loading = false
      }
    },
    handlePermission(member) {
      const roles = ['管理员', '销售员', '仓管员']
      const roleValues = ['ADMIN', 'SALES', 'WAREHOUSE']
      uni.showActionSheet({
        itemList: roles,
        success: async (res) => {
          const newRole = roleValues[res.tapIndex]
          if (newRole === member.role) {
            uni.showToast({ title: '角色未变更', icon: 'none' })
            return
          }
          try {
            await setMemberPermissions(member.id, { role: newRole })
            uni.showToast({ title: '角色已更新' })
            this.loadMembers()
          } catch (e) {
            uni.showToast({ title: e.message || '设置失败', icon: 'none' })
          }
        }
      })
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
.page-team {
  min-height: 100vh; background: var(--bg-page); padding: 24rpx;
  padding-bottom: 200rpx; position: relative;
}

/* =======================================
   1. 邀请卡片区
   ======================================= */
.invite-banner {
  position: relative; background: #ffffff;
  border-radius: 40rpx; padding: 48rpx 40rpx; margin-bottom: 32rpx;
  overflow: hidden; box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.03);
}
.invite-content { position: relative; z-index: 2; display: flex; flex-direction: column; }
.invite-label { font-size: 26rpx; color: var(--text-secondary); margin-bottom: 12rpx; font-weight: 600; }
.invite-number-row { display: inline-flex; align-items: center; gap: 16rpx; margin-bottom: 16rpx; }
.invite-number { font-size: 64rpx; font-weight: 800; color: var(--brand-primary); letter-spacing: 4rpx; }
.svg-icon-copy {
  width: 44rpx; height: 44rpx; opacity: 0.85; margin-left: 4rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23374151' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='9' y='9' width='13' height='13' rx='2' ry='2'%3E%3C/rect%3E%3Cpath d='M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1'%3E%3C/path%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.invite-desc { font-size: 24rpx; color: var(--text-tertiary); }

.invite-bg-icon {
  position: absolute; right: -20rpx; bottom: -40rpx;
  font-size: 200rpx; opacity: 0.02; z-index: 1; pointer-events: none; filter: grayscale(100%);
}

/* =======================================
   2. 成员名单区
   ======================================= */
.list-header { display: flex; align-items: center; gap: 16rpx; margin-bottom: 24rpx; padding: 0 8rpx; }
.list-title { font-size: 32rpx; font-weight: 700; color: var(--text-primary); }
.list-count { background: rgba(41, 121, 255, 0.1); padding: 4rpx 16rpx; border-radius: 20rpx; }
.count-text { font-size: 24rpx; font-weight: 700; color: var(--brand-primary); }

.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 300rpx; gap: 16rpx; }
.loading-icon { font-size: 48rpx; }
.loading-text { font-size: 26rpx; color: var(--text-tertiary); }

.member-list { display: flex; flex-direction: column; gap: 24rpx; }
.member-card { 
  display: flex; flex-direction: column; padding: 32rpx 28rpx;
  border: 1rpx solid transparent; transition: all 0.2s;
}
.member-card--hover { transform: scale(0.98); border-color: rgba(41, 121, 255, 0.2); box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.05); }

.member-main { display: flex; align-items: center; gap: 24rpx; }
.member-avatar {
  position: relative; width: 96rpx; height: 96rpx; border-radius: 50%;
  background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 8rpx 16rpx rgba(142, 197, 252, 0.3);
}
.avatar-letter { font-size: 40rpx; font-weight: 800; color: #fff; }

.member-info { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.info-top { display: flex; align-items: center; gap: 16rpx; margin-bottom: 8rpx; }
.member-name { font-size: 32rpx; font-weight: 800; color: var(--text-primary); letter-spacing: 1rpx; }
.role-badge { font-size: 20rpx; font-weight: 700; padding: 6rpx 16rpx; border-radius: 30rpx; text-transform: uppercase; letter-spacing: 1rpx; }
.role-bg-seller { background: rgba(255, 152, 0, 0.1); color: #ff9800; }
.role-bg-admin { background: rgba(41, 121, 255, 0.1); color: var(--brand-primary); }
.role-bg-sales { background: rgba(24, 188, 55, 0.1); color: var(--color-success); }
.role-bg-warehouse { background: rgba(156, 39, 176, 0.1); color: #9c27b0; }

.member-phone { font-size: 24rpx; color: var(--text-tertiary); letter-spacing: 1rpx; }

.member-actions {
  display: flex; justify-content: flex-end; gap: 16rpx; margin-top: 24rpx;
  padding-top: 24rpx; border-top: 1rpx dashed var(--border-light);
}
.action-btn { padding: 12rpx 32rpx; border-radius: 30rpx; background: #f5f6fa; transition: background 0.2s; }
.action-text { font-size: 24rpx; font-weight: 600; color: var(--text-secondary); }
.text-danger { color: var(--color-danger); }
.action-remove:active { background: rgba(244, 67, 54, 0.1); }
.action-edit:active { background: rgba(41, 121, 255, 0.1); }

.safe-bottom-space { display: block; width: 100%; height: 60rpx; }
</style>
