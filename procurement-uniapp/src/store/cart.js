/**
 * 购物车状态管理（买家端采购清单）
 */
import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', {
  state: () => ({
    /** 当前商家企业 ID */
    enterpriseId: null,
    /** 购物车商品列表 */
    items: []
    // item: { productId, name, spec, unit, price, quantity, image }
  }),

  getters: {
    /** 购物车商品总数 */
    totalCount: (state) =>
      state.items.reduce((sum, item) => sum + item.quantity, 0),

    /** 购物车总金额 */
    totalAmount: (state) =>
      state.items.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0),

    /** 购物车是否为空 */
    isEmpty: (state) => state.items.length === 0
  },

  actions: {
    /**
     * 设置当前商家（切换商家时清空购物车）
     */
    setEnterprise(enterpriseId) {
      if (this.enterpriseId !== enterpriseId) {
        this.enterpriseId = enterpriseId
        this.items = []
        this._save()
      }
    },

    /**
     * 添加商品到购物车
     */
    addItem(product) {
      const existing = this.items.find(i => i.productId === product.productId)
      if (existing) {
        existing.quantity += (product.quantity || 1)
      } else {
        this.items.push({
          productId: product.productId,
          name: product.name,
          spec: product.spec || '',
          unit: product.unit || '',
          price: product.price,
          quantity: product.quantity || 1,
          image: product.image || ''
        })
      }
      this._save()
    },

    /**
     * 更新商品数量
     */
    updateQuantity(productId, quantity) {
      const item = this.items.find(i => i.productId === productId)
      if (item) {
        if (quantity <= 0) {
          this.removeItem(productId)
        } else {
          item.quantity = quantity
          this._save()
        }
      }
    },

    /**
     * 移除商品
     */
    removeItem(productId) {
      this.items = this.items.filter(i => i.productId !== productId)
      this._save()
    },

    /**
     * 清空购物车
     */
    clearCart() {
      this.items = []
      this.enterpriseId = null
      this._save()
    },

    /**
     * 从本地缓存恢复
     */
    restoreFromStorage() {
      try {
        const data = uni.getStorageSync('cart')
        if (data) {
          const parsed = JSON.parse(data)
          this.enterpriseId = parsed.enterpriseId || null
          this.items = parsed.items || []
        }
      } catch (e) {
        // ignore
      }
    },

    /** 内部：持久化到本地缓存 */
    _save() {
      uni.setStorageSync('cart', JSON.stringify({
        enterpriseId: this.enterpriseId,
        items: this.items
      }))
    }
  }
})
