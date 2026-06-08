import { readFileSync } from 'node:fs'
import { dirname } from 'node:path'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')

const checks = [
  {
    file: 'src/views/LoginView.vue',
    state: 'isCodeSending',
    guard: 'codeCountdown.value > 0 || isCodeSending.value',
    disabled: 'codeCountdown > 0 || isCodeSending',
    label: 'login code send button'
  },
  {
    file: 'src/views/RegisterView.vue',
    state: 'isCodeSending',
    guard: 'codeCountdown.value > 0 || isCodeSending.value',
    disabled: 'codeCountdown > 0 || isCodeSending',
    label: 'register code send button'
  },
  {
    file: 'src/views/ChangePasswordView.vue',
    state: 'isCodeSending',
    guard: 'codeCountdown.value > 0 || isCodeSending.value',
    disabled: 'codeCountdown > 0 || isCodeSending',
    label: 'change-password code send button'
  }
]

const failures = []

for (const check of checks) {
  const source = readFileSync(resolve(root, check.file), 'utf8')
  if (!source.includes(`const ${check.state} = ref(false)`)) {
    failures.push(`${check.label}: missing ${check.state} state`)
  }
  if (!source.includes(check.guard)) {
    failures.push(`${check.label}: missing in-flight/countdown guard`)
  }
  if (!source.includes(`:disabled="${check.disabled}"`)) {
    failures.push(`${check.label}: button is not disabled while sending`)
  }
}

if (failures.length > 0) {
  console.error(failures.join('\n'))
  process.exit(1)
}

console.log('Code send cooldown checks passed')
