import { getDifyDatasets } from '@/api'

let cachedIds: string[] = []

/**
 * 从后端获取案例库 dataset ID 列表（对应 application.yml 中 dify.dataset.visible-ids.case-audit）
 * 首次调用时请求后端，结果缓存避免重复请求
 */
export async function fetchCaseDatasetIds(): Promise<string[]> {
  if (cachedIds.length > 0) return cachedIds
  try {
    const res: any = await getDifyDatasets('case-audit')
    const items = res?.data?.items || res?.data || []
    cachedIds = items.map((d: any) => d.id).filter(Boolean)
  } catch (e) {
    console.warn('获取案例库 ID 失败，回退空列表', e)
    cachedIds = []
  }
  return cachedIds
}

/** 同步获取已缓存的案例库 ID，未加载时返回空数组 */
export function getCaseDatasetIds(): string[] {
  return cachedIds
}
