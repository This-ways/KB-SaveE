import api from '@/api'

const BASE_URL = '/api/categories'

export default {
  // 카테고리 전체 목록 (13종, categoryId 오름차순)
  async getList() {
    const { data } = await api.get(BASE_URL)
    console.log('CATEGORY GET LIST: ', data)
    return data
  },
}
