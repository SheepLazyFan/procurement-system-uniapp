<template>
  <view class="page-create-enterprise container">
    <NavBar title="创建企业" />

    <view class="card">
      <view class="form-group">
        <text class="form-label">企业名称 *</text>
        <input class="form-input" v-model="form.name" placeholder="请输入企业名称" />
      </view>
      <view class="form-group">
        <text class="form-label">联系人</text>
        <input class="form-input" v-model="form.contactName" placeholder="请输入联系人姓名" />
      </view>
      <view class="form-group">
        <text class="form-label">联系电话</text>
        <input class="form-input" v-model="form.contactPhone" placeholder="请输入联系电话" type="number" />
      </view>
      <view class="form-group">
        <text class="form-label">企业地址</text>
        <input class="form-input" v-model="form.address" placeholder="请输入企业地址" />
      </view>
    </view>

    <view class="card or-section">
      <text class="or-text">—— 或使用邀请码加入 ——</text>
      <view class="invite-row">
        <input class="form-input" v-model="inviteCode" placeholder="输入邀请码" style="flex:1" />
        <button class="btn-invite" @tap="handleJoin">加入</button>
      </view>
    </view>

    <button class="btn-primary" @tap="handleCreate">创建企业</button>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import { useEnterpriseStore } from '@/store/enterprise'
import { useUserStore } from '@/store/user'
import { joinByInviteCode } from '@/api/team'
import { WX_STOCK_WARNING_TEMPLATE_ID } from '@/config/index'

export default {
  components: { NavBar },
  data() {
    return {
      form: { name: '', contactName: '', contactPhone: '', address: '' },
      inviteCode: ''
    }
  },
  methods: {
    async handleCreate() {
      if (!this.form.name) return uni.showToast({ title: '请输入企业名称', icon: 'none' })
      try {
        await useEnterpriseStore().createEnterprise(this.form)
        await useUserStore().fetchProfile()
        uni.showToast({ title: '创建成功', icon: 'success' })
        setTimeout(() => this.goBack(), 500)
      } catch (e) {
        uni.showToast({ title: e.message || '创建失败', icon: 'none' })
      }
    },
    async handleJoin() {
      if (!this.inviteCode.trim()) return uni.showToast({ title: '请输入邀请码', icon: 'none' })

      // 在 tap 直接回调链中申请订阅配额，加入成功后方可推送预警通知
      // #ifdef MP-WEIXIN
      let subscribeAccepted = false
      await new Promise(resolve => {
        wx.requestSubscribeMessage({
          tmplIds: [WX_STOCK_WARNING_TEMPLATE_ID],
          success: (res) => {
            subscribeAccepted = res[WX_STOCK_WARNING_TEMPLATE_ID] === 'accept'
          },
          complete: () => resolve()
        })
      })
      // #endif

      try {
        await joinByInviteCode(this.inviteCode)
        await useUserStore().fetchProfile()
        // #ifdef MP-WEIXIN
        if (subscribeAccepted) {
          useUserStore().setNotifyEnabled(true)
        }
        // #endif
        uni.showToast({ title: '加入成功', icon: 'success' })
        setTimeout(() => this.goBack(), 500)
      } catch (e) {
        uni.showToast({ title: e.message || '加入失败', icon: 'none' })
      }
    },
    goBack() {
      // 获取页面栈判断来源
      const pages = getCurrentPages()
      if (pages.length > 1) {
        const prev = pages[pages.length - 2]
        // 如果上一页是 tabBar 页，用 switchTab 确保 onShow 正常触发
        const tabPages = ['pages/inventory/index', 'pages/purchase/index', 'pages/sales/index', 'pages/statistics/index', 'pages/profile/index']
        if (tabPages.includes(prev.route)) {
          uni.switchTab({ url: '/' + prev.route })
          return
        }
      }
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.form-group {
  margin-bottom: 24rpx;
}
.form-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}
.form-input {
  width: 100%;
  height: 76rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.or-section {
  text-align: center;
}
.or-text {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 20rpx;
}
.invite-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}
.btn-invite {
  width: 160rpx;
  height: 76rpx;
  line-height: 76rpx;
  background: #2979ff;
  color: #fff;
  font-size: 28rpx;
  border-radius: 12rpx;
  text-align: center;
  padding: 0;
}
</style>
