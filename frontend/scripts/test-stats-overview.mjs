import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const viewPath = resolve('src/views/DreamStatsView.vue')
const source = readFileSync(viewPath, 'utf8')
const cards = source.split('<div class="overview-card')

if (cards.length < 3) {
  throw new Error('DreamStatsView overview cards were not found')
}

const totalCard = cards[1]
const todayCard = cards[2]

assertIncludes(totalCard, '{{ totalDreams }}', 'total card should still show totalDreams')
assertNotIncludes(totalCard, 'ov-trend', 'total card should not show the day-over-day trend badge')
assertNotIncludes(totalCard, '较昨日', 'total card should not carry the yesterday comparison label')

assertIncludes(todayCard, '{{ todayCount }}', 'today card should show todayCount')
assertIncludes(todayCard, 'ov-trend', 'today card should show the day-over-day trend badge')
assertIncludes(todayCard, '较昨日', 'today card should carry the yesterday comparison label')

function assertIncludes(value, expected, message) {
  if (!value.includes(expected)) {
    throw new Error(`${message}: missing ${expected}`)
  }
}

function assertNotIncludes(value, expected, message) {
  if (value.includes(expected)) {
    throw new Error(`${message}: found ${expected}`)
  }
}
