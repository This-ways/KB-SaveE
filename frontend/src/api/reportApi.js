import api from '@/api'

const BASE_URL = '/api/report'

export default {
  async get(params) {
    const { data } = await api.get(BASE_URL, { params })
    console.log('REPORT GET: ', data)
    return data
  },

  // AI 요약 강제 새로고침 (10분에 1번 제한, 실패 시 에러 throw)
  async refreshAiSummary(params) {
    const { data } = await api.post(`${BASE_URL}/ai-summary/refresh`, null, { params })
    console.log('REPORT AI SUMMARY REFRESH: ', data)
    return data
  },
}
