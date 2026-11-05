import { Capacitor } from '@capacitor/core'
import {
  AdMob,
  BannerAdSize,
  BannerAdPosition,
  BannerAdPluginEvents,
} from '@capacitor-community/admob'

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

// Bottom safe-area inset in dp. The app runs edge-to-edge (see MainActivity:
// setDecorFitsSystemWindows(window,false)), so the native banner is anchored to
// the *window* bottom and would otherwise sit under the system navigation bar.
// The AdMob plugin multiplies `margin` by the device density, so we measure the
// CSS env inset in px and convert it back to dp here.
function bottomInsetDp() {
  try {
    const probe = document.createElement('div')
    probe.style.cssText =
      'position:fixed;left:0;bottom:0;width:0;visibility:hidden;' +
      'height:env(safe-area-inset-bottom,0px);'
    document.body.appendChild(probe)
    const px = probe.getBoundingClientRect().height || 0
    document.body.removeChild(probe)
    const dpr = window.devicePixelRatio || 1
    return Math.round(px / dpr)
  } catch (e) {
    return 0
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
      margin: bottomInsetDp(),
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

// Subscribe to the native banner's size changes. The event payload carries the
// height in dp (AdSize units), so we multiply by devicePixelRatio to get the CSS
// pixel height the web layer must reserve. Returns an unsubscribe function.
export function onBannerSize(cb) {
  if (!isNative()) return () => {}
  const dpr = window.devicePixelRatio || 1
  const handle = AdMob.addListener(BannerAdPluginEvents.SizeChanged, (info) => {
    try {
      const px = Math.max(0, Math.round(((info && info.height) || 0) * dpr))
      cb(px)
    } catch (e) {
      /* ignore */
    }
  })
  return () => {
    try {
      handle.then((h) => h && h.remove())
    } catch (e) {
      /* ignore */
    }
  }
}

export { isNative as adsAvailable }
