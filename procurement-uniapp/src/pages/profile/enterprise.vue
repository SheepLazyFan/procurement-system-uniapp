<template>
  <view class="page-enterprise">
    <NavBar title="企业中心" />

    <!-- 企业名称 Hero 区 -->
    <view class="hero-enterprise animate-fade-up">
      <text class="hero-title">{{ form.name || '未设置企业名称' }}</text>
      <text class="native-tag">{{ isSeller ? '企业管理者' : '企业成员' }}</text>
    </view>

    <!-- 档案表单区 -->
    <view class="form-container animate-fade-up" style="animation-delay: 0.1s;">
      <view class="section-header">
        <text class="card-title">基础档案</text>
        <view v-if="hasFullAccess" class="edit-btn-minimal" @tap="toggleEdit" hover-class="edit-hover">
          <text class="edit-text">{{ isEdit ? '保存' : '修改' }}</text>
        </view>
      </view>

      <view class="flip-wrapper" :class="{ 'is-flipped': isEdit }">
        <view class="flip-inner">
          <!-- 展示态 (Front) -->
          <view class="info-body certificate-mode flip-front">
            <view class="form-row">
              <text class="form-label">企业名称</text>
              <text class="form-value">{{ form.name || '尚未填写' }}</text>
            </view>
            <view class="form-divider"></view>
            <view class="form-row">
              <text class="form-label">联系人</text>
              <text class="form-value">{{ form.contactName || '尚未填写' }}</text>
            </view>
            <view class="form-divider"></view>
            <view class="form-row">
              <text class="form-label">联系电话</text>
              <text class="form-value num-font">{{ form.contactPhone || '尚未填写' }}</text>
            </view>
            <view class="form-divider"></view>
            <view class="form-row">
              <text class="form-label">企业地址</text>
              <text class="form-value">{{ form.address || '尚未填写' }}</text>
            </view>
          </view>
          
          <!-- 编辑态 (Back) -->
          <view class="info-body edit-mode flip-back">
            <view class="form-row">
              <text class="form-label">企业名称</text>
              <view class="saas-input"><input class="input-control" v-model="form.name" placeholder="请输入企业名称" /></view>
            </view>
            <view class="form-divider"></view>
            <view class="form-row">
              <text class="form-label">联系人</text>
              <view class="saas-input"><input class="input-control" v-model="form.contactName" placeholder="请输入联系人" /></view>
            </view>
            <view class="form-divider"></view>
            <view class="form-row">
              <text class="form-label">联系电话</text>
              <view class="saas-input"><input class="input-control" type="number" v-model="form.contactPhone" placeholder="请输入联系电话" /></view>
            </view>
            <view class="form-divider"></view>
            <view class="form-row">
              <text class="form-label">企业地址</text>
              <view class="saas-input"><input class="input-control" v-model="form.address" placeholder="请输入详细地址" /></view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 收款二维码与邀请码 (专属管理) -->
    <view class="saas-card admin-card animate-fade-up" style="animation-delay: 0.2s;" v-if="isSeller">
      <view class="admin-section">
        <view class="section-header">
          <text class="section-title">收款二维码</text>
          <text class="section-subtitle">买家扫描付款</text>
        </view>
        <view class="qr-upload-box" hover-class="box-hover" @tap="handleUploadPaymentQr">
          <image v-if="form.paymentQrUrl" :src="$fileUrl(form.paymentQrUrl)" class="qr-preview" mode="aspectFit" />
          <view v-else class="qr-empty">
            <text class="qr-empty-icon">➕</text>
            <text class="qr-empty-text">上传微信/支付宝收款码</text>
          </view>
        </view>
      </view>

      <view class="card-divider"></view>

      <view class="admin-section" v-if="enterpriseInfo.inviteCode">
        <view class="section-header">
          <text class="section-title">内部邀请码</text>
          <text class="section-subtitle">用于员工加入企业</text>
        </view>
        <view class="invite-code-box">
          <text class="invite-code num-font">{{ enterpriseInfo.inviteCode }}</text>
          <view class="invite-actions">
            <view class="action-tag bg-blue" @tap="handleCopyCode"><text class="text-blue">复制</text></view>
            <view class="action-tag bg-orange" @tap="handleRefreshCode"><text class="text-orange">刷新</text></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 团队成员简影 -->
    <view class="saas-card team-card animate-fade-up" style="animation-delay: 0.3s;">
      <view class="section-header">
        <text class="card-title">团队成员 <text class="count-tag">{{ members.length }} 人</text></text>
      </view>
      <view class="team-list">
        <view class="team-item" v-for="item in members" :key="item.id">
          <view class="member-avatar">
            <text class="avatar-letter">{{ (item.nickName || item.phone || '?').slice(0,1) }}</text>
          </view>
          <view class="member-info">
            <text class="member-name">{{ item.nickName || '未设置' }}</text>
            <text class="member-phone num-font">{{ item.phone || '未绑定手机号' }}</text>
          </view>
          <view class="member-status">
            <text class="role-badge" :class="'role-bg-' + (item.role || '').toLowerCase()">{{ roleText(item.role) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="safe-bottom-space"></view>

    <!-- 底部悬浮舱 (仅编辑态可见) -->
    <view class="bottom-action-bar animate-fade-up" v-if="hasFullAccess && isEdit">
      <button class="action-btn action-btn--primary" hover-class="action-btn--active" @tap="handleSave" :disabled="saving">
        <text v-if="saving" class="btn-icon animate-spin">⏳</text>
        <text class="btn-text">{{ saving ? '保存中...' : '保存更改' }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import { useEnterpriseStore } from '@/store/enterprise'
import { useUserStore } from '@/store/user'
import { getTeamMembers } from '@/api/enterprise'
import { chooseAndUploadImages } from '@/api/upload'

const ROLE_MAP = { SELLER: '店主', ADMIN: '管理员', SALES: '销售员', WAREHOUSE: '仓管员' }

export default {
  components: { NavBar },
  data() {
    return {
      form: { name: '', contactName: '', contactPhone: '', address: '', paymentQrUrl: '' },
      members: [],
      saving: false, isEdit: false
    }
  },
  computed: {
    enterpriseInfo() {
      return useEnterpriseStore().info || {}
    },
    isSeller() {
      return useUserStore().isSeller
    },
    hasFullAccess() {
      return useUserStore().hasFullAccess
    }
  },
  onShow() {
    this.loadData()
  },
  methods: {
    roleText(role) {
      return ROLE_MAP[role] || role
    },
    async loadData() {
      const store = useEnterpriseStore()
      await store.fetchEnterprise()
      if (store.info) {
        this.form = {
          name: store.info.name || '',
          contactName: store.info.contactName || '',
          contactPhone: store.info.contactPhone || '',
          address: store.info.address || '',
          paymentQrUrl: store.info.paymentQrUrl || ''
        }
      }
      // 加载团队成员
      try {
        const res = await getTeamMembers()
        this.members = res || []
      } catch (e) {
        this.members = []
      }
    },
    async toggleEdit() {
      if (this.isEdit) {
        // 处于编辑模式，点击保存按钮将执行保存操作
        await this.handleSave();
      } else {
        // 处于展示模式，点击修改将激活翻牌动画并进入编辑框
        this.isEdit = true;
      }
    },
    async handleSave() {
      this.saving = true;
      if (!this.form.name) return uni.showToast({ title: '请输入企业名称', icon: 'none' })
      try {
        await useEnterpriseStore().updateEnterprise(this.form)
        uni.showToast({ title: '保存成功' })
        this.isEdit = false
      } catch (e) {
        uni.showToast({ title: e.message || '保存失败', icon: 'none' })
      } finally {
        this.saving = false;
      }
    },
    async handleUploadPaymentQr() {
      try {
        const urls = await chooseAndUploadImages(1, 'qrcode')
        if (urls && urls.length > 0) {
          this.form.paymentQrUrl = urls[0]
          // 立即保存二维码，无需等用户点保存按钮
          await useEnterpriseStore().updateEnterprise(this.form)
          uni.showToast({ title: '收款码已更新' })
        }
      } catch (e) {
        if (e) uni.showToast({ title: '上传失败', icon: 'none' })
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
    },
    handleCopyCode() {
      const code = this.enterpriseInfo.inviteCode
      if (!code) return
      uni.setClipboardData({
        data: code,
        success: () => {
          uni.showToast({ title: '邀请码已复制' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-enterprise {
  min-height: 100vh; background: var(--bg-page); padding: 24rpx; padding-bottom: 200rpx;
}

/* =======================================
   1. 企业认证 Hero 区
   ======================================= */
.hero-enterprise {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 80rpx 0 60rpx;
}
.hero-title { font-size: 48rpx; font-weight: 800; color: var(--text-primary); margin-bottom: 16rpx; text-align: center; letter-spacing: 2rpx; }
.native-tag {
  display: inline-block;
  background: transparent; color: var(--text-secondary);
  font-size: 26rpx; font-weight: 500; padding: 4rpx 16rpx; border-radius: 32rpx;
}

/* =======================================
   2. 基础信息卡片
   ======================================= */
.form-container { padding: 0 40rpx 60rpx; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32rpx; }
.card-title { font-size: 36rpx; font-weight: 800; color: var(--text-primary); letter-spacing: 1rpx; }
.edit-btn-minimal { display: flex; align-items: center; padding: 8rpx 16rpx; transition: opacity 0.2s; }
.edit-hover { opacity: 0.5; }
.edit-text { font-size: 28rpx; color: var(--brand-primary); font-weight: 600; letter-spacing: 1rpx; }

.flip-wrapper { perspective: 1000px; width: 100%; }
.flip-inner {
  position: relative; width: 100%; transition: transform 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.2);
  transform-style: preserve-3d;
}
.flip-wrapper.is-flipped .flip-inner { transform: rotateY(180deg); }

.flip-front, .flip-back {
  width: 100%; backface-visibility: hidden; box-sizing: border-box;
  border-radius: var(--radius-lg); padding: 24rpx 32rpx;
}

.flip-wrapper:not(.is-flipped) .flip-front { position: relative; }
.flip-wrapper:not(.is-flipped) .flip-back { position: absolute; top: 0; left: 0; opacity: 0; pointer-events: none; }
.flip-wrapper.is-flipped .flip-front { position: absolute; top: 0; left: 0; opacity: 0; pointer-events: none; }
.flip-wrapper.is-flipped .flip-back { position: relative; }

.flip-back { transform: rotateY(180deg); background: transparent; padding: 0; }

.certificate-mode {
  background: #ffffff;
  border: 2rpx solid transparent;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.02);
  position: relative; overflow: hidden;
}

.form-row { display: flex; flex-direction: column; gap: 12rpx; padding: 24rpx 0; position: relative; z-index: 2; }
.edit-mode .form-row { padding: 16rpx 0; }
.form-label { font-size: 24rpx; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 2rpx; }
.form-value { font-size: 32rpx; font-weight: 700; color: var(--text-primary); }
.form-divider { height: 1rpx; background: rgba(0,0,0,0.04); margin: 0; position: relative; z-index: 2; }

.saas-input { 
  width: 100%; height: 96rpx; background: rgba(41, 121, 255, 0.04); border-radius: 20rpx;
  display: flex; align-items: center; padding: 0 24rpx; box-sizing: border-box;
  border: 2rpx dashed rgba(41, 121, 255, 0.2); transition: all 0.3s cubic-bezier(0.25, 1.25, 0.2, 1);
}
.saas-input:hover { background: rgba(41, 121, 255, 0.08); border-color: rgba(41, 121, 255, 0.4); border-style: solid; }
.input-control { width: 100%; height: 100%; font-size: 30rpx; font-weight: 600; color: var(--brand-primary); }
.saas-input:focus-within { background: #ffffff; border-color: var(--brand-primary); border-style: solid; box-shadow: 0 4rpx 16rpx rgba(41, 121, 255, 0.15); }

/* =======================================
   3. 管理专属卡片 (QR & Invite)
   ======================================= */
.admin-card { padding: 0; }
.admin-section { padding: 32rpx; }
.section-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24rpx; }
.section-title { font-size: 30rpx; font-weight: 700; color: var(--text-primary); }
.section-subtitle { font-size: 24rpx; color: var(--text-tertiary); }

.qr-upload-box {
  width: 100%; height: 280rpx; border-radius: var(--radius-lg);
  border: 2rpx dashed var(--border-light); background: #f8f9fc;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
  transition: all 0.2s;
}
.box-hover { background: #f0f2f5; border-color: var(--brand-primary); }
.qr-preview { width: 240rpx; height: 240rpx; }
.qr-empty { display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.qr-empty-icon { font-size: 48rpx; opacity: 0.6; }
.qr-empty-text { font-size: 26rpx; color: var(--text-secondary); font-weight: 600; }

.card-divider { height: 1rpx; background: var(--border-light); margin: 0 32rpx; }

.invite-code-box {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--bg-page); padding: 24rpx 32rpx; border-radius: var(--radius-md); border: 1rpx solid var(--border-light);
}
.invite-code { font-size: 44rpx; font-weight: 800; color: var(--brand-primary); letter-spacing: 4rpx; }
.invite-actions { display: flex; gap: 16rpx; }
.action-tag { padding: 8rpx 20rpx; border-radius: 30rpx; }
.bg-blue { background: rgba(41, 121, 255, 0.1); }
.bg-orange { background: rgba(255, 152, 0, 0.1); }
.text-blue { font-size: 24rpx; font-weight: 600; color: var(--brand-primary); }
.text-orange { font-size: 24rpx; font-weight: 600; color: #ff9800; }

/* =======================================
   4. 团队成员小卡
   ======================================= */
.count-tag { font-size: 24rpx; color: var(--brand-primary); background: rgba(41, 121, 255, 0.1); padding: 4rpx 16rpx; border-radius: 20rpx; margin-left: 12rpx; }
.team-list { display: flex; flex-direction: column; }
.team-item { display: flex; align-items: center; padding: 32rpx 0; border-bottom: 1rpx solid rgba(0,0,0,0.04); }
.team-item:last-child { border-bottom: none; padding-bottom: 0; }
.member-avatar { width: 88rpx; height: 88rpx; border-radius: 50%; background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%); display: flex; align-items: center; justify-content: center; margin-right: 24rpx; }
.avatar-letter { font-size: 36rpx; font-weight: 800; color: #1e3a8a; }
.member-info { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.member-name { font-size: 32rpx; font-weight: 800; color: var(--text-primary); letter-spacing: 1rpx; }
.member-phone { font-size: 26rpx; font-weight: 600; color: var(--text-tertiary); }

.member-status { display: flex; align-items: center; }
.role-badge { font-size: 20rpx; font-weight: 700; padding: 6rpx 16rpx; border-radius: 30rpx; text-transform: uppercase; letter-spacing: 1rpx; }
.role-bg-seller { background: rgba(255, 152, 0, 0.1); color: #ff9800; }
.role-bg-admin { background: rgba(41, 121, 255, 0.1); color: var(--brand-primary); }
.role-bg-sales { background: rgba(24, 188, 55, 0.1); color: var(--color-success); }
.role-bg-warehouse { background: rgba(244, 67, 54, 0.1); color: var(--color-danger); }

</style>
