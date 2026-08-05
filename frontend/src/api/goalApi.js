import api from '@/api'

const BASE_URL = '/api/goals'

export default {
  // 주의: 이 API는 A팀이 이미 JWT 인증 필수로 막아둠 (SecurityConfig: /api/goals/** authenticated())
  // 로그인 기능 붙기 전까지는 401이 나는 게 정상 - 호출부에서 try/catch로 감싸서 써야 함
  async getList(params) {
    const { data } = await api.get(BASE_URL, { params })
    console.log('GOAL GET LIST: ', data)
    return data
  },
}
