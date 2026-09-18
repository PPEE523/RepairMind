import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { encrypt, decrypt } from './crypto'

// 加密开关：通过环境变量控制，默认关闭
const CRYPTO_ENABLED = import.meta.env.VITE_CRYPTO_ENABLED === 'true'

// 请求不需要加密的URL路径（与后端 CryptoProperties.excludePaths 对齐）
const CRYPTO_EXCLUDE_PATHS = [
  '/auth/',
  '/upload',
  '/documents/upload',
  '/documents/download',
  '/search',
  '/chat/',
  '/audio-to-text'
]

/**
 * 判断请求是否需要加密
 * 仅控制请求加密，不控制响应解密
 */
function shouldEncryptRequest(url?: string): boolean {
  if (!CRYPTO_ENABLED || !url) return false
  return !CRYPTO_EXCLUDE_PATHS.some(path => url.includes(path))
}

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 对于非 GET 请求，自动添加 Content-Type（FormData 由 axios 自动处理）
    if (config.data && config.method !== 'get' && !(config.data instanceof FormData)) {
      if (!config.headers?.['Content-Type']) {
        config.headers = config.headers || {}
        config.headers['Content-Type'] = 'application/json'
      }
    }

    // 加密请求体（仅POST/PUT/PATCH/DELETE且有body）
    if (shouldEncryptRequest(config.url) && config.data && config.method !== 'get') {
      const isMultipart = config.data instanceof FormData
      if (!isMultipart) {
        try {
          const plaintext = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
          const encrypted = encrypt(plaintext)
          config.data = JSON.stringify({ encrypted })
        } catch (e) {
          console.warn('请求加密失败，使用原始数据:', e)
        }
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    let data = response.data

    // 响应解密：只要数据中包含 encrypted 字段就解密（与URL无关）
    if (CRYPTO_ENABLED && data?.encrypted) {
      try {
        const decrypted = decrypt(data.encrypted)
        data = JSON.parse(decrypted)
        response.data = data
      } catch (e) {
        console.warn('响应解密失败，使用原始数据:', e)
      }
    }

    if (data.code === 200) {
      return data
    } else if (data.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(data)
    } else if (data.code === 403) {
      ElMessage.error('权限不足')
      return Promise.reject(data)
    } else if (data.code === 429) {
      ElMessage.error('请求频率过高，请稍后再试')
      return Promise.reject(data)
    } else {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(data)
    }
  },
  (error) => {
    if (error.response?.status === 429) {
      ElMessage.error('请求频率过高，请稍后再试')
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
