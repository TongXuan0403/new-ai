import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api'

const TOKEN_KEY = 'codex-ai-token'
const USER_KEY = 'codex-ai-user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.userInfo && Number(state.userInfo.userType) === 2,
    displayName: (state) => state.userInfo?.displayName || state.userInfo?.nickname || state.userInfo?.username || '同学'
  },
  actions: {
    async login(payload) {
      const data = await apiLogin(payload)
      this.token = data.token
      this.userInfo = data.userInfo
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.userInfo))
      return data
    },
    async register(payload) {
      const data = await apiRegister(payload)
      this.token = data.token
      this.userInfo = data.userInfo
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.userInfo))
      return data
    },
    async logout() {
      try {
        await apiLogout()
      } catch (e) {
        // 忽略退出接口失败，本地清理
      }
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
