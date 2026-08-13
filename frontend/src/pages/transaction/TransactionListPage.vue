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
const allMonthAmountByCategory = ref({}) // 목표만 보기 정확도용: 상위5 제한 없는 카테고리별 전체 합계
const allCategories = ref([]) // 13개 전체 (필터칩/변경 바텀시트 순서 구성용)
const goalCategoryIds = ref([]) // 이번 달 목표로 설정한 카테고리 id 목록 (앞쪽에 배치)
const goals = ref([]) // 이번 달 목표 전체 (categoryId, targetAmount) - 도넛 범례에 목표 대비 표시용

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

// 더보기 관련 상태 - loadList()가 마운트 시 곧바로(동기적으로) 호출되므로
// loadList보다 반드시 앞에서 선언해야 함 (TDZ 에러로 loadList 전체가 조용히 죽는 버그가 있었음)
const INITIAL_TXN_LIMIT = 10
const showAllTransactions = ref(false)

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
    showAllTransactions.value = false // 목록을 새로 불러올 땐 항상 10건부터 다시 시작
    if (selectedCategoryIds.value.length === 0) {
      transactions.value = await transactionApi.getList({
        userId: userId.value,
        yearMonth: yearMonth.value,
      })
      // 목표만 보기 정확도용 전체 합계도 여기서 같이 계산 (동일 API를 별도로 또 호출하지 않음 -
      // loadAllMonthAmounts와 완전히 같은 요청을 동시에 두 번 날리면 레이스 컨디션으로
      // 초기 진입 시 목록이 빈 배열로 보이는 문제가 있었음)
      const map = {}
      transactions.value.forEach((t) => {
        if (t.categoryId) {
          map[t.categoryId] = (map[t.categoryId] || 0) + t.amount
        }
      })
      allMonthAmountByCategory.value = map
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
    // 카테고리 필터가 걸려있을 땐 전체 합계를 위해 필터 없는 전체 목록을 별도로 조회
    await loadAllMonthAmounts()
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

// 요약 API는 상위 5개만 주기 때문에("나머지"로 뭉침), "목표만 보기"에서
// 순위 밖 카테고리가 0원으로 잘못 나오는 걸 막기 위해 이번 달 전체 거래를 따로 합산
const loadAllMonthAmounts = async () => {
  try {
    const all = await transactionApi.getList({ userId: userId.value, yearMonth: yearMonth.value })
    const map = {}
    all.forEach((t) => {
      if (t.categoryId) {
        map[t.categoryId] = (map[t.categoryId] || 0) + t.amount
      }
    })
    allMonthAmountByCategory.value = map
  } catch (e) {
    console.error('카테고리별 전체 합계 조회 실패', e)
    allMonthAmountByCategory.value = {}
  }
}

// 이제 로그인 기능이 실제로 동작하므로, 정상적으로 목표 카테고리를 가져와야 함
// (실패하면 여전히 기본 순서로 대체 - 목표를 아예 안 세운 유저도 있을 수 있으니 방어는 유지)
const loadGoalCategoryIds = async () => {
  try {
    const myGoals = await goalApi.getMyGoals(yearMonth.value)
    goals.value = myGoals
    goalCategoryIds.value = myGoals.map((g) => g.categoryId)
  } catch (e) {
    console.warn('목표 카테고리 조회 실패 - 기본 순서로 대체', e)
    goals.value = []
    goalCategoryIds.value = []
  }
}

const load = async () => {
  // loadList()가 필터 없는 상태(초기 진입 시 항상 그러함)에서는
  // allMonthAmountByCategory도 같이 채워주므로 여기서 별도로 또 부르지 않음
  await Promise.all([loadSummary(), loadList(), loadCategories(), loadGoalCategoryIds()])
}
load()

// ===== 날짜 이동 =====
const MIN_YEAR_MONTH = '2026-01' // 서비스 데이터가 존재하는 가장 이른 달 - 이보다 이전으로는 못 감

const monthLabel = computed(() => moment(yearMonth.value, 'YYYY-MM').format('YYYY년 MM월'))
const isAtMinMonth = computed(() => yearMonth.value === MIN_YEAR_MONTH)
const isAtMaxMonth = computed(() => yearMonth.value === moment().format('YYYY-MM')) // 이번 달보다 미래로는 못 감

const changeMonth = (diff) => {
  yearMonth.value = moment(yearMonth.value, 'YYYY-MM').add(diff, 'months').format('YYYY-MM')
  load()
}
const prevMonth = () => {
  if (isAtMinMonth.value) return
  changeMonth(-1)
}
const nextMonth = () => {
  if (isAtMaxMonth.value) return
  changeMonth(1)
}

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
      dayOfWeek: moment(date).format('ddd'),
      items: groups[date],
    }))
})

// ===== 더보기 (초기 10건만 표시) =====
const visibleGroupedByDate = computed(() => {
  if (showAllTransactions.value) return groupedByDate.value

  const result = []
  let count = 0
  for (const group of groupedByDate.value) {
    if (count >= INITIAL_TXN_LIMIT) break
    const items = group.items.slice(0, INITIAL_TXN_LIMIT - count)
    result.push({ ...group, items })
    count += items.length
  }
  return result
})

const hasMoreTransactions = computed(
  () => !showAllTransactions.value && transactions.value.length > INITIAL_TXN_LIMIT,
)

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

// ===== 카테고리별 목표 대비 (기존 "카테고리별 지출" 화면과 통합) =====
const goalMap = computed(() =>
  goals.value.reduce((acc, g) => {
    acc[g.categoryId] = g.targetAmount
    return acc
  }, {}),
)

const barColor = (rate) => {
  if (rate == null) return '#e5e7eb'
  if (rate >= 90) return '#ef4444'
  if (rate >= 70) return '#f97316'
  return '#ffbc00'
}

// ===== 소비 구성 (가로 스택 바) =====
// 요약 API가 이미 상위 5개 + 나머지로 정리해서 주기 때문에 그대로 사용
const CHART_COLORS = ['#127f5f', '#fc9558', '#ffd239', '#e8512b', '#8ecae6']
const REMAINDER_COLOR = '#ced4da'

const donutSegments = computed(() => {
  const slices = summary.categories.map((cat, i) => {
    const target = goalMap.value[cat.categoryId] ?? null
    const rate =
      target && target > 0 ? Math.min(Math.round((cat.amount / target) * 100), 100) : null
    return {
      categoryId: cat.categoryId,
      label: cat.categoryName,
      amount: cat.amount,
      color: CHART_COLORS[i % CHART_COLORS.length],
      target,
      rate,
    }
  })

  if (summary.remainder && summary.remainder.amount > 0) {
    slices.push({
      categoryId: null,
      label: summary.remainder.label,
      amount: summary.remainder.amount,
      color: REMAINDER_COLOR,
      target: null,
      rate: null,
    })
  }

  return slices.map((slice) => ({
    ...slice,
    ratio: summary.totalAmount > 0 ? slice.amount / summary.totalAmount : 0,
  }))
})

// 목표만 보기: 상위5 제한 없는 allMonthAmountByCategory 기준이라 순위 밖 카테고리도 정확히 나옴
const goalOnlySegments = computed(() => {
  return goals.value.map((g, i) => {
    const amount = allMonthAmountByCategory.value[g.categoryId] || 0
    const rate =
      g.targetAmount > 0 ? Math.min(Math.round((amount / g.targetAmount) * 100), 100) : null
    const catInfo = allCategories.value.find((c) => c.categoryId === g.categoryId)
    return {
      categoryId: g.categoryId,
      label: catInfo?.name || '카테고리',
      amount,
      color: CHART_COLORS[i % CHART_COLORS.length],
      target: g.targetAmount,
      rate,
      ratio: summary.totalAmount > 0 ? amount / summary.totalAmount : 0,
    }
  })
})

// 체크하면 목표 설정한 카테고리만 소비구성에 표시
const showGoalOnly = ref(false)
const displaySegments = computed(() =>
  showGoalOnly.value ? goalOnlySegments.value : donutSegments.value,
)

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
    await Promise.all([loadSummary(), loadList()]) // 카테고리 바뀌었으니 요약/목록 다시 불러옴 (전체합계는 loadList 안에서 같이 처리됨)
  } catch (e) {
    console.error('카테고리 수정 실패', e)
    alert('카테고리 수정에 실패했어요. (수입/고정비 거래는 수정할 수 없어요)')
  }
}
</script>

<template>
  <div style="padding: 76px 20px 100px; background-color: #f8f9fa; min-height: 100vh">
    <div
      class="d-flex align-items-center"
      style="
        gap: 12px;
        position: fixed;
        top: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 100%;
        max-width: 420px;
        z-index: 100;
        background-color: #f8f9fa;
        padding: 16px 20px;
      "
    >
  <button type="button" class="btn btn-sm p-0" @click="goHome" style="width: 24px">
    <i class="fa-solid fa-chevron-left" style="color: #495057; font-size: 18px"></i>
  </button>
  <h1 style="font-size: 17px; font-weight: 700; margin: 0">지출 상세 내역</h1>
</div>

    <!-- 날짜 이동 -->
    <div class="d-flex align-items-center justify-content-center gap-3 mb-3">
      <button type="button" class="btn btn-sm btn-light" :disabled="isAtMinMonth" @click="prevMonth">
        <i class="fa-solid fa-chevron-left" :style="{ color: isAtMinMonth ? '#ced4da' : '#495057' }"></i>
      </button>
      <span class="fw-semibold">{{ monthLabel }}</span>
      <button type="button" class="btn btn-sm btn-light" :disabled="isAtMaxMonth" @click="nextMonth">
        <i class="fa-solid fa-chevron-right" :style="{ color: isAtMaxMonth ? '#ced4da' : '#495057' }"></i>
      </button>
    </div>

    <!-- 소비 구성 (Top5 + 나머지, 가로 스택 바) -->
    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <h2 class="h6 text-secondary mb-0 fw-bold">소비 구성</h2>
          <button
            type="button"
            class="goal-only-btn"
            :class="{ active: showGoalOnly }"
            @click="showGoalOnly = !showGoalOnly"
          >
            <i
              class="fa-solid"
              :class="showGoalOnly ? 'fa-check' : 'fa-filter'"
            ></i>
            목표만 보기
          </button>
        </div>
        <div class="fw-bold mb-2" style="font-size: 1.3rem">
          {{ formatAmount(summary.totalAmount) }}
        </div>

        <!-- 카테고리 비율을 색깔별로 이어붙인 막대 -->
        <div class="stack-bar mb-3">
          <div
            v-for="seg in displaySegments"
            :key="seg.label"
            class="stack-seg"
            :style="{ width: seg.ratio * 100 + '%', backgroundColor: seg.color }"
          ></div>
        </div>

        <div class="flex-grow-1">
          <div
            v-for="seg in displaySegments"
            :key="seg.label"
            class="legend-row"
            >
              <div class="d-flex justify-content-between align-items-center small">
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

              <!-- 목표가 실제로 설정된 카테고리만 진행률 바 표시. 없으면 문구만, 빈 바 트랙 안 그림 -->
              <template v-if="seg.categoryId">
                <template v-if="seg.target">
                  <div class="legend-bar-bg mt-1">
                    <div
                      class="legend-bar"
                      :style="{ width: (seg.rate ?? 0) + '%', backgroundColor: barColor(seg.rate) }"
                    ></div>
                  </div>
                  <span class="legend-target">
                    목표 {{ formatAmount(seg.target) }} · {{ seg.rate }}% 사용
                  </span>
                </template>
                <span v-else class="legend-target no-goal">목표 미설정</span>
              </template>
            </div>
          </div>
          <p
            v-if="showGoalOnly && displaySegments.length === 0"
            class="text-secondary small mb-0"
          >
            목표를 설정한 카테고리가 없어요.
          </p>
        </div>
      </div>

    <!-- 카테고리 필터칩: 전체 + 13개 (목표 설정한 카테고리가 앞쪽), 한 줄 + 가로 슬라이드 -->
    <div class="position-relative mb-4">
      <div class="chip-scroll d-flex flex-nowrap gap-2">
      <button
        type="button"
        class="btn btn-sm bg-white flex-shrink-0"
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
        class="btn btn-sm bg-white d-inline-flex align-items-center gap-1 flex-shrink-0"
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
      <div class="chip-fade"></div>
    </div>

    <!-- 날짜별 거래내역 -->
    <div v-for="group in visibleGroupedByDate" :key="group.date" class="mb-3">
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

    <!-- 더보기 -->
    <button
      v-if="hasMoreTransactions"
      type="button"
      class="btn w-100 mb-3 border-0 shadow-sm rounded-4 bg-white"
      style="padding: 10px"
      @click="showAllTransactions = true"
    >
      더보기 <i class="fa-solid fa-chevron-down small ms-1" style="color: #495057"></i>
    </button>

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

<style scoped>
/* 소비 구성 - 가로 스택 바 (도넛 대체) */
.stack-bar {
  display: flex;
  width: 100%;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: #f1f2f4;
}
.stack-seg {
  height: 100%;
}
.goal-only-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid #dee2e6;
  background: #fff;
  color: #6b7280;
  font-size: 11px;
  font-weight: 600;
}
.goal-only-btn i {
  font-size: 10px;
}
.goal-only-btn.active {
  border: 2px solid #ffd239;
  background: #fffbea;
  color: #111;
}

/* 목표 대비 진행률 (기존 카테고리별 지출 화면에서 가져옴) */
.legend-row {
  margin-bottom: 10px;
}
.legend-row:last-child {
  margin-bottom: 0;
}
.legend-bar-bg {
  height: 5px;
  border-radius: 999px;
  background: #f1f2f4;
  overflow: hidden;
}
.legend-bar {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}
.legend-target {
  font-size: 11px;
  color: #9ca3af;
}
.legend-target.no-goal {
  color: #d1d5db;
}

/* 필터칩 가로 슬라이드 - 넘치는 칩은 좌우로 스와이프, 스크롤바는 안 보이게 */
.chip-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE, Edge */
}
.chip-scroll::-webkit-scrollbar {
  display: none; /* Chrome, Safari */
}

/* 오른쪽 끝 그라데이션 페이드 - 더 스크롤할 칩이 있다는 걸 은근히 암시 */
.chip-fade {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 32px;
  background: linear-gradient(to right, rgba(248, 249, 250, 0), rgba(248, 249, 250, 1));
  pointer-events: none; /* 페이드 위에 있어도 칩 클릭이 그대로 통과되게 */
}
</style>