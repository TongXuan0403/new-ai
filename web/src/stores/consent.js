import { defineStore } from 'pinia'
import { getConsentStatus, submitConsent, revokeConsent } from '../api'

export const useConsentStore = defineStore('consent', {
  state: () => ({
    complete: false,
    ageConfirmed: false,
    privacyPolicyVersion: '',
    sensitiveInfoVersion: '',
    productBoundaryVersion: '',
    revoked: false,
    consentedAt: null,
    loadedAt: 0
  }),
  actions: {
    async load() {
      const data = await getConsentStatus()
      this.complete = Boolean(data.complete)
      this.ageConfirmed = Boolean(data.ageConfirmed)
      this.privacyPolicyVersion = data.privacyPolicyVersion
      this.sensitiveInfoVersion = data.sensitiveInfoVersion
      this.productBoundaryVersion = data.productBoundaryVersion
      this.revoked = Boolean(data.revoked)
      this.consentedAt = data.consentedAt
      this.loadedAt = Date.now()
    },
    async loadIfStale() {
      if (Date.now() - this.loadedAt > 30000) {
        await this.load()
      }
    },
    async submit(payload) {
      await submitConsent(payload)
      await this.load()
    },
    async revoke() {
      await revokeConsent()
      await this.load()
    }
  }
})
