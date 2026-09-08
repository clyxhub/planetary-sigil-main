import { registerPlugin } from '@capacitor/core'

const PlanetaryAlarm = registerPlugin('PlanetaryAlarm', {
  web: () => ({
    async schedule(opts) {
      const { timestamp, planetName, leadMinutes = 0 } = opts
      const when = new Date(Number(timestamp)).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
      console.warn(
        `PlanetaryAlarm.schedule() called on web — scheduled for ${planetName} at ${when}${leadMinutes > 0 ? ` (reminder ${leadMinutes} min before)` : ''}. Replace with native APK to test real alarms.`
      )
      return { value: false }
    },
    async cancel(_opts) {
      console.warn('PlanetaryAlarm.cancel() called on web — no native alarm to cancel.')
      return { value: false }
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
        value:
          typeof Notification !== 'undefined' &&
          Notification.permission === 'granted',
      }
    },
    async saveMedia(_opts) {
      console.warn('PlanetaryAlarm.saveMedia() called on web — falling back to browser download.')
      return { success: false }
    },
    async saveSvg(_opts) {
      console.warn('PlanetaryAlarm.saveSvg() called on web — falling back to browser download.')
      return { success: false }
    },
  }),
})

export { PlanetaryAlarm }
