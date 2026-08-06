<script setup>
import { ref, reactive, computed } from 'vue'
import moment from 'moment'
import transactionApi from '@/api/transactionApi'
import categoryApi from '@/api/categoryApi'
import goalApi from '@/api/goalApi'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const goHome = () => router.push({ name: 'home' })

const authStore = useAuthStore()
const userId = computed(() => authStore.userId)

const yearMonth = ref(moment().format('YYYY-MM'))

// 선택된 카테고리 목록 (빈 배열 = 전체, 복수 선택 가능)
const selectedCategoryIds = ref([])

const summary = reactive({
  totalAmount: 0,
  categories: [],
  remainder: null,
})

const transactions = ref([])
const allCategories = ref([]) // 13개 전체 (필터칩/변경 바텀시트 순서 구성용)
const goalCategoryIds = ref([]) // 이번 달 목표로 설정한 카테고리 id 목록 (앞쪽에 배치)

// 카테고리 이름 -> FontAwesome 아이콘 클래스
const CATEGORY_ICONS = {
  식비: 'fa-utensils',
  '카페/간식': 'fa-mug-hot',
  온라인쇼핑: 'fa-cart-shopping',
  '패션/쇼핑': 'fa-shirt',
  '문화/여가': 'fa-film',
  '술/유흥': 'fa-martini-glass',
  교통: 'fa-bus',
  생활: 'fa-house',
  뷰티: 'fa-wand-magic-sparkles',
  교육: 'fa-book',
  반려동물: 'fa-paw',
  여행: 'fa-plane',
  기타: 'fa-ellipsis',
}
const iconOf = (name) => CATEGORY_ICONS[name] || 'fa-tag'

// KB스타뱅킹 참고 캡처 기준 카테고리별 고유 색상
const CATEGORY_COLORS = {
  식비: '#4a90d9',
  '카페/간식': '#8b5e3c',
  온라인쇼핑: '#26a69a',
  '패션/쇼핑': '#ec6d9c',
  '문화/여가': '#8e6fce',
  '술/유흥': '#f0a830',
  교통: '#5cb85c',
  생활: '#5b9bd5',
  뷰티: '#e05a9e',
  교육: '#5a6472',
  반려동물: '#a97c50',
  여행: '#4a90d9',
  기타: '#4a90d9',
}
const colorOf = (name) => CATEGORY_COLORS[name] || '#127f5f'

// ===== 데이터 로딩 =====
const loadSummary = async () => {
  try {
    const data = await transactionApi.getSummary({ userId: userId.value, yearMonth: yearMonth.value })
    summary.totalAmount = data.totalAmount
    summary.categories = data.categories
    summary.remainder = data.remainder
  } catch (e) {
    console.error('요약 조회 실패', e)
  }
}

const loadList = async () => {
  try {
    if (selectedCategoryIds.value.length === 0) {
      transactions.value = await transactionApi.getList({
        userId: userId.value,
        yearMonth: yearMonth.value,
      })
      return
    }
    const results = await Promise.all(
      selectedCategoryIds.value.map((categoryId) =>
        transactionApi.getList({ userId: userId.value, yearMonth: yearMonth.value, categoryId }),
      ),
    )
    transactions.value = results.flat().sort((a, b) => {
      if (a.txnDate !== b.txnDate) return a.txnDate < b.txnDate ? 1 : -1
      return b.txnId - a.txnId // 같은 날짜면 항상 이 기준(등록순)으로 고정 - 카테고리 클릭 순서와 무관하게 안정적
    })
  } catch (e) {
    console.error('거래내역 조회 실패', e)
  }
}

const loadCategories = async () => {
  try {
    allCategories.value = await categoryApi.getList()
  } catch (e) {
    console.error('카테고리 목록 조회 실패', e)
  }
}

// 로그인 전이라 401이 나는 게 지금은 정상 - 이땐 기본 순서로 대체
const loadGoalCategoryIds = async () => {
  try {
    const goals = await goalApi.getList({ yearMonth: yearMonth.value })
    goalCategoryIds.value = goals.map((g) => g.categoryId)
  } catch (e) {
    console.warn('목표 카테고리 조회 실패(로그인 전이면 정상) - 기본 순서로 대체', e)
    goalCategoryIds.value = []
  }
}

const load = async () => {
  await Promise.all([loadSummary(), loadList(), loadCategories(), loadGoalCategoryIds()])
}
load()

// ===== 날짜 이동 =====
const monthLabel = computed(() => moment(yearMonth.value, 'YYYY-MM').format('YYYY년 MM월'))

const changeMonth = (diff) => {
  yearMonth.value = moment(yearMonth.value, 'YYYY-MM').add(diff, 'months').format('YYYY-MM')
  load()
}
const prevMonth = () => changeMonth(-1)
const nextMonth = () => changeMonth(1)

// ===== 필터칩 =====
const chipCategories = computed(() => {
  const goalFirst = goalCategoryIds.value
    .map((id) => allCategories.value.find((c) => c.categoryId === id))
    .filter(Boolean)
  const rest = allCategories.value.filter((c) => !goalCategoryIds.value.includes(c.categoryId))
  return [...goalFirst, ...rest]
})

const selectAllCategories = () => {
  selectedCategoryIds.value = []
  loadList()
}

const toggleCategory = (categoryId) => {
  const idx = selectedCategoryIds.value.indexOf(categoryId)
  if (idx === -1) {
    selectedCategoryIds.value.push(categoryId)
  } else {
    selectedCategoryIds.value.splice(idx, 1)
  }
  loadList()
}

// ===== 날짜별 그룹핑 =====
const groupedByDate = computed(() => {
  const groups = {}
  for (const txn of transactions.value) {
    if (!groups[txn.txnDate]) {
      groups[txn.txnDate] = []
    }
    groups[txn.txnDate].push(txn)
  }
  return Object.keys(groups)
    .sort((a, b) => (a < b ? 1 : -1))
    .map((date) => ({
      date,
      dayOfWeek: moment(date).format('dd'),
      items: groups[date],
    }))
})

const formatAmount = (amount) => amount.toLocaleString('ko-KR') + '원'

// category_id가 NULL인 거래(아낄 수 없는 고정비)를 화면에 어떻게 표시할지
// 적금 상품명이면 "적금 납입", 그 외(월세 등)는 "고정비"로 구분
const SAVINGS_PRODUCT_NAMES = ['KB내맘대로적금', 'KB맑은안녕적금', 'KB 특★한 적금', 'KB국민프리미엄적금']
const categoryLabel = (txn) => {
  if (txn.categoryName) return txn.categoryName
  if (SAVINGS_PRODUCT_NAMES.includes(txn.merchantName)) return '적금 납입'
  return '고정비'
}

// 거래 하나에 맞는 아이콘/색 조합 (카테고리 있으면 그 카테고리 색, 없으면 적금/고정비용 별도 처리)
const iconForTxn = (txn) => {
  if (txn.categoryName) {
    return { icon: iconOf(txn.categoryName), color: colorOf(txn.categoryName) }
  }
  if (SAVINGS_PRODUCT_NAMES.includes(txn.merchantName)) {
    return { icon: 'fa-piggy-bank', color: '#127f5f' }
  }
  return { icon: 'fa-house', color: '#6c757d' }
}

// ===== 도넛 차트 (소비 구성) =====
const CHART_COLORS = ['#127f5f', '#fc9558', '#ffd239', '#e8512b', '#8ecae6']
const REMAINDER_COLOR = '#ced4da'
const RADIUS = 50
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

const donutSegments = computed(() => {
  const slices = summary.categories.map((cat, i) => ({
    label: cat.categoryName,
    amount: cat.amount,
    color: CHART_COLORS[i % CHART_COLORS.length],
  }))
  if (summary.remainder && summary.remainder.amount > 0) {
    slices.push({ label: summary.remainder.label, amount: summary.remainder.amount, color: REMAINDER_COLOR })
  }

  let cumulative = 0
  return slices.map((slice) => {
    const ratio = summary.totalAmount > 0 ? slice.amount / summary.totalAmount : 0
    const length = ratio * CIRCUMFERENCE
    const segment = { ...slice, ratio, length, offset: -cumulative }
    cumulative += length
    return segment
  })
})

// ===== 카테고리 변경 바텀시트 =====
const editingTxn = ref(null) // 지금 카테고리를 바꾸려는 거래 (없으면 시트 닫힘)
const isSheetOpen = computed(() => editingTxn.value !== null)

const openCategorySheet = (txn) => {
  editingTxn.value = txn
}
const closeCategorySheet = () => {
  editingTxn.value = null
}

const chooseCategory = async (categoryId) => {
  if (!editingTxn.value) return
  try {
    await transactionApi.updateCategory(editingTxn.value.txnId, userId.value, categoryId)
    closeCategorySheet()
    await Promise.all([loadSummary(), loadList()]) // 카테고리 바뀌었으니 요약/목록 다시 불러옴
  } catch (e) {
    console.error('카테고리 수정 실패', e)
    alert('카테고리 수정에 실패했어요. (수입/고정비 거래는 수정할 수 없어요)')
  }
}
</script>

<template>
  <div style="padding: 20px 20px 100px">
    <div class="d-flex align-items-center mb-3">
  <button type="button" class="btn btn-sm p-0" @click="goHome" style="width: 24px">
    <i class="fa-solid fa-chevron-left" style="color: #495057; font-size: 18px"></i>
  </button>
  <h1 class="h5 mb-0 fw-bold flex-grow-1 text-center">지출 상세 내역</h1>
  <div style="width: 24px"></div>
</div>

    <!-- 날짜 이동 -->
    <div class="d-flex align-items-center justify-content-center gap-3 mb-3">
      <button type="button" class="btn btn-sm btn-light" @click="prevMonth">
        <i class="fa-solid fa-chevron-left" style="color: #495057;"></i>
      </button>
      <span class="fw-semibold">{{ monthLabel }}</span>
      <button type="button" class="btn btn-sm btn-light" @click="nextMonth">
        <i class="fa-solid fa-chevron-right" style="color: #495057;"></i>
      </button>
    </div>

    <!-- 소비 구성 (Top5 + 나머지, 도넛 차트) -->
    <div class="card mb-3">
      <div class="card-body">
        <h2 class="h6 text-secondary mb-3 fw-bold">소비 구성</h2>
        <div class="d-flex align-items-center gap-4">
          <div class="position-relative flex-shrink-0" style="width: 120px; height: 120px">
            <svg width="120" height="120" viewBox="0 0 120 120">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#f0f0f0" stroke-width="16" />
              <circle
                v-for="seg in donutSegments"
                :key="seg.label"
                cx="60"
                cy="60"
                r="50"
                fill="none"
                :stroke="seg.color"
                stroke-width="16"
                :stroke-dasharray="`${seg.length} ${CIRCUMFERENCE - seg.length}`"
                :stroke-dashoffset="seg.offset"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div
              class="position-absolute top-50 start-50 translate-middle text-center"
              style="width: 90px"
            >
              <div class="small text-secondary">총 지출</div>
              <div class="fw-bold small">{{ formatAmount(summary.totalAmount) }}</div>
            </div>
          </div>

          <div class="flex-grow-1">
            <div
              v-for="seg in donutSegments"
              :key="seg.label"
              class="d-flex justify-content-between align-items-center small mb-2"
            >
              <span class="fw-semibold">
                <span
                  class="me-1"
                  :style="{
                    display: 'inline-block',
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: seg.color,
                  }"
                ></span>
                {{ seg.label }}
              </span>
              <span class="fw-semibold">{{ formatAmount(seg.amount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 카테고리 필터칩: 전체 + 13개 (목표 설정한 카테고리가 앞쪽) -->
    <div class="d-flex flex-wrap gap-2 mb-4">
      <button
        type="button"
        class="btn btn-sm bg-white"
        :class="selectedCategoryIds.length === 0 ? 'fw-semibold' : 'fw-normal text-secondary'"
        :style="{
          border: selectedCategoryIds.length === 0 ? '2px solid #ffd239' : '1px solid #dee2e6',
        }"
        @click="selectAllCategories"
      >
        전체
      </button>
      <button
        v-for="cat in chipCategories"
        :key="cat.categoryId"
        type="button"
        class="btn btn-sm bg-white d-inline-flex align-items-center gap-1"
        :class="selectedCategoryIds.includes(cat.categoryId) ? 'fw-semibold' : 'fw-normal text-secondary'"
        :style="{
          border: selectedCategoryIds.includes(cat.categoryId) ? '2px solid #ffd239' : '1px solid #dee2e6',
        }"
        @click="toggleCategory(cat.categoryId)"
      >
        <i
          class="fa-solid"
          :class="iconOf(cat.name)"
          :style="{ color: selectedCategoryIds.includes(cat.categoryId) ? colorOf(cat.name) : '#adb5bd' }"
        ></i>
        {{ cat.name }}
      </button>
    </div>

    <!-- 날짜별 거래내역 -->
    <div v-for="group in groupedByDate" :key="group.date" class="mb-3">
      <div class="text-secondary small mb-1">
        {{ moment(group.date).format('MM월 DD일') }} ({{ group.dayOfWeek }})
      </div>
      <ul class="list-group">
        <li
          v-for="txn in group.items"
          :key="txn.txnId"
          class="list-group-item d-flex align-items-center gap-3"
        >
          <div
            class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
            style="width: 36px; height: 36px; background-color: #f0f7f5"
          >
            <i class="fa-solid" :class="iconForTxn(txn).icon" :style="{ color: iconForTxn(txn).color }"></i>
          </div>
          <div class="flex-grow-1">
            <div class="fw-semibold">{{ txn.merchantName }}</div>
            <div class="text-secondary small d-flex align-items-center gap-2">
              <span>{{ txn.categoryName || categoryLabel(txn) }}</span>
              <button
                type="button"
                class="btn btn-link btn-sm p-0 text-decoration-none"
                @click="openCategorySheet(txn)"
              >
                변경
              </button>
            </div>
          </div>
          <div class="text-danger fw-semibold">-{{ formatAmount(txn.amount) }}</div>
        </li>
      </ul>
    </div>

    <p v-if="transactions.length === 0" class="text-secondary text-center mt-5">
      해당 월의 거래내역이 없어요.
    </p>

    <!-- 카테고리 변경 바텀시트 -->
    <div
      v-if="isSheetOpen"
      class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-end justify-content-center"
      style="background: rgba(0, 0, 0, 0.45); z-index: 1050"
      @click.self="closeCategorySheet"
    >
      <div class="bg-white w-100 p-3" style="max-width: 480px; border-radius: 20px 20px 0 0">
        <div class="mx-auto mb-3" style="width: 36px; height: 4px; background: #ccc; border-radius: 2px"></div>
        <p class="text-center fw-semibold mb-1">카테고리 변경</p>
        <p class="text-center text-secondary small mb-3">
          {{ editingTxn?.merchantName }} · -{{ formatAmount(editingTxn?.amount ?? 0) }}
        </p>
        <div class="row row-cols-2 g-2">
          <div class="col-12">
            <button
              type="button"
              class="btn w-100 d-flex align-items-center gap-2"
              :class="editingTxn?.categoryId === null ? 'btn-outline-dark' : 'btn-light'"
              @click="chooseCategory(null)"
            >
              <i class="fa-solid fa-ban" style="color: #6c757d;"></i>
              미지정
            </button>
          </div>
          <div v-for="cat in allCategories" :key="cat.categoryId" class="col">
            <button
              type="button"
              class="btn w-100 d-flex align-items-center gap-2"
              :class="editingTxn?.categoryId === cat.categoryId ? 'btn-outline-dark' : 'btn-light'"
              @click="chooseCategory(cat.categoryId)"
            >
              <i
                class="fa-solid"
                :class="iconOf(cat.name)"
                :style="{ color: editingTxn?.categoryId === cat.categoryId ? colorOf(cat.name) : '#adb5bd' }"
              ></i>
              {{ cat.name }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>