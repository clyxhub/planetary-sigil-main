import { registerPlugin } from '@capacitor/core'

const PlanetaryAlarm = registerPlugin('PlanetaryAlarm', {
  web: () => ({
    async schedule(_opts) {
      console.warn('PlanetaryAlarm.schedule() called on web — no native alarm.')
      return { success: false }
    },
    async cancel(_opts) {
      return { success: false }
    },
    async hasExactAlarmPermission() {
      return { value: true }
    },
    async requestExactAlarmPermission() {
      return { value: true }
    },
    async hasNotificationPermission() {
      return { value: true }
    },
    async requestNotificationPermission() {
      if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
        await Notification.requestPermission()
      }
      return {
        value: typeof Notification !== 'undefined' && Notification.permission === 'granted',
      }
    },
    async saveFile(_opts) {
      console.warn('PlanetaryAlarm.saveFile() called on web — using browser download.')
      return { success: false }
    },
  }),
})

export { PlanetaryAlarm }
