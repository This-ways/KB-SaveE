<script setup>
import { ref, computed } from 'vue'
import moment from 'moment'
import reportApi from '@/api/reportApi'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 마이데이터 미연결 상태(온보딩 이탈 등)에서 뒤로가기를 누르면
// 어중간하게 앱 안에 남기지 않고 로그아웃 후 로그인 화면으로 완전히 빠져나가게 한다.
const goHome = () => {
  if (!authStore.isMydataConnected) {
    authStore.logout()
    router.push('/auth/login')
    return
  }
  router.push({ name: 'home' })
}

const userId = computed(() => authStore.userId)

const yearMonth = ref(moment().format('YYYY-MM'))

const report = ref(null) // 이번 달 리포트
const prevSaveAmount = ref(null) // 전월 세이브 금액 (비교용)

const loadReport = async () => {
  try {
    report.value = await reportApi.get({ userId: userId.value, yearMonth: yearMonth.value })
  } catch (e) {
    console.error('리포트 조회 실패', e)
    return
  }

  // 전월 대비 비교용 - 전월 리포트도 같이 조회
  // 주의: /api/report는 AI 요약까지 같이 계산하는 API라, 이걸 호출하면
  // 전월 몫 AI 요약도 그 시점에 생성/캐시됨 (OpenAI 비용 관련 TODO: 필요시 세이브금액만 내려주는 전용 API로 분리)
  const prevYearMonth = moment(yearMonth.value, 'YYYY-MM').subtract(1, 'months').format('YYYY-MM')
  try {
    const prevReport = await reportApi.get({ userId: userId.value, yearMonth: prevYearMonth })
    prevSaveAmount.value = prevReport.saveAmount
  } catch (e) {
    console.warn('전월 리포트 조회 실패 - 전월 대비 비교 생략', e)
    prevSaveAmount.value = null
  }
}
loadReport()

// ===== 날짜 이동 =====
const MIN_YEAR_MONTH = '2026-01' // 서비스 데이터가 존재하는 가장 이른 달 - 이보다 이전으로는 못 감

const monthLabel = computed(() => moment(yearMonth.value, 'YYYY-MM').format('YYYY년 MM월'))
const prevMonthLabel = computed(() => moment(yearMonth.value, 'YYYY-MM').subtract(1, 'months').format('MM월'))
const isAtMinMonth = computed(() => yearMonth.value === MIN_YEAR_MONTH)
const isAtMaxMonth = computed(() => yearMonth.value === moment().format('YYYY-MM')) // 이번 달보다 미래로는 못 감

const changeMonth = (diff) => {
  yearMonth.value = moment(yearMonth.value, 'YYYY-MM').add(diff, 'months').format('YYYY-MM')
  loadReport()
}
const prevMonth = () => {
  if (isAtMinMonth.value) return
  changeMonth(-1)
}
const nextMonth = () => {
  if (isAtMaxMonth.value) return
  changeMonth(1)
}

const formatAmount = (amount) => {
  if (amount === null || amount === undefined) return '-'
  return amount.toLocaleString('ko-KR') + '원'
}
const formatSigned = (amount) => {
  if (amount === null || amount === undefined) return '-'
  const sign = amount > 0 ? '+' : ''
  return sign + amount.toLocaleString('ko-KR') + '원'
}
// "445000" -> "44.5만" 형태 (그래프 라벨용, 공간 절약)
const formatCompact = (amount) => {
  if (amount === null || amount === undefined) return '-'
  const sign = amount < 0 ? '-' : ''
  return sign + (Math.abs(amount) / 10000).toFixed(1) + '만'
}

// ===== 세이브 금액 전월 대비 =====
const saveDiff = computed(() => {
  if (!report.value) return null
  if (report.value.saveAmount === null || prevSaveAmount.value === null) return null
  return report.value.saveAmount - prevSaveAmount.value
})

// ===== 주차별 소비 막대그래프 =====
const maxWeekly = computed(() => {
  if (!report.value) return 1
  return Math.max(...report.value.weeklySpending.map((w) => w.amount), 1)
})
const maxWeeklyLabel = computed(() => {
  if (!report.value || report.value.weeklySpending.length === 0) return null
  const top = [...report.value.weeklySpending].sort((a, b) => b.amount - a.amount)[0]
  return top.weekNo
})

// 주차별 실제 일수 계산 (백엔드와 동일한 공식: CEIL((일 + 월시작요일-1) / 7))
// 1주차/마지막주차처럼 7일이 안 되는 주차를 찾아서 라벨에 표시하기 위함
const weekDayCounts = computed(() => {
  const counts = {}
  const monthStart = moment(yearMonth.value, 'YYYY-MM').startOf('month')
  const firstDow = monthStart.day() + 1 // moment: 0=일~6=토 -> SQL DAYOFWEEK 방식(1=일~7=토)으로 변환
  const daysInMonth = monthStart.daysInMonth()
  for (let day = 1; day <= daysInMonth; day++) {
    const weekNo = Math.ceil((day + firstDow - 1) / 7)
    counts[weekNo] = (counts[weekNo] || 0) + 1
  }
  return counts
})

// ===== 요일별 소비 막대그래프 =====
const maxWeekday = computed(() => {
  if (!report.value) return 1
  return Math.max(...report.value.weekdaySpending.map((w) => w.amount), 1)
})
const maxWeekdayLabel = computed(() => {
  if (!report.value || report.value.weekdaySpending.length === 0) return ''
  const top = [...report.value.weekdaySpending].sort((a, b) => b.amount - a.amount)[0]
  return top.dayOfWeek
})

// ===== 연령별 비교 (내 소비 vs 또래 평균, 공통 스케일) =====
const maxCompare = computed(() => {
  if (!report.value) return 1
  const values = report.value.topCategories.flatMap((c) => [c.myAmount, c.peerAmount ?? 0])
  return Math.max(...values, 1)
})

const MY_COLOR = '#ffd239' // 브랜드 메인 노랑 (또래 평균의 회색과 확실히 구분되게)
</script>

<template>
  <div style="padding: 20px 20px 100px; background-color: #f8f9fa; min-height: 100vh">
    <div class="d-flex align-items-center mb-3">
  <button type="button" class="btn btn-sm p-0" @click="goHome" style="width: 24px">
    <i class="fa-solid fa-chevron-left" style="color: #495057; font-size: 18px"></i>
  </button>
  <h1 class="h5 mb-0 fw-bold flex-grow-1 text-center">소비 리포트</h1>
  <div style="width: 24px"></div>
</div>

    <!-- 날짜 이동 -->
    <div class="d-flex align-items-center justify-content-center gap-3 mb-4">
      <button type="button" class="btn btn-sm btn-light" :disabled="isAtMinMonth" @click="prevMonth">
        <i class="fa-solid fa-chevron-left" :style="{ color: isAtMinMonth ? '#ced4da' : '#495057' }"></i>
      </button>
      <span class="fw-semibold">{{ monthLabel }}</span>
      <button type="button" class="btn btn-sm btn-light" :disabled="isAtMaxMonth" @click="nextMonth">
        <i class="fa-solid fa-chevron-right" :style="{ color: isAtMaxMonth ? '#ced4da' : '#495057' }"></i>
      </button>
    </div>

    <template v-if="report">
      <!-- 현금흐름 -->
      <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body">
          <h2 class="h6 text-secondary mb-3 fw-bold">이번 달 현금 흐름</h2>
          <div class="d-flex justify-content-between mb-1">
            <span class="text-secondary">전월 잔액</span>
            <span class="fw-semibold">{{ formatAmount(report.cashFlow.prevBalance) }}</span>
          </div>
          <div class="d-flex justify-content-between mb-1">
            <span class="text-secondary">수입</span>
            <span class="text-primary fw-semibold">+{{ formatAmount(report.cashFlow.income) }}</span>
          </div>
          <div class="d-flex justify-content-between mb-2">
            <span class="text-secondary">지출</span>
            <span class="text-danger fw-semibold">-{{ formatAmount(report.cashFlow.expense) }}</span>
          </div>
          <hr />
          <div class="d-flex justify-content-between fw-bold">
            <span>이달 잔액</span>
            <span>{{ formatAmount(report.cashFlow.currentBalance) }}</span>
          </div>
        </div>
      </div>

      <!-- 세이브 금액 / 납입 적금 -->
      <div class="row g-3 mb-3">
        <div class="col-6">
          <div class="card h-100 border-0 shadow-sm rounded-4 bg-white">
            <div class="card-body text-center">
              <div class="text-secondary small mb-1"><i class="fa-solid fa-piggy-bank" style="color: #fc9558;"></i> 이번 달 세이브</div>
              <div
                class="fw-bold"
                :style="{ color: report.saveAmount > 0 ? '#127f5f' : report.saveAmount < 0 ? '#e8512b' : '' }"
              >
                {{ formatSigned(report.saveAmount) }}
              </div>
              <div
                v-if="saveDiff !== null"
                class="small mt-1"
                :style="{ color: saveDiff >= 0 ? '#127f5f' : '#e8512b' }"
              >
                {{ prevMonthLabel }} 대비 {{ formatSigned(saveDiff) }}
              </div>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="card h-100 border-0 shadow-sm rounded-4 bg-white">
            <div class="card-body text-center">
              <div class="text-secondary small mb-1"><i class="fa-solid fa-coins" style="color: #fc9558;"></i> 이번 달 적금 납입</div>
              <div class="fw-bold">{{ formatAmount(report.savingPayment) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 주차별 소비 -->
      <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body">
          <h2 class="h6 text-secondary mb-3 fw-bold">주차별 소비</h2>
          <div class="d-flex align-items-end justify-content-between" style="height: 120px">
            <div
              v-for="week in report.weeklySpending"
              :key="week.weekNo"
              class="d-flex flex-column align-items-center flex-fill"
            >
              <div class="small text-secondary mb-1 fw-semibold">{{ formatCompact(week.amount) }}</div>
              <div
                class="rounded-top"
                style="width: 24px"
                :style="{
                  height: (week.amount / maxWeekly) * 70 + 'px',
                  backgroundColor: week.weekNo === maxWeeklyLabel ? '#ffc107' : '#ced4da',
                }"
              ></div>
              <div class="small text-secondary mt-1">
                {{ week.weekNo }}주{{ weekDayCounts[week.weekNo] !== 7 ? `(총 ${weekDayCounts[week.weekNo]}일)` : '' }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 요일별 소비 패턴 -->
      <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body">
          <h2 class="h6 text-secondary mb-1 fw-bold">요일별 소비 패턴</h2>
          <p class="small text-secondary mb-3" v-if="maxWeekdayLabel">
            {{ maxWeekdayLabel }}요일 소비가 가장 많았어요
          </p>
          <div class="d-flex align-items-end justify-content-between" style="height: 120px">
            <div
              v-for="day in report.weekdaySpending"
              :key="day.dayOfWeek"
              class="d-flex flex-column align-items-center flex-fill"
            >
              <div class="small text-secondary mb-1 fw-semibold">{{ formatCompact(day.amount) }}</div>
              <div
                class="rounded-top"
                style="width: 24px"
                :style="{
                  height: (day.amount / maxWeekday) * 70 + 'px',
                  backgroundColor: day.dayOfWeek === maxWeekdayLabel ? '#ffc107' : '#ced4da',
                }"
              ></div>
              <div class="small text-secondary mt-1">{{ day.dayOfWeek }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 연령별 비교 Top3 -->
      <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body">
          <h2 class="h6 text-secondary mb-3 fw-bold">
            많이 쓴 카테고리 Top3
            <span class="float-end small">
              <span :style="{ color: MY_COLOR }">■</span> 내 소비
              <span class="text-secondary ms-2">■</span> 또래 평균
            </span>
          </h2>
          <div v-for="(cat, i) in report.topCategories" :key="cat.categoryId" class="mb-3">
            <div class="small mb-1 fw-semibold">{{ i + 1 }}. {{ cat.categoryName }}</div>
            <div class="d-flex align-items-center gap-2 mb-1">
              <div class="progress flex-grow-1" style="height: 10px">
                <div
                  class="progress-bar"
                  :style="{ width: (cat.myAmount / maxCompare) * 100 + '%', backgroundColor: MY_COLOR }"
                ></div>
              </div>
              <span class="small fw-semibold" style="width: 70px">{{ formatCompact(cat.myAmount) }}</span>
            </div>
            <div class="d-flex align-items-center gap-2">
              <div class="progress flex-grow-1" style="height: 10px">
                <div
                  class="progress-bar bg-secondary"
                  :style="{ width: ((cat.peerAmount ?? 0) / maxCompare) * 100 + '%' }"
                ></div>
              </div>
              <span class="small text-secondary" style="width: 70px">{{ formatCompact(cat.peerAmount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 요약 -->
      <div class="card mb-4 shadow-sm rounded-4" style="background-color: #fef7d8; border: none">
        <div class="card-body">
          <h2 class="h6 mb-2"><i class="fa-solid fa-wand-magic-sparkles" style="color: #fc9558;"></i> 이달의 Tip</h2>
          <p class="small mb-0">{{ report.aiSummary }}</p>
        </div>
      </div>
    </template>

    <p v-else class="text-secondary text-center mt-5">리포트를 불러오는 중이에요...</p>
  </div>
</template>