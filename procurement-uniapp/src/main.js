import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import shareMixin from './mixins/share'
import { fileUrl } from './api/request'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.mixin(shareMixin)
  app.config.globalProperties.$fileUrl = fileUrl
  return {
    app
  }
}
