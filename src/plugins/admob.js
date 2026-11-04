import { Capacitor } from '@capacitor/core'
import { AdMob, BannerAdSize, BannerAdPosition } from '@capacitor-community/admob'

// AdMob ids for the "Planetary Sigil" app.
const BANNER_AD_ID = 'ca-app-pub-4971705725374261/7089666188'

// Real ads only work on a native build (Capacitor Android). On the web/dev
// preview everything here is a no-op so the layout never breaks.
const isNative = () => Capacitor.isNativePlatform()

let initialized = false

export async function initAds() {
  if (!isNative() || initialized) return
  try {
    await AdMob.initialize()
    initialized = true
  } catch (e) {
    console.warn('AdMob init failed:', e)
  }
}

export async function showBanner({ testing = false } = {}) {
  if (!isNative()) return
  try {
    if (!initialized) await initAds()
    await AdMob.showBanner({
      adId: BANNER_AD_ID,
      adSize: BannerAdSize.ADAPTIVE_BANNER,
      position: BannerAdPosition.BOTTOM_CENTER,
      margin: 0,
      isTesting: testing,
    })
  } catch (e) {
    console.warn('showBanner failed:', e)
  }
}

export async function hideBanner() {
  if (!isNative()) return
  try {
    await AdMob.hideBanner()
  } catch (e) {
    /* ignore */
  }
}

export { isNative as adsAvailable }