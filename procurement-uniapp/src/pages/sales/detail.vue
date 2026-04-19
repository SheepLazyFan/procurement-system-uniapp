<template>
  <view class="page-sales-detail">
    <view class="saas-card animate-fade-up" style="animation-delay: 0s;">
      <view class="flex-between">
        <text class="order-no">{{ order.orderNo }}</text>
        <StatusTag :text="getStatusText(order.status, order.cancelBy)" :type="getStatusType(order.status)" />
      </view>
      <view class="divider" />
      <view class="info-row">
        <text class="info-label">客户</text>
        <text class="info-value">{{ (order.customer && order.customer.name) || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">支付状态</text>
        <text class="info-value">{{ order.paymentStatus === 'PAID' ? '已支付' : order.paymentStatus === 'CLAIMED' ? '待确认收款' : '未支付' }}</text>
      </view>
      <view class="info-row" v-if="order.deliveryAddress">
        <text class="info-label">收货地址</text>
        <text class="info-value">{{ order.deliveryAddress }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ formatDateTime(order.createdAt) }}</text>
      </view>
      <view class="info-row" v-if="order.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order.remark }}</text>
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0.1s;">
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
        <text class="total-amount num-font price-text">¥{{ order.totalAmount }}</text>
      </view>
      <view class="total-row flex-between" v-if="order.totalProfit != null && hasFullAccess" style="padding-top: 0;">
        <text class="total-label">利润</text>
        <text class="profit-amount num-font">¥{{ order.totalProfit }}</text>
      </view>
    </view>

    <!-- 悬浮操作舱 -->
    <view class="bottom-action-bar safe-area-bottom animate-fade-up" style="animation-delay: 0.2s;" v-if="order.status">
      
      <!-- 次要操作：取消订单 -->
      <button v-if="canCancel" class="btn-ghost" @tap="handleAction('cancel')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">取消订单</button>
      
      <!-- 财务操作：收款 -->
      <button v-if="(order.paymentStatus === 'UNPAID' || order.paymentStatus === 'CLAIMED') && order.status !== 'CANCELLED' && (hasFullAccess || isSales)" class="btn-secondary" @tap="handleAction('pay')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
        确认收款
      </button>

      <!-- 核心业务流：确认/发货/完成 -->
      <button v-if="order.status === 'PENDING' && (hasFullAccess || isSales)" class="btn-primary" @tap="handleAction('confirm')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">确认订单</button>
      <button v-if="order.status === 'CONFIRMED' && order.paymentStatus === 'PAID'" class="btn-primary" @tap="handleAction('ship')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">标记发货</button>
      <button v-if="order.status === 'SHIPPED' && order.paymentStatus === 'PAID'" class="btn-primary" @tap="handleAction('complete')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">完成订单</button>

      <!-- 售后：打印 -->
      <button v-if="order.status === 'COMPLETED'" class="btn-print btn-full" @tap="handlePrint" :disabled="printing" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
        <view class="btn-print__icon"></view>
        <text>{{ printing ? '打印中...' : '打印小票' }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import {
  getSalesOrderDetail,
  confirmSalesOrder,
  shipSalesOrder,
  completeSalesOrder,
  cancelSalesOrder,
  paySalesOrder
} from '@/api/salesOrder'
import { getSalesStatusText, formatDateTime } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'
import { usePrinterStore } from '@/store/printer'
import { useEnterpriseStore } from '@/store/enterprise'
import { buildSalesReceipt } from '@/utils/receipt'
import { useUserStore } from '@/store/user'

export default {
  components: { StatusTag },
  data() {
    return {
      orderId: null,
      order: { items: [] },
      printing: false
    }
  },
  computed: {
    hasFullAccess() { return useUserStore().hasFullAccess },
    isSales()       { return useUserStore().isSales },
    isWarehouse()   { return useUserStore().isWarehouse },
    canCancel() {
      const s = this.order.status
      if (this.hasFullAccess) return s === 'PENDING' || s === 'CONFIRMED'
      if (this.isSales)       return s === 'PENDING' || s === 'CONFIRMED'
      return false
    }
  },
  onLoad(query) {
    this.orderId = Number(query.id)
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        this.order = await getSalesOrderDetail(this.orderId)
      } catch (e) {
        uni.showToast({ title: '加载订单失败', icon: 'none' })
      }
    },
    formatDateTime,
    getStatusText(status, cancelBy) { return getSalesStatusText(status, cancelBy) },
    getStatusType(status) {
      const map = { PENDING: 'warning', CONFIRMED: 'primary', SHIPPED: 'purple', COMPLETED: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    async handleAction(action) {
      if (action === 'cancel') {
        const content = this.order.paymentStatus === 'PAID'
          ? '该订单已确认收款，取消前请先与买家沟通并完成线下退款，确认继续取消？'
          : this.order.paymentStatus === 'CLAIMED'
            ? '买家已提交付款声明，请先核对收款情况并与买家沟通退款后再取消，确认继续？'
            : '确认取消此订单？'
        const confirmed = await new Promise(resolve =>
          uni.showModal({ title: '取消订单', content, success: res => resolve(res.confirm) })
        )
        if (!confirmed) return
      }
      const fns = {
        confirm: confirmSalesOrder,
        ship: shipSalesOrder,
        complete: completeSalesOrder,
        cancel: cancelSalesOrder,
        pay: paySalesOrder
      }
      try {
        await fns[action](this.orderId)
        uni.showToast({ title: '操作成功', icon: 'success' })
        this.loadDetail()
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    },
    async handlePrint() {
      const printerStore = usePrinterStore()
      if (!printerStore.connectedDevice) {
        uni.showModal({
          title: '未连接打印机',
          content: '请先在"我的-打印机管理"中连接蓝牙打印机',
          confirmText: '去连接',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/profile/printer' })
            }
          }
        })
        return
      }
      if (!printerStore.isConnected) {
        uni.showLoading({ title: '重连打印机...', mask: true })
        await printerStore.reconnect()
        uni.hideLoading()
        if (!printerStore.isConnected) {
          uni.showToast({ title: '打印机连接断开，请重新连接', icon: 'none' })
          return
        }
      }
      this.printing = true
      try {
        const enterpriseName = useEnterpriseStore().info?.name || ''
        const buffer = buildSalesReceipt(this.order, enterpriseName)
        await printerStore.print(buffer)
        uni.showToast({ title: '打印成功', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '打印失败', icon: 'none' })
      } finally {
        this.printing = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-sales-detail {
  padding: 24rpx;
  /* 底部预留悬浮舱高度，防止内容被遮挡 */
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.order-no {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-number);
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.info-label {
  font-size: 28rpx;
  color: var(--text-secondary);
}

.info-value {
  font-size: 28rpx;
  color: var(--text-primary);
  font-weight: 500;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid var(--border-light);
  &:last-child {
    border-bottom: none;
  }
}

.item-name {
  font-size: 28rpx;
  color: var(--text-primary);
  font-weight: 500;
  display: block;
  margin-bottom: 6rpx;
}

.item-spec {
  font-size: 24rpx;
  color: var(--text-tertiary);
}

.item-right {
  text-align: right;
}

.item-qty {
  font-size: 26rpx;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 6rpx;
}

.item-amount {
  font-size: 30rpx;
  color: var(--text-primary);
  font-weight: 600;
  font-family: var(--font-number);
}

.total-row {
  padding-top: 24rpx;
}

.total-label {
  font-size: 28rpx;
  color: var(--text-primary);
  font-weight: 600;
}

.total-amount {
  font-size: 40rpx;
  color: var(--color-danger);
  font-weight: 700;
}
.profit-amount {
  font-size: 36rpx;
  color: var(--color-success);
  font-weight: 700;
}

/* ================= 悬浮操作舱 ================= */
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);
  z-index: 100;
  box-sizing: border-box;
}

.bottom-action-bar button {
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
  &::after { border: none; }
}

.btn-ghost {
  flex: 1;
  background: var(--bg-page);
  color: var(--text-secondary);
}

.btn-secondary {
  flex: 1;
  background: var(--brand-primary-light);
  color: var(--brand-primary);
}

.btn-primary {
  flex: 1.5;
  background: var(--brand-primary);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.2);
}

.btn-full {
  flex: 1;
}

.btn-print {
  flex: 1;
  background: #f8fafc;
  color: var(--text-primary);
  border: 2rpx solid var(--border-light);
  font-weight: 500;
  
  &__icon {
    width: 32rpx;
    height: 32rpx;
    margin-right: 12rpx;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23333333' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 6 2 18 2 18 9'%3E%3C/polyline%3E%3Cpath d='M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2'%3E%3C/path%3E%3Crect x='6' y='14' width='12' height='8'%3E%3C/rect%3E%3C/svg%3E");
    background-size: cover;
  }
}
</style>
