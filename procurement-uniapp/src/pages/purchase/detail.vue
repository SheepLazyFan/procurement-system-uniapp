<template>
  <view class="page-purchase-detail">
    <view class="saas-card animate-fade-up" style="animation-delay: 0s;">
      <view class="flex-between">
        <text class="order-no">{{ order.orderNo }}</text>
        <StatusTag :text="getStatusText(order.status)" :type="getStatusType(order.status)" />
      </view>
      <view class="divider" />
      <view class="info-row">
        <text class="info-label">供应商</text>
        <text class="info-value">{{ (order.supplier && order.supplier.name) || '-' }}</text>
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
    </view>

    <!-- 悬浮操作舱 -->
    <view class="bottom-action-bar safe-area-bottom animate-fade-up" style="animation-delay: 0.2s;" v-if="order.status">
      <!-- 草稿 / 采购中: 次极操作 + 主操作并排 -->
      <template v-if="['DRAFT','PURCHASING'].includes(order.status)">
        <button class="btn-ghost" @tap="handleAction('cancel')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">取消采购</button>
        <button v-if="order.status === 'DRAFT'" class="btn-primary" @tap="handleAction('purchasing')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">标记采购中</button>
        <button v-if="order.status === 'PURCHASING'" class="btn-primary" @tap="handleAction('arrive')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">标记到货</button>
      </template>

      <!-- 已到货: 单一主操作撑满 -->
      <button v-if="order.status === 'ARRIVED'" class="btn-primary btn-full" @tap="handleAction('complete')" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">完成采购</button>

      <!-- 已完成: 工具型操作撑满 -->
      <button v-if="order.status === 'COMPLETED'" class="btn-print btn-full" @tap="handlePrint" :disabled="printing" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
        <view class="btn-print__icon"></view>
        <text>{{ printing ? '打印中...' : '打印小票' }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import { getPurchaseOrderDetail, markPurchasing, markArrived, completePurchaseOrder, cancelPurchaseOrder } from '@/api/purchaseOrder'
import { getPurchaseStatusText, formatDateTime } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'
import { usePrinterStore } from '@/store/printer'
import { useEnterpriseStore } from '@/store/enterprise'
import { buildPurchaseReceipt } from '@/utils/receipt'

export default {
  components: { StatusTag },
  data() {
    return {
      orderId: null,
      order: { items: [] },
      printing: false
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
      } catch (e) {
        uni.showToast({ title: '加载订单失败', icon: 'none' })
      }
    },
    formatDateTime,
    getStatusText(status) {
      return getPurchaseStatusText(status)
    },
    getStatusType(status) {
      const map = { DRAFT: 'info', PURCHASING: 'primary', ARRIVED: 'warning', COMPLETED: 'success', CANCELLED: 'danger' }
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
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
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
        const buffer = buildPurchaseReceipt(this.order, enterpriseName)
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
.page-purchase-detail {
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

/* ================= 悬浮操作舱 ================= */
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
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  &::after { border: none; }
}

.btn-ghost {
  flex: 1;
  background: var(--bg-page);
  color: var(--text-secondary);
}

.btn-primary {
  flex: 2;
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
