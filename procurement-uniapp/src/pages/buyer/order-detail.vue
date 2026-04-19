<template>
  <view class="page-order-detail">
    <view class="container" v-if="order.id">
      <!-- 状态头部 -->
      <view class="status-header" :class="'status-header--' + order.status">
        <StatusTag :status="order.status" :type="statusTagType(order.status)" />
        <text class="status-header__text">{{ statusDesc }}</text>
      </view>

      <!-- 订单信息 -->
      <view class="detail-card">
        <view class="detail-card__title">订单信息</view>
        <view class="info-row">
          <text class="info-row__label">订单号</text>
          <text class="info-row__value">{{ order.orderNo }}</text>
        </view>
        <view class="info-row">
          <text class="info-row__label">下单时间</text>
          <text class="info-row__value">{{ formatDateTime(order.createdAt) }}</text>
        </view>
        <view class="info-row" v-if="order.contactName">
          <text class="info-row__label">联系人</text>
          <text class="info-row__value">{{ order.contactName }}</text>
        </view>
        <view class="info-row" v-if="order.contactPhone">
          <text class="info-row__label">联系电话</text>
          <text class="info-row__value">{{ order.contactPhone }}</text>
        </view>
        <view class="info-row" v-if="order.address">
          <text class="info-row__label">收货地址</text>
          <text class="info-row__value">{{ order.address }}</text>
        </view>
        <view class="info-row" v-if="order.remark">
          <text class="info-row__label">备注</text>
          <text class="info-row__value">{{ order.remark }}</text>
        </view>
      </view>

      <!-- 商品列表 -->
      <view class="detail-card">
        <view class="detail-card__title">商品明细</view>
        <view v-for="item in order.items" :key="item.id" class="goods-line">
          <text class="goods-line__name">{{ item.productName }}</text>
          <text class="goods-line__qty">×{{ item.quantity }}</text>
          <text class="goods-line__amount">￥{{ item.amount }}</text>
        </view>
        <view class="goods-total">
          <text class="goods-total__label">合计</text>
          <text class="goods-total__price">￥{{ order.totalAmount }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-area">
        <button v-if="order.paymentStatus === 'UNPAID' && order.status !== 'CANCELLED' && order.status !== 'COMPLETED'" class="btn-pay-action" hover-class="btn-pay-action--hover" @tap="handlePay">去支付</button>
        <button v-if="order.status === 'PENDING' && order.paymentStatus === 'UNPAID'" class="btn-cancel-action" hover-class="btn-cancel-action--hover" @tap="handleCancel">取消订单</button>
      </view>
    </view>

    <!-- 付款弹窗 -->
    <PayQrPopup
      :show="showPayPopup"
      :amount="order.totalAmount"
      :enterpriseId="order.enterpriseId"
      :orderId="orderId"
      @close="showPayPopup = false"
      @paid="onPaySuccess"
    />
  </view>
</template>

<script>
import { getBuyerOrderDetail, cancelBuyerOrder } from '@/api/buyer'
import { getSalesStatusText, formatDateTime } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'
import PayQrPopup from '@/components/buyer/PayQrPopup.vue'

export default {
  components: { StatusTag, PayQrPopup },
  data() {
    return {
      orderId: null,
      order: { items: [] },
      showPayPopup: false
    }
  },
  computed: {
    statusDesc() {
      const s = this.order.status
      const p = this.order.paymentStatus
      if (s === 'CANCELLED') return '订单已取消'
      if (s === 'COMPLETED') return '交易完成'
      if (p === 'UNPAID') return '等待买家付款'
      if (p === 'CLAIMED') return '已声明付款，等待商家确认'
      if (p === 'PAID') return '付款已确认'
      return ''
    }
  },
  onLoad(query) {
    this.orderId = query.id
    this.loadDetail()
  },
  methods: {
    formatDateTime,
    async loadDetail() {
      try {
        this.order = await getBuyerOrderDetail(this.orderId) || { items: [] }
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    statusTagType(status) {
      const map = { PENDING: 'warning', CONFIRMED: 'primary', SHIPPED: 'purple', COMPLETED: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    handlePay() {
      this.showPayPopup = true
    },
    onPaySuccess() {
      this.showPayPopup = false
      this.loadDetail()
    },
    handleCancel() {
      uni.showModal({
        title: '取消订单',
        content: '确定取消该订单吗？取消后库存将恢复。',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelBuyerOrder(this.orderId)
              uni.showToast({ title: '已取消', icon: 'success' })
              this.loadDetail()
            } catch (e) {
              uni.showToast({ title: '取消失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-order-detail {
  background: #f7f8fa;
  min-height: 100vh;
}

/* 状态头部 */
.status-header {
  display: flex;
  align-items: center;
  padding: 28rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.status-header__text {
  font-size: 24rpx;
  color: #888;
  margin-left: 16rpx;
}

/* 详情卡片 */
.detail-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.detail-card__title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20rpx;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10rpx 0;
}
.info-row__label {
  font-size: 24rpx;
  color: #aaa;
  flex-shrink: 0;
  width: 140rpx;
}
.info-row__value {
  font-size: 24rpx;
  color: #333;
  text-align: right;
  flex: 1;
  word-break: break-all;
}
.goods-line {
  display: flex;
  align-items: center;
  padding: 10rpx 0;
  border-bottom: 1rpx solid #f8f8f8;
  &:last-child { border-bottom: none; }
}
.goods-line__name { flex: 1; font-size: 26rpx; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-line__qty { font-size: 24rpx; color: #aaa; margin: 0 16rpx; }
.goods-line__amount { font-size: 26rpx; color: #333; font-weight: 600; }
.goods-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  margin-top: 8rpx;
  border-top: 1rpx solid #f0f0f0;
}
.goods-total__label { font-size: 28rpx; color: #1a1a1a; font-weight: 600; }
.goods-total__price { font-size: 36rpx; color: #ff4d4f; font-weight: 700; }

/* 操作区 */
.action-area {
  margin-top: 8rpx;
}
.btn-pay-action {
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
  background: #2979ff;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 42rpx;
  text-align: center;
  border: none;
  letter-spacing: 2rpx;
}
.btn-pay-action--hover { opacity: 0.85; }
.btn-cancel-action {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #fff;
  color: #e43d33;
  font-size: 28rpx;
  border-radius: 40rpx;
  text-align: center;
  border: 1rpx solid #fde2e0;
  margin-top: 16rpx;
}
.btn-cancel-action--hover { background: #fff5f5; }
</style>
