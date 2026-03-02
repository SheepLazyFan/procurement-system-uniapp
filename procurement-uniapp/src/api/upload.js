/**
 * 文件上传 API
 */
import { uploadFile } from './request'

/**
 * 上传图片（商品图片、企业 Logo 等）
 * @param {string} filePath - 本地临时文件路径
 * @param {string} bizType - 业务类型：product / logo / template
 * @returns {Promise<{url: string}>}
 */
export const uploadImage = (filePath, bizType = 'product') =>
  uploadFile(filePath, '/files/upload', { bizType })

/**
 * 从相册或相机选择并上传图片
 * @param {number} count - 最多选择数量
 * @param {string} bizType - 业务类型
 * @returns {Promise<string[]>} 图片 URL 数组
 */
export const chooseAndUploadImages = (count = 1, bizType = 'product') => {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        try {
          const urls = []
          for (const path of res.tempFilePaths) {
            const result = await uploadImage(path, bizType)
            urls.push(result.url)
          }
          resolve(urls)
        } catch (err) {
          reject(err)
        }
      },
      fail: (err) => {
        // 用户取消不报错
        if (err.errMsg?.includes('cancel')) return
        reject(err)
      }
    })
  })
}
