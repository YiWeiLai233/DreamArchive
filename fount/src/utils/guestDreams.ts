import { saveDream, type DreamForm } from '@/api/dream'

export const GUEST_DREAMS_KEY = 'guest_dreams'
export const DEVICE_ID_KEY = 'device_id'

export interface GuestDream {
  id: string
  userId: number
  title?: string
  content: string
  emotion: string
  place?: string
  time?: string
  interpretation?: string | null
  createdAt: string
}

function createLocalId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function getDeviceId(): string {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY)
  if (!deviceId) {
    deviceId = createLocalId()
    localStorage.setItem(DEVICE_ID_KEY, deviceId)
  }
  return deviceId
}

export function loadGuestDreams(): GuestDream[] {
  const raw = localStorage.getItem(GUEST_DREAMS_KEY)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function persistGuestDreams(dreams: GuestDream[]) {
  if (dreams.length === 0) {
    localStorage.removeItem(GUEST_DREAMS_KEY)
    return
  }
  localStorage.setItem(GUEST_DREAMS_KEY, JSON.stringify(dreams))
}

export function saveGuestDream(dream: Omit<GuestDream, 'id' | 'userId' | 'createdAt'>): GuestDream {
  const dreams = loadGuestDreams()
  const newDream: GuestDream = {
    ...dream,
    id: createLocalId(),
    userId: 0,
    createdAt: new Date().toISOString(),
    interpretation: dream.interpretation || null
  }
  persistGuestDreams([...dreams, newDream])
  return newDream
}

export function updateGuestDreamInterpretation(id: string, interpretation: string | null) {
  const dreams = loadGuestDreams()
  const updated = dreams.map((dream) => (
    dream.id === id ? { ...dream, interpretation } : dream
  ))
  persistGuestDreams(updated)
}

export async function syncGuestDreams(userId: number) {
  const dreams = loadGuestDreams()
  if (dreams.length === 0) {
    return { synced: 0, failed: 0 }
  }

  const failedDreams: GuestDream[] = []
  let synced = 0

  for (const dream of dreams) {
    try {
      const dreamData: DreamForm = {
        userId,
        title: dream.title || '',
        content: dream.content,
        emotion: dream.emotion,
        place: dream.place || '未知',
        time: dream.time || '',
        interpretation: dream.interpretation || ''
      }
      const res = await saveDream(dreamData)
      if (res.data.code !== 200) {
        throw new Error(res.data.message || '同步失败')
      }
      synced += 1
    } catch (e) {
      console.error('同步游客梦境失败:', e)
      failedDreams.push(dream)
    }
  }

  persistGuestDreams(failedDreams)
  return { synced, failed: failedDreams.length }
}
