import api from '@/api'

const BASE_URL = '/api/report'

export default {
  async get(params) {
    const { data } = await api.get(BASE_URL, { params })
    console.log('REPORT GET: ', data)
    return data
  },
}
