import DOMPurify from 'dompurify'

/**
 * HTML消毒工具 - 防止XSS攻击
 * 对AI返回的Markdown/HTML内容进行安全过滤
 */

// 允许的安全标签和属性
const ALLOWED_TAGS = [
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'p', 'br', 'hr',
  'ul', 'ol', 'li',
  'strong', 'em', 'b', 'i', 'u', 's', 'del', 'ins',
  'a', 'img',
  'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'blockquote', 'pre', 'code',
  'div', 'span',
  'input',
  // 引用图标标记（emoji 图标，无需 SVG 标签）
  'sup'
]

const ALLOWED_ATTR = [
  'href', 'src', 'alt', 'title', 'class', 'style',
  'target', 'rel',
  'width', 'height',
  'type', 'checked', 'disabled'
]

/**
 * 清理HTML内容，移除不安全的标签和属性
 */
export function sanitizeHtml(html: string): string {
  if (!html) return ''

  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: true,
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'form', 'textarea', 'select'],
    FORBID_ATTR: ['onerror', 'onclick', 'onload', 'onmouseover', 'onfocus', 'onblur']
  })
}

/**
 * 清理Markdown渲染后的HTML
 */
export function sanitizeMarkdown(markdownHtml: string): string {
  return sanitizeHtml(markdownHtml)
}
