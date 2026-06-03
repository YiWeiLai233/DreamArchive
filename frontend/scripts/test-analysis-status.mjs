import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import vm from 'node:vm'
import ts from 'typescript'

const sourcePath = path.resolve('src/utils/analysisStatus.ts')
const source = fs.readFileSync(sourcePath, 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020
  }
}).outputText

const sandbox = { exports: {}, module: { exports: {} } }
sandbox.exports = sandbox.module.exports
vm.runInNewContext(compiled, sandbox, { filename: sourcePath })

const {
  getAnalysisFields,
  isPendingAnalysis,
  needsAnalysis,
  toSyncableInterpretation
} = sandbox.module.exports

assert.equal(
  JSON.stringify(getAnalysisFields({ analysisStatus: 'PENDING', analysisError: 'queued' })),
  JSON.stringify({ analysisStatus: 'PENDING', analysisError: 'queued' })
)
assert.equal(isPendingAnalysis({ analysisStatus: 'PENDING', interpretation: '' }), true)
assert.equal(needsAnalysis({ analysisStatus: 'FAILED', interpretation: '' }), true)
assert.equal(needsAnalysis({ analysisStatus: 'SUCCESS', interpretation: '已解析' }), false)
assert.equal(toSyncableInterpretation('AI 解析失败，请稍后重试'), '')
assert.equal(toSyncableInterpretation('游客体验已用完，请注册登录后继续使用AI解析功能'), '')
assert.equal(toSyncableInterpretation('一段正常的 AI 解析'), '一段正常的 AI 解析')
