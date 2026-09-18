import CryptoJS from 'crypto-js'

/**
 * AES-256-CBC 加解密工具
 * 与后端 CryptoUtil 对应
 * 加密格式：Base64(IV[16字节] + 密文)
 *
 * 密钥通过 VITE_CRYPTO_SECRET_KEY 环境变量配置
 * 未配置时使用默认密钥（仅限开发环境）
 */

const SECRET_KEY = import.meta.env.VITE_CRYPTO_SECRET_KEY || 'dev-only-fallback-key-32b!'

/**
 * AES-256-CBC 加密
 * @param plaintext 明文
 * @returns Base64(IV + 密文)
 */
export function encrypt(plaintext: string): string {
  const key = CryptoJS.enc.Utf8.parse(SECRET_KEY)

  // 生成16字节随机IV
  const iv = CryptoJS.lib.WordArray.random(16)

  const encrypted = CryptoJS.AES.encrypt(plaintext, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })

  // IV + 密文拼接后Base64编码（与后端一致）
  const ivWords = iv.clone()
  const cipherWords = CryptoJS.enc.Base64.parse(encrypted.toString())
  const combined = ivWords.concat(cipherWords)

  return CryptoJS.enc.Base64.stringify(combined)
}

/**
 * AES-256-CBC 解密
 * @param ciphertext Base64(IV + 密文)
 * @returns 明文
 */
export function decrypt(ciphertext: string): string {
  const key = CryptoJS.enc.Utf8.parse(SECRET_KEY)

  // Base64解码，提取IV和密文
  const combined = CryptoJS.enc.Base64.parse(ciphertext)

  // 前16字节是IV
  const ivWords = combined.words.slice(0, 4)
  const iv = CryptoJS.lib.WordArray.create(ivWords, 16)

  // 剩余部分是密文
  const cipherWords = combined.words.slice(4)
  const cipherBytes = combined.sigBytes - 16
  const cipherData = CryptoJS.lib.WordArray.create(cipherWords, cipherBytes)

  const decrypted = CryptoJS.AES.decrypt(
    { ciphertext: cipherData } as any,
    key,
    {
      iv: iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )

  return decrypted.toString(CryptoJS.enc.Utf8)
}
