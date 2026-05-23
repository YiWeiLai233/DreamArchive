export type DreamInterpretationBlock =
  | { type: 'heading'; content: string }
  | { type: 'paragraph'; content: string }
  | { type: 'item'; marker: string; content: string }

const headingWords = [
  '整体解读',
  '情绪层面',
  '情绪分析',
  '象征层面',
  '象征分析',
  '梦境象征',
  '现实启发',
  '现实层面',
  '现实关联',
  '心理启发',
  '可能意涵',
  '温柔提醒',
  '行动建议',
  '总结'
]

export function formatDreamInterpretation(raw?: string | null): DreamInterpretationBlock[] {
  if (!raw || !raw.trim()) {
    return []
  }

  let text = raw
    .replace(/\*\*/g, '')
    .replace(/[\[\]\u3010\u3011]/g, '')
    .replace(/\r\n/g, '\n')
    .replace(/\t/g, ' ')
    .replace(/[ \u00a0]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()

  for (const heading of headingWords) {
    text = text.replace(new RegExp(`\\s*${heading}\\s*[：:]?\\s*`, 'g'), `\n@@heading::${heading}\n`)
  }

  text = text
    .replace(/(^|\s)(\d{1,2})[.、]\s+/g, '\n@@item::$2::')
    .replace(/(^|\n)\s*[-•]\s+/g, '\n@@item::::')

  const blocks: DreamInterpretationBlock[] = []
  const lines = text.split(/\n+/).map(line => line.trim()).filter(Boolean)

  let itemCounter = 0
  for (const line of lines) {
    if (line.startsWith('@@heading::')) {
      const content = line.replace('@@heading::', '').trim()
      if (content) {
        blocks.push({ type: 'heading', content })
      }
      continue
    }

    if (line.startsWith('@@item::')) {
      const item = line.replace('@@item::', '')
      const separatorIndex = item.indexOf('::')
      const content = separatorIndex >= 0 ? item.slice(separatorIndex + 2).trim() : item.trim()
      if (content) {
        itemCounter++
        blocks.push({ type: 'item', marker: String(itemCounter), content })
      }
      continue
    }

    for (const paragraph of splitLongParagraph(line)) {
      blocks.push({ type: 'paragraph', content: paragraph })
    }
  }

  return blocks
}

function splitLongParagraph(text: string): string[] {
  if (text.length <= 140) {
    return [text]
  }

  const sentences = text.match(/[^。！？!?；;]+[。！？!?；;]?/g) || [text]
  const paragraphs: string[] = []
  let current = ''

  for (const sentence of sentences) {
    const next = current ? `${current}${sentence}` : sentence
    if (next.length > 140 && current) {
      paragraphs.push(current.trim())
      current = sentence
    } else {
      current = next
    }
  }

  if (current.trim()) {
    paragraphs.push(current.trim())
  }

  return paragraphs
}
