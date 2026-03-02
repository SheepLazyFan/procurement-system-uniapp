<template>
  <view class="page-enterprise container">
    <NavBar title="企业信息" />

    <view class="card">
      <view class="form-group">
        <text class="form-label">企业名称</text>
        <input class="form-input" v-model="form.name" placeholder="请输入企业名称" />
      </view>
      <view class="form-group">
        <text class="form-label">联系人</text>
        <input class="form-input" v-model="form.contactName" placeholder="请输入联系人" />
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

    <!-- 邀请码 -->
    <view class="card" v-if="enterpriseInfo.inviteCode">
      <view class="form-group">
        <text class="form-label">邀请码</text>
        <view class="invite-code-row">
          <text class="invite-code">{{ enterpriseInfo.inviteCode }}</text>
          <text class="invite-refresh" @tap="handleRefreshCode">刷新</text>
        </view>
      </view>
    </view>

    <button class="btn-primary" @tap="handleSave">保存修改</button>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import { useEnterpriseStore } from '@/store/enterprise'

export default {
  components: { NavBar },
  data() {
    return {
      form: { name: '', contactName: '', contactPhone: '', address: '' }
    }
  },
  computed: {
    enterpriseInfo() {
      return useEnterpriseStore().info || {}
    }
  },
  onShow() {
    this.loadData()
  },
  methods: {
    async loadData() {
      const store = useEnterpriseStore()
      await store.fetchEnterprise()
      if (store.info) {
        this.form = {
          name: store.info.name || '',
          contactName: store.info.contactName || '',
          contactPhone: store.info.contactPhone || '',
          address: store.info.address || ''
        }
      }
    },
    async handleSave() {
      if (!this.form.name) return uni.showToast({ title: '请输入企业名称', icon: 'none' })
      try {
        await useEnterpriseStore().updateEnterprise(this.form)
        uni.showToast({ title: '保存成功' })
      } catch (e) {
        uni.showToast({ title: e.message || '保存失败', icon: 'none' })
      }
    },
    async handleRefreshCode() {
      uni.showModal({
        title: '提示',
        content: '刷新后旧邀请码将失效，确定？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await useEnterpriseStore().refreshInviteCode()
              uni.showToast({ title: '已刷新' })
            } catch (e) {
              uni.showToast({ title: '刷新失败', icon: 'none' })
            }
          }
        }
      })
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
.invite-code-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.invite-code {
  font-size: 36rpx;
  font-weight: 600;
  color: #2979ff;
  letter-spacing: 4rpx;
}
.invite-refresh {
  font-size: 24rpx;
  color: #2979ff;
  border: 1rpx solid #2979ff;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}
</style>
