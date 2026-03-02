<template>
  <view class="page-purchase-detail container">
    <view class="card">
      <view class="flex-between">
        <text class="order-no">{{ order.orderNo }}</text>
        <StatusTag :text="getStatusText(order.status)" :type="getStatusType(order.status)" />
      </view>
      <view class="divider" />
      <view class="info-row">
        <text class="info-label">供应商</text>
        <text class="info-value">{{ order.supplierName || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ order.createdAt }}</text>
      </view>
      <view class="info-row" v-if="order.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order.remark }}</text>
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="card">
      <text class="section-title">商品明细</text>
      <view v-for="item in order.items" :key="item.productId" class="item-row">
        <view class="item-info">
          <text class="item-name">{{ item.productName }}</text>
          <text class="item-spec">{{ item.spec }} / {{ item.unit }}</text>
        </view>
        <view class="item-right">
          <text class="item-qty">x{{ item.quantity }}</text>
          <text class="item-amount">¥{{ item.amount }}</text>
        </view>
      </view>
      <view class="divider" />
      <view class="total-row flex-between">
        <text class="total-label">合计</text>
        <text class="total-amount price-text">¥{{ order.totalAmount }}</text>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-buttons" v-if="order.status">
      <button v-if="order.status === 'DRAFT'" class="btn-primary" @tap="handleAction('purchasing')">标记采购中</button>
      <button v-if="order.status === 'PURCHASING'" class="btn-primary" @tap="handleAction('arrive')">标记到货</button>
      <button v-if="order.status === 'ARRIVED'" class="btn-primary" @tap="handleAction('complete')">完成采购</button>
      <button v-if="['DRAFT','PURCHASING'].includes(order.status)" class="btn-cancel" @tap="handleAction('cancel')">取消采购</button>
    </view>
  </view>
</template>

<script>
import { getPurchaseOrderDetail, markPurchasing, markArrived, completePurchaseOrder, cancelPurchaseOrder } from '@/api/purchaseOrder'
import { getPurchaseStatusText } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'

export default {
  components: { StatusTag },
  data() {
    return {
      orderId: null,
      order: { items: [] }
    }
  },
  onLoad(query) {
    this.orderId = Number(query.id)
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        this.order = await getPurchaseOrderDetail(this.orderId)
      } catch (e) {}
    },
    getStatusText(status) {
      return getPurchaseStatusText(status)
    },
    getStatusType(status) {
      const map = { DRAFT: 'info', PURCHASING: 'primary', ARRIVED: 'success', COMPLETED: 'info', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    async handleAction(action) {
      const actionMap = {
        purchasing: markPurchasing,
        arrive: markArrived,
        complete: completePurchaseOrder,
        cancel: cancelPurchaseOrder
      }
      try {
        await actionMap[action](this.orderId)
        uni.showToast({ title: '操作成功', icon: 'success' })
        this.loadDetail()
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.order-no {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}

.info-label {
  font-size: 26rpx;
  color: #999;
}

.info-value {
  font-size: 26rpx;
  color: #333;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.item-name {
  font-size: 28rpx;
  color: #333;
}

.item-spec {
  font-size: 24rpx;
  color: #999;
}

.item-right {
  text-align: right;
}

.item-qty {
  font-size: 26rpx;
  color: #666;
  display: block;
}

.item-amount {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.total-row {
  padding: 16rpx 0;
}

.total-label {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.total-amount {
  font-size: 36rpx;
}

.action-buttons {
  padding: 32rpx 0;
}

.btn-cancel {
  margin-top: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: #f5f6fa;
  border-radius: 12rpx;
  color: #e43d33;
  font-size: 28rpx;
  border: none;
}
</style>
