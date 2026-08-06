import api from '@/api'

const BASE_URL = '/api/transactions'

export default {
  async getList(params) {
    const { data } = await api.get(BASE_URL, { params })
    console.log('TRANSACTION GET LIST: ', data)
    return data
  },

  async getSummary(params) {
    const { data } = await api.get(`${BASE_URL}/summary`, { params })
    console.log('TRANSACTION GET SUMMARY: ', data)
    return data
  },

  async updateCategory(txnId, userId, categoryId) {
    const { data } = await api.patch(
      `${BASE_URL}/${txnId}/category`,
      { categoryId },
      { params: { userId } },
    )
    console.log('TRANSACTION PATCH CATEGORY: ', data)
    return data
  },
}
