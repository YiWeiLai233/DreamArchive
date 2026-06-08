import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const viewPath = resolve(scriptDir, '../src/views/DreamListView.vue')
const source = readFileSync(viewPath, 'utf8')

function assertIncludes(snippet, message) {
  if (!source.includes(snippet)) {
    throw new Error(message)
  }
}

assertIncludes('const DREAMS_PAGE_SIZE = 9', 'DreamListView should cap each page to 9 dreams.')
assertIncludes('const currentPage = ref(1)', 'DreamListView should track the current dream page.')
assertIncludes('const totalDreams = ref(0)', 'DreamListView should track backend total dreams.')
assertIncludes('const paginatedDreams = computed', 'DreamListView should expose paginated dreams.')
assertIncludes('getUserDreamsPage', 'DreamListView should call the backend pagination API.')
assertIncludes('pageSize: DREAMS_PAGE_SIZE', 'DreamListView should pass page size to the backend.')
assertIncludes('filter: activeFilter.value', 'DreamListView should pass filters to the backend.')
assertIncludes('keyword: searchQuery.value.trim() || undefined', 'DreamListView should pass search text to the backend.')
assertIncludes('currentPage.value = pageData.page', 'DreamListView should trust the backend-clamped page.')
assertIncludes('totalDreams.value = pageData.total', 'DreamListView should use backend total count.')
assertIncludes('v-for="(dream, index) in paginatedDreams"', 'Dream cards should render only the current page.')
assertIncludes('class="pagination-bar glass"', 'DreamListView should render pagination controls.')
assertIncludes('watch([searchQuery, activeFilter], () => {', 'Search and filters should reset pagination.')

console.log('Dream list pagination checks passed.')
