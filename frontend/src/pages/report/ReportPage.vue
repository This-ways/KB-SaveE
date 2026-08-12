<script setup>
import { ref, computed } from 'vue'
import moment from 'moment'
import reportApi from '@/api/reportApi'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

const router = useRouter()
const authStore = useAuthStore()

// 마이데이터 미연결 상태에서 뒤로가기 시 로그아웃
const goHome = () => {
  if (!authStore.isMydataConnected) {
    authStore.logout()
    router.push('/auth/login')
    return
  }
  router.push({ name: 'home' })
}

const userId = computed(() => authStore.userId)

// PDF 저장 참조 변수
const pdfContainerRef = ref(null)
const pdfExporting = ref(false)

// 커스텀 확인 모달 상태 (브라우저 기본 confirm 대신, 우리 디자인으로 직접 그림)
const showPasswordNotice = ref(false)
let resolvePasswordNotice = null

const askPasswordNoticeConfirm = () => {
  showPasswordNotice.value = true
  return new Promise((resolve) => {
    resolvePasswordNotice = resolve
  })
}

const confirmPasswordNotice = () => {
  showPasswordNotice.value = false
  resolvePasswordNotice?.(true)
}

const cancelPasswordNotice = () => {
  showPasswordNotice.value = false
  resolvePasswordNotice?.(false)
}

const exportPdf = async () => {
  if (pdfExporting.value) return
  if (!pdfContainerRef.value) return

  // 비밀번호 = 생년월일 6자리(YYMMDD). authStore에 없으면 비밀번호 없이 저장(기능이 안 죽게 방어)
  const birthPassword = authStore.birthDate ? moment(authStore.birthDate).format('YYMMDD') : null

  if (birthPassword) {
    const proceed = await askPasswordNoticeConfirm()
    if (!proceed) return
  } else {
    console.warn('생년월일 정보가 없어 PDF 비밀번호 없이 저장합니다.')
  }

  pdfExporting.value = true
  try {
    // 캡처 영역 임시 노출 처리
    const el = pdfContainerRef.value
    el.style.display = 'block'

    const canvas = await html2canvas(el, {
      scale: 2,
      backgroundColor: '#f8f9fa',
      useCORS: true
    })

    // 임시 노출 원복
    el.style.display = 'none'

    const pdf = birthPassword
      ? new jsPDF({
          orientation: 'p',
          unit: 'mm',
          format: 'a4',
          encryption: {
            userPassword: birthPassword,
            ownerPassword: birthPassword,
            userPermissions: ['print'],
          },
        })
      : new jsPDF('p', 'mm', 'a4')

    const pageWidth = pdf.internal.pageSize.getWidth()
    const pageHeight = pdf.internal.pageSize.getHeight()

    const imgWidth = pageWidth
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    const imgData = canvas.toDataURL('image/png')

    // A4 한 장에 맞춰 이미지 추가
    pdf.addImage(imgData, 'PNG', 0, 0, imgWidth, Math.min(imgHeight, pageHeight))
    pdf.save(`소비리포트_${yearMonth.value}.pdf`)
  } catch (e) {
    console.error('PDF 저장 실패', e)
    alert('PDF 저장에 실패했어요. 잠시 후 다시 시도해주세요.')
  } finally {
    pdfExporting.value = false
  }
}

const yearMonth = ref(moment().format('YYYY-MM'))
const report = ref(null) // 이번 달 리포트
const prevSaveAmount = ref(null) // 전월 세이브 금액

const aiRefreshing = ref(false)
const aiRefreshError = ref('')

const refreshAiSummary = async () => {
  if (aiRefreshing.value) return
  aiRefreshing.value = true
  aiRefreshError.value = ''
  try {
    const newSummary = await reportApi.refreshAiSummary({ userId: userId.value, yearMonth: yearMonth.value })
    report.value.aiSummary = newSummary
  } catch (e) {
    aiRefreshError.value = '10분 안에 이미 새로고침했어요. 잠시 후 다시 시도해주세요.'
    setTimeout(() => (aiRefreshError.value = ''), 3000)
  } finally {
    aiRefreshing.value = false
  }
}

const loadReport = async () => {
  try {
    report.value = await reportApi.get({ userId: userId.value, yearMonth: yearMonth.value })
  } catch (e) {
    console.error('리포트 조회 실패', e)
    return
  }

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
const MIN_YEAR_MONTH = '2026-01'

const monthLabel = computed(() => moment(yearMonth.value, 'YYYY-MM').format('YYYY년 MM월'))
const prevMonthLabel = computed(() => moment(yearMonth.value, 'YYYY-MM').subtract(1, 'months').format('MM월'))
const isAtMinMonth = computed(() => yearMonth.value === MIN_YEAR_MONTH)
const isAtMaxMonth = computed(() => yearMonth.value === moment().format('YYYY-MM'))

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

const weekDayCounts = computed(() => {
  const counts = {}
  const monthStart = moment(yearMonth.value, 'YYYY-MM').startOf('month')
  const firstDow = monthStart.day() + 1
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

// ===== 연령별 비교 =====
const maxCompare = computed(() => {
  if (!report.value) return 1
  const values = report.value.topCategories.flatMap((c) => [c.myAmount, c.peerAmount ?? 0])
  return Math.max(...values, 1)
})

const MY_COLOR = '#ffd239'
</script>

<template>
  <div style="padding: 20px 20px 100px; background-color: #f8f9fa; min-height: 100vh">
    <template v-if="report">
      <!-- 네비게이션 헤더 -->
      <div class="d-flex align-items-center mb-3">
        <button type="button" class="btn btn-sm p-0 no-print" @click="goHome" style="width: 24px">
          <i class="fa-solid fa-chevron-left" style="color: #495057; font-size: 18px"></i>
        </button>
        <h1 class="h5 mb-0 fw-bold flex-grow-1 text-center">소비 리포트</h1>
        <button
          type="button"
          class="btn btn-sm p-0 no-print d-flex flex-column align-items-center"
          :disabled="pdfExporting"
          @click="exportPdf"
          aria-label="PDF 내보내기"
        >
          <i
            class="fa-solid"
            :class="pdfExporting ? 'fa-spinner fa-spin' : 'fa-file-arrow-down'"
            style="color: #495057; font-size: 18px"
          ></i>
          <span class="text-secondary" style="font-size: 10px">PDF 내보내기</span>
        </button>
      </div>

      <!-- 날짜 이동 -->
      <div class="d-flex align-items-center justify-content-center gap-3 mb-4">
        <button type="button" class="btn btn-sm btn-light no-print" :disabled="isAtMinMonth" @click="prevMonth">
          <i class="fa-solid fa-chevron-left" :style="{ color: isAtMinMonth ? '#ced4da' : '#495057' }"></i>
        </button>
        <span class="fw-semibold">{{ monthLabel }}</span>
        <button type="button" class="btn btn-sm btn-light no-print" :disabled="isAtMaxMonth" @click="nextMonth">
          <i class="fa-solid fa-chevron-right" :style="{ color: isAtMaxMonth ? '#ced4da' : '#495057' }"></i>
        </button>
      </div>

      <!-- 일반 모바일 화면용 뷰 -->
      <div class="mobile-view">
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
                <div class="text-secondary small mb-1"><i class="fa-solid fa-piggy-bank" style="color: #fc9558;"></i> 목표 대비 절약액</div>
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
                <div class="text-secondary small mb-1"><i class="fa-solid fa-coins" style="color: #fc9558;"></i> 이번 달 적금 납입액</div>
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

        <!-- 많이 쓴 카테고리 Top3 -->
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

        <!-- AI 요약 (화면 표시용) -->
        <div class="card mb-4 shadow-sm rounded-4" style="background-color: #fef7d8; border: none">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h2 class="h6 mb-0"><i class="fa-solid fa-wand-magic-sparkles" style="color: #fc9558;"></i> 이달의 Tip</h2>
              <button
                type="button"
                class="btn btn-sm p-0 no-print"
                :disabled="aiRefreshing"
                @click="refreshAiSummary"
                aria-label="AI 요약 새로고침"
              >
                <i
                  class="fa-solid fa-rotate-right"
                  :class="{ 'fa-spin': aiRefreshing }"
                  style="color: #fc9558; font-size: 14px"
                ></i>
              </button>
            </div>
            <p class="small mb-0">{{ report.aiSummary }}</p>
            <p v-if="aiRefreshError" class="small text-danger mb-0 mt-1">{{ aiRefreshError }}</p>
          </div>
        </div>
      </div>

      <!-- PDF 캡처용 1페이지 (좌/우 2컬럼 레이아웃, 오프스크린 렌더링) -->
      <div ref="pdfContainerRef" class="pdf-export-layout p-4" style="display: none; background-color: #f8f9fa; width: 800px;">
        <!-- 상단 헤더 / 타이틀 -->
        <div class="text-center mb-4">
          <h1 class="h4 fw-bold mb-1">소비 리포트</h1>
          <span class="text-secondary fw-semibold">{{ monthLabel }}</span>
        </div>

        <div class="row g-3">
          <!-- 좌측 컬럼: 현금흐름, 목표대비 절약액/적금, 주차별 소비 -->
          <div class="col-6">
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

            <!-- 목표대비/적금납입 -->
            <div class="row g-2 mb-3">
              <div class="col-6">
                <div class="card h-100 border-0 shadow-sm rounded-4 bg-white">
                  <div class="card-body text-center p-2">
                    <div class="text-secondary small mb-1" style="font-size: 11px;"><i class="fa-solid fa-piggy-bank" style="color: #fc9558;"></i> 목표 대비 절약액</div>
                    <div
                      class="fw-bold small"
                      :style="{ color: report.saveAmount > 0 ? '#127f5f' : report.saveAmount < 0 ? '#e8512b' : '' }"
                    >
                      {{ formatSigned(report.saveAmount) }}
                    </div>
                  </div>
                </div>
              </div>
              <div class="col-6">
                <div class="card h-100 border-0 shadow-sm rounded-4 bg-white">
                  <div class="card-body text-center p-2">
                    <div class="text-secondary small mb-1" style="font-size: 11px;"><i class="fa-solid fa-coins" style="color: #fc9558;"></i> 이번 달 적금 납입액</div>
                    <div class="fw-bold small">{{ formatAmount(report.savingPayment) }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 주차별 소비 -->
            <div class="card border-0 shadow-sm rounded-4 bg-white">
              <div class="card-body">
                <h2 class="h6 text-secondary mb-3 fw-bold">주차별 소비</h2>
                <div class="d-flex align-items-end justify-content-between" style="height: 100px">
                  <div
                    v-for="week in report.weeklySpending"
                    :key="week.weekNo"
                    class="d-flex flex-column align-items-center flex-fill"
                  >
                    <div class="small text-secondary mb-1 fw-semibold" style="font-size: 10px;">{{ formatCompact(week.amount) }}</div>
                    <div
                      class="rounded-top"
                      style="width: 18px"
                      :style="{
                        height: (week.amount / maxWeekly) * 55 + 'px',
                        backgroundColor: week.weekNo === maxWeeklyLabel ? '#ffc107' : '#ced4da',
                      }"
                    ></div>
                    <div class="small text-secondary mt-1" style="font-size: 10px;">
                      {{ week.weekNo }}주
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 우측 컬럼: 요일별 패턴, 많이 쓴 카테고리 Top3 -->
          <div class="col-6">
            <!-- 요일별 소비 패턴 -->
            <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
              <div class="card-body">
                <h2 class="h6 text-secondary mb-1 fw-bold">요일별 소비 패턴</h2>
                <p class="small text-secondary mb-3" v-if="maxWeekdayLabel" style="font-size: 11px;">
                  {{ maxWeekdayLabel }}요일 소비가 가장 많았어요
                </p>
                <div class="d-flex align-items-end justify-content-between" style="height: 100px">
                  <div
                    v-for="day in report.weekdaySpending"
                    :key="day.dayOfWeek"
                    class="d-flex flex-column align-items-center flex-fill"
                  >
                    <div class="small text-secondary mb-1 fw-semibold" style="font-size: 10px;">{{ formatCompact(day.amount) }}</div>
                    <div
                      class="rounded-top"
                      style="width: 18px"
                      :style="{
                        height: (day.amount / maxWeekday) * 55 + 'px',
                        backgroundColor: day.dayOfWeek === maxWeekdayLabel ? '#ffc107' : '#ced4da',
                      }"
                    ></div>
                    <div class="small text-secondary mt-1" style="font-size: 10px;">{{ day.dayOfWeek }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 많이 쓴 카테고리 Top3 -->
            <div class="card border-0 shadow-sm rounded-4 bg-white">
              <div class="card-body">
                <h2 class="h6 text-secondary mb-3 fw-bold">
                  많이 쓴 카테고리 Top3
                  <span class="float-end small" style="font-size: 10px;">
                    <span :style="{ color: MY_COLOR }">■</span> 내 소비
                    <span class="text-secondary ms-1">■</span> 또래
                  </span>
                </h2>
                <div v-for="(cat, i) in report.topCategories" :key="cat.categoryId" class="mb-3">
                  <div class="small mb-1 fw-semibold" style="font-size: 11px;">{{ i + 1 }}. {{ cat.categoryName }}</div>
                  <div class="d-flex align-items-center gap-2 mb-1">
                    <div class="progress flex-grow-1" style="height: 8px">
                      <div
                        class="progress-bar"
                        :style="{ width: (cat.myAmount / maxCompare) * 100 + '%', backgroundColor: MY_COLOR }"
                      ></div>
                    </div>
                    <span class="small fw-semibold" style="width: 55px; font-size: 10px;">{{ formatCompact(cat.myAmount) }}</span>
                  </div>
                  <div class="d-flex align-items-center gap-2">
                    <div class="progress flex-grow-1" style="height: 8px">
                      <div
                        class="progress-bar bg-secondary"
                        :style="{ width: ((cat.peerAmount ?? 0) / maxCompare) * 100 + '%' }"
                      ></div>
                    </div>
                    <span class="small text-secondary" style="width: 55px; font-size: 10px;">{{ formatCompact(cat.peerAmount) }}</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- (이달의 Tip 제거됨) -->
          </div>
        </div>
      </div>
    </template>

    <p v-else class="text-secondary text-center mt-5">리포트를 불러오는 중이에요...</p>
  </div>

  <!-- PDF 비밀번호 안내 모달 (브라우저 기본 confirm 대신 우리 디자인으로) -->
  <div
    v-if="showPasswordNotice"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background-color: rgba(0, 0, 0, 0.4); z-index: 1000"
    @click.self="cancelPasswordNotice"
  >
    <div class="bg-white rounded-4 shadow p-4" style="width: 300px">
      <div class="d-flex align-items-center gap-2 mb-3">
        <i class="fa-solid fa-lock" style="color: #fc9558; font-size: 20px"></i>
        <h2 class="h6 mb-0 fw-bold">PDF 보안 안내</h2>
      </div>
      <p class="small text-secondary mb-1">PDF 파일은 비밀번호로 보호돼요.</p>
      <p class="small mb-4">
        비밀번호는 <span class="fw-bold">생년월일 6자리</span>예요.
      </p>
      <div class="d-flex gap-2">
        <button type="button" class="btn btn-light flex-fill" @click="cancelPasswordNotice">취소</button>
        <button
          type="button"
          class="btn flex-fill fw-semibold"
          style="background-color: #ffd239; color: #212529"
          @click="confirmPasswordNotice"
        >
          계속 진행
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media print {
  .no-print {
    display: none !important;
  }
}
</style>