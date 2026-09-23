import { marked } from 'marked'
import { sanitizeHtml } from './sanitize'

// 自定义 renderer：将内部文件路径代理到后端
const renderer = {
  image({ href, title, text }: { href?: string; title?: string | null; text?: string }): string | false {
    let src = href || ''
    if (src.startsWith('/files/') || src.startsWith('/api/files/')) {
      src = `/api/dify/knowledge/files/preview?path=${encodeURIComponent(src)}`
    }
    const alt = text || ''
    const titleAttr = title ? ` title="${title}"` : ''
    // 注：onerror 等事件属性会被 DOMPurify 移除，无需拼接；加载失败时由浏览器默认处理
    return `<img src="${src}" alt="${alt}"${titleAttr} style="max-width:100%;border-radius:6px;cursor:pointer;" />`
  }
}

// 配置 marked 使用自定义 renderer
marked.use({
  breaks: true,
  gfm: true,
  renderer
})

/**
 * 预处理 Markdown 文本：清理引用标记、规范化标题格式
 */
function preprocessMarkdown(text: string, closeCodeBlock: boolean): string {
  // 1. 去除完整的引用标签，如 [ref:knowledge:doc_id:xxx,seg_id:xxx]
  let processed = text.replace(/\[ref:[^\]]*\]/g, '')
  // 2. 去除流式传输中被截断的不完整引用标签，如 [ref:knowledge:doc_id:xxx
  processed = processed.replace(/\[ref:[^\]]*$/g, '')
  // 3. 规范化列表标记：
  //   a) 行首 *中文 → * 中文（Dify列表项有时省略空格，且仅限行首避免破坏行内 *斜体* 和 **粗体**）
  processed = processed.replace(/^([*\-])([\u4e00-\u9fff])/gm, '$1 $2')
  //   b) 标准列表标记（* 、- 、数字. ）前插入换行
  processed = processed.replace(/([^\n\s*_\-])([*-] |\d+\. |#{1,6} )/g, '$1\n$2')
  // 4. 规范化标题格式：确保 # 后面有空格
  processed = processed.replace(/^(#{1,6})([^\s#\n])/gm, '$1 $2')
  // 5. 处理不完整的代码块
  if (closeCodeBlock) {
    const codeBlockCount = (processed.match(/```/g) || [])
    if (codeBlockCount.length % 2 !== 0) {
      processed += '\n```'
    }
  }
  return processed
}

/**
 * 渲染 Markdown 文本，自动消毒HTML防止XSS
 * 用于完整内容（done 事件 / 刷新时从数据库加载）
 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    const processedText = preprocessMarkdown(text, true)
    const html = marked.parse(processedText) as string
    return sanitizeHtml(html)
  } catch {
    return sanitizeHtml(text)
  }
}

/**
 * 流式渲染 Markdown：不强制闭合不完整的代码块
 * 避免在流式输出过程中将后续标题等 Markdown 标记误包在 <code> 块内
 */
export function renderMarkdownStreaming(text: string): string {
  if (!text) return ''
  try {
    const processedText = preprocessMarkdown(text, false)
    const html = marked.parse(processedText) as string
    return sanitizeHtml(html)
  } catch {
    return sanitizeHtml(text)
  }
}
