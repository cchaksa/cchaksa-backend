const KAKAO_SDK_URL = 'https://t1.kakaocdn.net/kakao_js_sdk/2.8.3/kakao.min.js'
const KAKAO_SDK_INTEGRITY =
  'sha384-oroumrnFVE0xtgqyDZJARgERibXg2C28380uaUZz2kHDS5CR7tu20eGiOU6GkTpy'

interface KakaoSdk {
  init(appKey: string): void
  isInitialized(): boolean
  Auth: {
    authorize(settings: {
      redirectUri: string
      nonce: string
      state: string
    }): void
    getAppKey(): string
  }
}

interface KakaoAuthorizationOptions {
  javascriptAppKey: string
  redirectUri: string
  nonce: string
  state: string
}

declare global {
  interface Window {
    Kakao?: KakaoSdk
  }
}

let sdkPromise: Promise<KakaoSdk> | null = null

function getLoadedSdk() {
  return window.Kakao ?? null
}

function loadKakaoSdk() {
  const loadedSdk = getLoadedSdk()
  if (loadedSdk) return Promise.resolve(loadedSdk)
  if (sdkPromise) return sdkPromise

  sdkPromise = new Promise<KakaoSdk>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = KAKAO_SDK_URL
    script.integrity = KAKAO_SDK_INTEGRITY
    script.crossOrigin = 'anonymous'
    script.onload = () => {
      const sdk = getLoadedSdk()
      if (sdk) {
        resolve(sdk)
        return
      }
      sdkPromise = null
      reject(new Error('카카오 로그인 모듈을 초기화하지 못했습니다.'))
    }
    script.onerror = () => {
      script.remove()
      sdkPromise = null
      reject(new Error('카카오 로그인 모듈을 불러오지 못했습니다.'))
    }
    document.head.append(script)
  })

  return sdkPromise
}

export async function authorizeWithKakao({
  javascriptAppKey,
  redirectUri,
  nonce,
  state,
}: KakaoAuthorizationOptions) {
  const kakao = await loadKakaoSdk()
  if (!kakao.isInitialized()) {
    kakao.init(javascriptAppKey)
  } else if (kakao.Auth.getAppKey() !== javascriptAppKey) {
    throw new Error('카카오 로그인 앱 설정이 일치하지 않습니다.')
  }

  kakao.Auth.authorize({ redirectUri, nonce, state })
}
