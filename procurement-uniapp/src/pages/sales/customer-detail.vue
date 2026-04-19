<template>
  <view class="page-customer-detail">
    <view class="saas-card animate-fade-up" style="animation-delay: 0s;">
      <text class="customer-name">{{ customer.name }}</text>
      <view class="info-row">
        <text class="info-label">电话</text>
        <text class="info-value num-font">{{ customer.phone || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">地址</text>
        <text class="info-value">{{ customer.address || '-' }}</text>
      </view>
      <view class="info-row" v-if="customer.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ customer.remark }}</text>
      </view>
    </view>

    <view class="saas-card animate-fade-up" style="animation-delay: 0.1s;">
      <view class="stats-row">
        <view class="stats-item">
          <text class="stats-value num-font">{{ customer.orderCount || 0 }}</text>
          <text class="stats-label">订单数</text>
        </view>
        <view class="stats-item">
          <text class="stats-value stats-value--red num-font">¥{{ customer.totalAmount || 0 }}</text>
          <text class="stats-label">累计金额</text>
        </view>
      </view>
    </view>

    <!-- 最近订单 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0.2s;">
      <text class="section-title">最近订单</text>
      <view v-for="order in customer.recentOrders" :key="order.id" class="order-acc" :class="{ 'order-acc--expanded': expandedOrders[order.id] }">
        <!-- 收起行：时间 + 状态 + 金额 -->
        <view class="order-acc__header" @tap="toggleOrder(order.id)" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
          <text class="order-acc__time">{{ formatTime(order.createdAt) }}</text>
          <StatusTag :text="statusLabel(order.status)" :type="getStatusType(order.status)" class="order-acc__status" />
          <text class="order-acc__amount num-font">¥{{ order.totalAmount }}</text>
          <text class="order-acc__arrow" :style="{ transform: expandedOrders[order.id] ? 'rotate(180deg)' : 'rotate(0deg)' }">▾</text>
        </view>
        <!-- 展开区：订单号 + 付款状态 -->
        <view v-if="expandedOrders[order.id]" class="order-acc__detail">
          <view class="detail-row" @tap.stop="goOrderDetail(order.id)" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
            <text class="detail-label">订单号</text>
            <view class="detail-value-wrap">
              <text class="detail-value--link num-font">{{ order.orderNo }}</text>
              <text class="detail-link-icon">›</text>
            </view>
          </view>
          <view class="detail-row">
            <text class="detail-label">付款状态</text>
            <StatusTag :text="paymentLabel(order.paymentStatus)" :type="getPaymentType(order.paymentStatus)" />
          </view>
        </view>
      </view>
      <EmptyState v-if="!customer.recentOrders || customer.recentOrders.length === 0" text="暂无订单" />
    </view>

    <!-- 悬浮操作舱 -->
    <view class="bottom-action-bar safe-area-bottom animate-fade-up" style="animation-delay: 0.3s;">
      <button class="btn-ghost" @tap="handleDelete" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">删除客户</button>
      <button class="btn-primary" @tap="showEditForm" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">编辑信息</button>
    </view>

    <!-- 编辑弹窗 -->
    <view class="popup-mask" v-if="showEditPopup" @tap="showEditPopup = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">编辑客户</text>
          <text class="popup-close" @tap="showEditPopup = false">✕</text>
        </view>
        <view class="popup-form">
          <view class="form-group">
            <text class="form-label">名称 *</text>
            <input v-model="editForm.name" placeholder="客户名称" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">电话</text>
            <input v-model="editForm.phone" placeholder="联系电话" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">地址</text>
            <input v-model="editForm.address" placeholder="地址" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">备注</text>
            <input v-model="editForm.remark" placeholder="备注" class="form-input" />
          </view>
          <button class="btn-primary" @tap="handleEdit">保存修改</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getCustomerDetail, updateCustomer, deleteCustomer } from '@/api/customer'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusTag from '@/components/common/StatusTag.vue'

export default {
  components: { EmptyState, StatusTag },
  data() {
    return {
      customerId: null,
      customer: {},
      expandedOrders: {},
      showEditPopup: false,
      editForm: { name: '', phone: '', address: '', remark: '' }
    }
  },
  onLoad(query) {
    this.customerId = Number(query.id)
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        this.customer = await getCustomerDetail(this.customerId)
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    goOrderDetail(id) {
      uni.navigateTo({ url: `/pages/sales/detail?id=${id}` })
    },
    toggleOrder(orderId) {
      this.expandedOrders = { ...this.expandedOrders, [orderId]: !this.expandedOrders[orderId] }
    },
    formatTime(dateStr) {
      if (!dateStr) return ''
      const d = new Date(dateStr.replace(' ', 'T'))
      if (isNaN(d.getTime())) return dateStr
      const M = String(d.getMonth() + 1).padStart(2, '0')
      const D = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      return `${M}-${D} ${h}:${m}`
    },
    statusLabel(status) {
      const map = { PENDING: '待确认', CONFIRMED: '已确认', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
      return map[status] || status || '-'
    },
    getStatusType(status) {
      const map = { PENDING: 'warning', CONFIRMED: 'primary', SHIPPED: 'purple', COMPLETED: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    paymentLabel(status) {
      const map = { UNPAID: '未付款', CLAIMED: '待确认收款', PAID: '已付款' }
      return map[status] || status || '-'
    },
    getPaymentType(status) {
      const map = { UNPAID: 'warning', CLAIMED: 'primary', PAID: 'success' }
      return map[status] || 'info'
    },
    showEditForm() {
      this.editForm = {
        name: this.customer.name || '',
        phone: this.customer.phone || '',
        address: this.customer.address || '',
        remark: this.customer.remark || ''
      }
      this.showEditPopup = true
    },
    async handleEdit() {
      if (!this.editForm.name.trim()) {
        return uni.showToast({ title: '请输入客户名称', icon: 'none' })
      }
      try {
        await updateCustomer(this.customerId, this.editForm)
        uni.showToast({ title: '修改成功', icon: 'success' })
        this.showEditPopup = false
        this.loadDetail()
      } catch (e) {
        uni.showToast({ title: '修改失败', icon: 'none' })
      }
    },
    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: `确定删除客户「${this.customer.name}」吗？删除后不可恢复。`,
        confirmColor: '#e43d33',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteCustomer(this.customerId)
              uni.showToast({ title: '删除成功', icon: 'success' })
              setTimeout(() => uni.navigateBack(), 1000)
            } catch (e) {
              uni.showToast({ title: '删除失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-customer-detail {
  padding: 24rpx;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.customer-name { font-size: 40rpx; font-weight: 700; color: var(--text-primary); display: block; margin-bottom: 24rpx; }
.section-title { font-size: 32rpx; font-weight: 600; color: var(--text-primary); display: block; margin-bottom: 24rpx; }

.info-row { display: flex; justify-content: space-between; padding: 12rpx 0; }
.info-label { font-size: 28rpx; color: var(--text-secondary); }
.info-value { font-size: 28rpx; color: var(--text-primary); font-weight: 500; text-align: right; max-width: 75%; }

.stats-row { display: flex; justify-content: space-around; padding: 12rpx 0; }
.stats-item { text-align: center; }
.stats-value { display: block; font-size: 52rpx; font-weight: 700; color: var(--text-primary); line-height: 1.2; margin-bottom: 8rpx; }
.stats-value--red { color: var(--color-danger); }
.stats-label { font-size: 24rpx; color: var(--text-tertiary); }

/* 近期订单手风琴 */
.order-acc {
  background: var(--bg-page);
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
  transition: all 0.3s;
  
  &--expanded {
    background: var(--brand-primary-light);
  }
}
.order-acc__header {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  gap: 16rpx;
}
.order-acc__time { font-size: 26rpx; color: var(--text-secondary); font-family: var(--font-number); flex-shrink: 0; }
.order-acc__status { flex-shrink: 0; }
.order-acc__amount { font-size: 30rpx; font-weight: 700; color: var(--color-danger); margin-left: auto; }
.order-acc__arrow { font-size: 24rpx; color: var(--text-tertiary); width: 32rpx; text-align: center; transition: transform 0.3s; }

.order-acc__detail {
  padding: 0 24rpx 20rpx;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
}
.detail-label { font-size: 26rpx; color: var(--text-secondary); }
.detail-value-wrap { display: flex; align-items: center; }
.detail-value--link { color: var(--brand-primary); font-size: 28rpx; font-weight: 600; }
.detail-link-icon { color: var(--brand-primary); font-size: 32rpx; margin-left: 8rpx; }

/* 悬浮操作舱 */
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);
  z-index: 100;
  box-sizing: border-box;
}
.bottom-action-bar button {
  margin: 0; padding: 0; display: flex; align-items: center; justify-content: center;
  height: 88rpx; border-radius: 44rpx; font-size: 30rpx; font-weight: 600; border: none;
  &::after { border: none; }
}
.btn-ghost {
  flex: 1; background: var(--bg-page); color: var(--color-danger);
}
.btn-primary {
  flex: 2; background: var(--brand-primary); color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.2);
}

/* 编辑弹窗 */
.popup-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 999; display: flex; align-items: flex-end;
}
.popup-content {
  width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding-bottom: env(safe-area-inset-bottom);
}
.popup-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 32rpx 40rpx; border-bottom: 1rpx solid var(--border-light);
}
.popup-title { font-size: 34rpx; font-weight: 600; color: var(--text-primary); }
.popup-close { font-size: 36rpx; color: var(--text-tertiary); padding: 8rpx; }
.popup-form { padding: 32rpx 40rpx; }
.form-group { margin-bottom: 32rpx; }
.form-label { display: block; font-size: 28rpx; font-weight: 500; color: var(--text-primary); margin-bottom: 16rpx; }
.form-input {
  height: 88rpx; background: var(--bg-page); border-radius: 16rpx; padding: 0 24rpx; font-size: 28rpx; color: var(--text-primary);
  border: 2rpx solid transparent; transition: all 0.2s;
  &:focus { border-color: var(--brand-primary-light); background: #fff; box-shadow: 0 4rpx 16rpx rgba(41,121,255,0.08); }
}
.popup-form .btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: var(--brand-primary);
  color: #fff;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  margin-top: 48rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.2);
  &::after { border: none; }
}
</style>
