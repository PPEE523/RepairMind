import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref<number | null>(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const roleName = ref(localStorage.getItem('roleName') || '')
  const roleId = ref<number | null>(Number(localStorage.getItem('roleId')) || null)
  const passwordExpired = ref(false)

  function setLoginInfo(data: {
    token: string
    userId: number
    username: string
    nickname: string
    roleName?: string
    roleId?: number
    passwordExpired?: boolean
  }) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    roleName.value = data.roleName || ''
    roleId.value = data.roleId || null
    passwordExpired.value = data.passwordExpired || false
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    localStorage.setItem('nickname', data.nickname)
    if (data.roleName) localStorage.setItem('roleName', data.roleName)
    if (data.roleId) localStorage.setItem('roleId', String(data.roleId))
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    nickname.value = ''
    roleName.value = ''
    roleId.value = null
    passwordExpired.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    localStorage.removeItem('roleName')
    localStorage.removeItem('roleId')
    localStorage.removeItem('menus')
  }

  function isAdmin() {
    return roleId.value === 1
  }

  function canAudit() {
    // 管理员或资深检修工程师可以审核
    return roleId.value === 1 || roleId.value === 2
  }

  function updateProfile(data: { username: string; nickname: string }) {
    username.value = data.username
    nickname.value = data.nickname
    localStorage.setItem('username', data.username)
    localStorage.setItem('nickname', data.nickname)
  }

  return {
    token,
    userId,
    username,
    nickname,
    roleName,
    roleId,
    passwordExpired,
    setLoginInfo,
    logout,
    isAdmin,
    canAudit,
    updateProfile
  }
})
