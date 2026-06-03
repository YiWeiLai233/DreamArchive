import assert from 'node:assert/strict'
import fs from 'node:fs'

const axiosSource = fs.readFileSync('src/api/axios.ts', 'utf8')
const storeSource = fs.readFileSync('src/stores/index.ts', 'utf8')
const adminApiSource = fs.readFileSync('src/api/admin.ts', 'utf8')

assert.match(axiosSource, /withCredentials:\s*true/, 'shared axios instance must send HttpOnly auth cookie')
assert.match(axiosSource, /xsrfCookieName:\s*'XSRF-TOKEN'/, 'shared axios instance must read XSRF-TOKEN cookie')
assert.match(axiosSource, /xsrfHeaderName:\s*'X-XSRF-TOKEN'/, 'shared axios instance must send X-XSRF-TOKEN header')
assert.doesNotMatch(axiosSource, /attachAuthHeader/, 'shared axios instance must not attach localStorage bearer token')

assert.doesNotMatch(storeSource, /authToken/, 'user store must not persist authToken in localStorage')
assert.doesNotMatch(storeSource, /\btoken\b/, 'user store must not expose token state')

assert.doesNotMatch(adminApiSource, /import axios from 'axios'/, 'admin API must use the shared axios instance')
assert.doesNotMatch(adminApiSource, /Authorization:\s*`Bearer/, 'admin API must not send bearer tokens from local state')
