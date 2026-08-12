<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';
import { useAlert } from '@/util/useAlert';

import CustomAlertModal from '@/components/common/CustomAlertModal.vue'; // 2. 공용 모달 임포트

const { alertState, showAlert, hideAlert } = useAlert();

const route = useRoute();
const router = useRouter();

const productId = ref(Number(route.params.productId));

const isBottomSheetOpen = ref(false);

// 백엔드에서 받아온 상품 정보 및 금리 리스트
const product = ref(null);
const rateList = ref([]); // [{ saveTerm: 6, minRate: 2.0, maxRate: 6.0 }, ...]

// 입력 폼 상태
const saveType = ref(route.query.saveType || '자유적립식');
const saveTerm = ref(Number(route.query.saveTerm) || 6);
const depositAmount = ref(Number(route.query.monthlyAmount) || 10000);
const paymentDay = ref(28);
const autoTransferAmount = ref(depositAmount.value);

const currentStep = ref(1); // 1: 입력폼, 2: 정보확인, 3: 가입완료
const loading = ref(true);

// 우대이율 데이터
const primeRates = ref([
  { id: 1, name: '급여 이체', rate: 0.1, selected: false },
  { id: 2, name: '카드결제계좌', rate: 0.1, selected: false },
  { id: 3, name: '자동이체저축', rate: 0.1, selected: false },
  { id: 4, name: '아파트관리비이체', rate: 0.1, selected: false },
  { id: 5, name: 'KB스타뱅킹 이체', rate: 0.1, selected: false },
  { id: 6, name: '장기거래', rate: 0.1, selected: false },
  { id: 7, name: '첫 거래', rate: 0.1, selected: false },
  { id: 8, name: '주택청약종합저축', rate: 0.1, selected: false },
  { id: 9, name: '소중한 날', rate: 0.1, selected: false },
]);

const togglePrimeRate = (item) => {
  if (!item.selected) {
    const selectedCount = primeRates.value.filter((p) => p.selected).length;
    if (selectedCount >= 6) {
      showAlert('우대금리는 최대 6개까지 선택 가능합니다.');
      return;
    }
  }
  item.selected = !item.selected;
};

// 동적 적립방식 옵션 및 선택 가능 여부 계산
const availableSaveTypes = computed(() => {
  const pType = product.value?.productType || product.value?.saveType || '';
  if (pType.includes(',')) {
    return pType.split(',').map((t) => t.trim());
  }
  return [pType.trim() || '자유적립식'];
});

// 둘 다 지원할 때만 선택 가능 (1개만 지원 시 disabled)
const isSaveTypeSelectable = computed(
  () => availableSaveTypes.value.length > 1,
);

// 가입 기간에 따른 기본금리 동적 구하기
const baseRate = computed(() => {
  if (!rateList.value || rateList.value.length === 0) return 2.0;
  const matched = rateList.value.find((r) => r.saveTerm === saveTerm.value);
  return matched ? matched.minRate : rateList.value[0].minRate;
});

// 우대금리 합산 및 최종 금리 계산
const selectedPrimeRateTotal = computed(() => {
  return primeRates.value
    .filter((p) => p.selected)
    .reduce((sum, p) => sum + p.rate, 0);
});

const calculatedAppliedRate = computed(() => {
  const total = baseRate.value + selectedPrimeRateTotal.value;
  return Number(total.toFixed(2));
});

// 상품 상세 정보 조회 API 호출
const fetchProductDetail = async () => {
  try {
    loading.value = true;
    const res = await savingsApi.getSavingsDetail(productId.value);
    product.value = res;
    rateList.value = res.rateList || [];

    // 적립방식 자동 세팅
    if (availableSaveTypes.value.length > 0) {
      if (!availableSaveTypes.value.includes(saveType.value)) {
        saveType.value = availableSaveTypes.value[0];
      }
    }

    // 가입 기간 세팅 (상품이 지원하는 기간에 맞춤)
    if (rateList.value.length > 0) {
      const hasTerm = rateList.value.some((r) => r.saveTerm === saveTerm.value);
      if (!hasTerm) {
        saveTerm.value = rateList.value[0].saveTerm;
      }
    }
  } catch (error) {
    console.error('상품 상세 정보 조회 실패:', error);
  } finally {
    loading.value = false;
  }
};

// 상품의 최소 / 최대 가입금액 정보 가져오기
const minDepositAmount = computed(() => {
  const text = product.value?.depositAmountText || '';
  const matches = text.replace(/,/g, '').match(/\d+/g);
  if (matches && matches.length > 0) {
    return Number(matches[0]);
  }
  return 1000;
});

const maxDepositAmount = computed(() => {
  const text = product.value?.depositAmountText || '';
  const matches = text.replace(/,/g, '').match(/\d+/g);
  if (matches && matches.length > 1) {
    return Number(matches[1]);
  }
  return 1000000;
});

// 최소 ~ 최대 범위 만족 여부 및 초과/미달 에러 메시지
const amountError = computed(() => {
  if (depositAmount.value < minDepositAmount.value) {
    return `최소 가입금액은 ${minDepositAmount.value.toLocaleString()}원입니다.`;
  }
  if (depositAmount.value > maxDepositAmount.value) {
    return `최대 가입금액은 ${maxDepositAmount.value.toLocaleString()}원입니다.`;
  }
  return '';
});

// 가입금액 세 자릿수 콤마 처리
const formattedDepositAmount = computed({
  get() {
    if (depositAmount.value === null || depositAmount.value === undefined)
      return '0';
    return depositAmount.value.toLocaleString('ko-KR');
  },
  set(newValue) {
    const numericValue = String(newValue).replace(/[^0-9]/g, '');
    const num = numericValue ? Number(numericValue) : 0;
    depositAmount.value = num;
    autoTransferAmount.value = num;
  },
});

// 자동이체금액 콤마 처리
const formattedAutoTransferAmount = computed({
  get() {
    if (
      autoTransferAmount.value === null ||
      autoTransferAmount.value === undefined
    )
      return '0';
    return autoTransferAmount.value.toLocaleString('ko-KR');
  },
  set(newValue) {
    const numericValue = String(newValue).replace(/[^0-9]/g, '');
    autoTransferAmount.value = numericValue ? Number(numericValue) : 0;
  },
});

// 숫자 및 콤마 실시간 제어 핸들러
const handleDepositInput = (event) => {
  const cleanValue = event.target.value.replace(/[^0-9]/g, '');
  const num = cleanValue ? Number(cleanValue) : 0;
  depositAmount.value = num;
  autoTransferAmount.value = num;
  event.target.value = formattedDepositAmount.value;
};

const handleAutoTransferInput = (event) => {
  const cleanValue = event.target.value.replace(/[^0-9]/g, '');
  autoTransferAmount.value = cleanValue ? Number(cleanValue) : 0;
  event.target.value = formattedAutoTransferAmount.value;
};

// 가입 확인 및 최종 가입 응답 상태 관리
const confirmData = ref(null);
const resultData = ref(null);
const isConfirming = ref(false);

// [STEP 1 -> STEP 2] 가입정보 확인 API 호출
const handleConfirm = async () => {
  if (amountError.value) {
    showAlert(amountError.value);
    return;
  }

  try {
    isConfirming.value = true;

    const payload = {
      productId: productId.value,
      saveType: saveType.value,
      saveTerm: saveTerm.value,
      depositAmount: depositAmount.value,
      paymentDay: paymentDay.value,
      autoTransferAmount: autoTransferAmount.value,
      appliedRate: calculatedAppliedRate.value,
    };

    const response = await savingsApi.confirmSubscribe(payload);
    confirmData.value = response;

    currentStep.value = 2; // 가입 정보 확인 화면으로 이동
  } catch (error) {
    console.error('가입 정보 확인 실패:', error);
    showAlert(
      error.response?.data?.message ||
        error.response?.data ||
        '가입 정보를 확인하는 중 오류가 발생했습니다.',
    );
  } finally {
    isConfirming.value = false;
  }
};

// [STEP 2 -> STEP 3] 최종 적금 가입 처리 API 호출
const handleFinalSubscribe = async () => {
  try {
    loading.value = true;

    const payload = {
      productId: productId.value,
      saveType: saveType.value,
      saveTerm: saveTerm.value,
      depositAmount: depositAmount.value,
      paymentDay: paymentDay.value,
      autoTransferAmount: autoTransferAmount.value,
      appliedRate: calculatedAppliedRate.value,
    };

    // 백엔드 최종 가입 API 호출 (POST /savings/subscribe)
    const response = await savingsApi.subscribeSavings(payload);
    resultData.value = response;

    currentStep.value = 3; // 가입 완료 화면으로 전환
  } catch (error) {
    console.error('적금 가입 처리 실패:', error);
    showAlert(
      error.response?.data?.message ||
        error.response?.data ||
        '적금 가입 중 오류가 발생했습니다.',
    );
  } finally {
    loading.value = false;
  }
};

const formatDateKorean = (dateStr) => {
  if (!dateStr) return '0000년 00월 00일';

  // '2029-08-10' 또는 '2029.08.10' 또는 '20290810' 대응
  const cleanDate = String(dateStr).replace(/[^0-9]/g, '');
  if (cleanDate.length === 8) {
    const year = cleanDate.substring(0, 4);
    const month = cleanDate.substring(4, 6);
    const day = cleanDate.substring(6, 8);
    return `${year}년 ${month}월 ${day}일`;
  }
  return dateStr;
};

// 가입 정보 확인 화면에서 뒤로가기 시 입력 단계(STEP 1)로 복귀
const goBackToInput = () => {
  currentStep.value = 1;
};

onMounted(() => {
  fetchProductDetail();
});

const onAmountChange = () => {
  autoTransferAmount.value = depositAmount.value;
};
</script>

<template>
  <div
    class="container py-3"
    style="max-width: 480px; background-color: #fff; min-height: 100vh"
  >
    <!-- 로딩 화면 -->
    <div v-if="loading" class="text-center py-5 text-secondary">
      상품 정보를 불러오는 중입니다...
    </div>

    <!-- ========================================== -->
    <!-- STEP 1: 입력 폼 단계 (우대금리 & 가입조건) -->
    <!-- ========================================== -->
    <div v-else-if="currentStep === 1">
      <div class="header">
        <button class="back-btn" @click="router.back()">
          <i class="fa-solid fa-chevron-left"></i>
        </button>
        <h1 class="header-title text-truncate">
          {{ product?.productName || '적금 신규' }}
        </h1>
      </div>

      <!-- 우대금리 선택 카드 -->
      <div
        class="card p-3 border-0 bg-light rounded-4 mb-3 d-flex flex-row justify-content-between align-items-center"
        style="cursor: pointer"
        @click="isBottomSheetOpen = true"
      >
        <div class="d-flex align-items-center gap-2">
          <i class="fa-solid fa-gift text-warning fs-5"></i>
          <div>
            <h6 class="fw-bold mb-0 fs-6">우대금리 선택</h6>
            <span class="micro-text text-secondary">
              {{ primeRates.filter((p) => p.selected).length }} / 6개 선택됨
            </span>
          </div>
        </div>
        <div class="d-flex align-items-center gap-1 text-secondary small">
          <span class="fw-bold text-dark me-1">
            +{{
              primeRates
                .filter((p) => p.selected)
                .reduce((sum, p) => sum + p.rate, 0)
                .toFixed(2)
            }}%p
          </span>
          <i class="fa-solid fa-chevron-right micro-text"></i>
        </div>
      </div>

      <!--  바텀시트 모달 (화면 하단에서 슬라이드 업) -->
      <Teleport to="body">
        <!-- 배경 오버레이 -->
        <Transition name="fade">
          <div
            v-if="isBottomSheetOpen"
            class="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-50"
            style="z-index: 1050"
            @click="isBottomSheetOpen = false"
          ></div>
        </Transition>

        <!-- 바텀시트 컨텐츠 -->
        <Transition name="slide-up">
          <div
            v-if="isBottomSheetOpen"
            class="position-fixed bottom-0 start-50 translate-middle-x bg-white rounded-top-4 p-3 w-100 shadow-lg d-flex flex-column"
            style="z-index: 1055; max-width: 430px; max-height: 80vh"
          >
            <!-- 손잡이 바 & 헤더 -->
            <div
              class="bg-secondary opacity-25 rounded-pill mx-auto mb-3"
              style="width: 40px; height: 4px"
            ></div>

            <div
              class="d-flex justify-content-between align-items-center mb-3 px-1"
            >
              <h6 class="fw-bold mb-0 fs-6">
                우대금리를 선택해 주세요
                <span class="fw-normal text-secondary micro-text"
                  >(최대 6개)</span
                >
              </h6>
              <button
                type="button"
                class="btn-close small"
                @click="isBottomSheetOpen = false"
              ></button>
            </div>

            <!-- 기존 체크박스 리스트 (스크롤 영역) -->
            <div class="d-flex flex-column gap-2 overflow-auto pe-1 mb-3">
              <div
                v-for="item in primeRates"
                :key="item.id"
                class="d-flex align-items-center justify-content-between p-2 px-3 rounded-3 bg-white border"
                :class="{
                  'border-warning bg-warning bg-opacity-10': item.selected,
                }"
                style="cursor: pointer; transition: all 0.2s"
                @click="togglePrimeRate(item)"
              >
                <div
                  class="d-flex align-items-center gap-2 flex-nowrap min-w-0 me-2"
                >
                  <i
                    class="fa-circle-check fs-5 flex-shrink-0"
                    :class="
                      item.selected
                        ? 'fa-solid text-warning'
                        : 'fa-regular text-secondary opacity-50'
                    "
                  ></i>
                  <span
                    class="small text-nowrap"
                    :class="
                      item.selected ? 'fw-bold text-dark' : 'text-secondary'
                    "
                  >
                    {{ item.name }}
                  </span>
                </div>

                <span
                  class="small text-nowrap flex-shrink-0"
                  :class="
                    item.selected ? 'fw-bold text-dark' : 'text-secondary'
                  "
                >
                  {{ item.rate }}%p
                </span>
              </div>
            </div>

            <!-- 하단 완료 버튼 -->
            <button
              type="button"
              class="btn btn-warning w-100 fw-bold py-2 rounded-3"
              @click="isBottomSheetOpen = false"
            >
              선택 완료
            </button>
          </div>
        </Transition>
      </Teleport>

      <!-- 금리 현황 요약 -->
      <div class="p-3 bg-warning bg-opacity-10 rounded-4 mb-4">
        <div class="d-flex justify-content-between small text-secondary mb-1">
          <span>기본 금리</span>
          <span>연 {{ baseRate.toFixed(2) }}%</span>
        </div>
        <div class="d-flex justify-content-between small text-secondary mb-1">
          <span>선택 우대 금리</span>
          <span class="text-success fw-bold"
            >+{{ selectedPrimeRateTotal.toFixed(2) }}%p</span
          >
        </div>
        <hr class="my-2" />
        <div class="d-flex justify-content-between fw-bold text-dark h6 mb-0">
          <span>최종 적용 금리</span>
          <span class="text-warning fs-5"
            >연 {{ calculatedAppliedRate.toFixed(2) }}%</span
          >
        </div>
      </div>

      <!-- 입력 폼 -->
      <div class="d-flex flex-column gap-3 mb-4">
        <!-- 1. 적립방식 선택 (버튼 방식) -->
        <div>
          <label class="form-label small text-secondary d-block mb-2"
            >적립방식</label
          >
          <div class="d-flex gap-2">
            <button
              v-for="type in availableSaveTypes"
              :key="type"
              type="button"
              class="btn flex-fill py-2-5 fw-bold rounded-3 transition-all"
              :class="
                saveType === type
                  ? 'btn-warning text-dark border-warning'
                  : 'btn-outline-secondary text-secondary border-light-subtle bg-light'
              "
              :disabled="!isSaveTypeSelectable"
              @click="saveType = type"
            >
              {{ type }}
            </button>
          </div>
        </div>

        <!-- 2. 가입기간 선택 (버튼 방식) -->
        <div>
          <label class="form-label small text-secondary d-block mb-2"
            >가입기간</label
          >
          <div class="row g-2">
            <div v-for="rate in rateList" :key="rate.saveTerm" class="col">
              <button
                type="button"
                class="btn w-100 py-2-5 fw-bold rounded-3 transition-all text-nowrap"
                :class="
                  saveTerm === rate.saveTerm
                    ? 'btn-warning text-dark border-warning'
                    : 'btn-outline-secondary text-secondary border-light-subtle bg-light'
                "
                @click="saveTerm = rate.saveTerm"
              >
                {{ rate.saveTerm }}개월
              </button>
            </div>
          </div>
        </div>

        <!-- 3. 가입금액 입력 -->
        <div>
          <div class="d-flex justify-content-between align-items-center mb-1">
            <label class="form-label small text-secondary mb-0"
              >가입금액 (원)</label
            >
            <span class="micro-text text-secondary">
              {{
                product?.depositAmountText ||
                `최소 ${minDepositAmount.toLocaleString()}원 ~ 최대 ${maxDepositAmount.toLocaleString()}원`
              }}
            </span>
          </div>

          <input
            :value="formattedDepositAmount"
            type="text"
            inputmode="numeric"
            class="form-control rounded-3 fw-bold text-end pe-3"
            :class="{ 'border-danger text-danger': amountError }"
            placeholder="0"
            @input="handleDepositInput"
          />

          <!-- 범위 초과 시 에러 메시지 -->
          <div v-if="amountError" class="text-danger micro-text mt-1 text-end">
            {{ amountError }}
          </div>
        </div>

        <!-- 4. 자동이체 설정 -->
        <div>
          <label class="form-label small text-secondary">자동이체 설정</label>
          <div class="input-group">
            <span class="input-group-text bg-light text-secondary micro-text"
              >매월</span
            >
            <input
              v-model.number="paymentDay"
              type="number"
              class="form-control fw-bold text-center"
              placeholder="28"
              min="1"
              max="31"
            />
            <span class="input-group-text bg-light text-secondary micro-text"
              >일 /</span
            >
            <input
              :value="formattedAutoTransferAmount"
              type="text"
              inputmode="numeric"
              class="form-control fw-bold text-end pe-3"
              @input="handleAutoTransferInput"
            />
            <span class="input-group-text bg-light text-secondary micro-text"
              >원</span
            >
          </div>
        </div>
      </div>

      <button
        class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark"
        :disabled="!!amountError"
        @click="handleConfirm"
      >
        다음 (가입정보 확인)
      </button>
    </div>

    <!-- ========================================== -->
    <!-- STEP 2: 가입정보 확인 화면 (POST /confirm) -->
    <!-- ========================================== -->
    <div v-else-if="currentStep === 2" class="d-flex flex-column gap-3">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <i
          class="fa-solid fa-chevron-left fs-5"
          style="cursor: pointer"
          @click="currentStep = 1"
        ></i>
        <span class="fw-bold fs-5">적금 신규</span>
        <span
          class="text-secondary small"
          style="cursor: pointer"
          @click="router.back()"
          >취소</span
        >
      </div>

      <!-- 프로그래스 바 (3/4) -->
      <div class="d-flex justify-content-between align-items-center">
        <div class="progress w-100 me-3" style="height: 4px">
          <div
            class="progress-bar bg-warning"
            role="progressbar"
            style="width: 75%"
          ></div>
        </div>
        <span class="micro-text text-secondary fw-bold">3/4</span>
      </div>

      <h5 class="fw-bold my-1">가입정보를 확인해 주세요</h5>

      <!-- 카드 목록 -->
      <div
        class="card border-0 bg-light rounded-4 p-3 d-flex flex-column gap-3"
      >
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">상품명</span>
          <span class="fw-bold text-dark micro-text">{{
            confirmData?.productName
          }}</span>
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">가입기간</span>
          <div class="text-end micro-text">
            <span class="fw-bold text-dark me-1"
              >{{ confirmData?.saveTerm }}개월</span
            >
            <span class="text-primary fw-semibold"
              >({{ confirmData?.endDate }})</span
            >
          </div>
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">가입금액</span>
          <span class="fw-bold text-dark micro-text"
            >{{ confirmData?.depositAmount?.toLocaleString() }}원</span
          >
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">적용금리</span>
          <span class="fw-bold text-warning micro-text fs-6"
            >연 {{ confirmData?.appliedRate }}%</span
          >
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">적립방식</span>
          <span class="fw-bold text-dark micro-text">{{
            confirmData?.saveType
          }}</span>
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">출금계좌번호</span>
          <span class="fw-bold text-dark micro-text">{{
            confirmData?.depositAccountNo
          }}</span>
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">자동이체일</span>
          <span class="fw-bold text-dark micro-text"
            >매월 {{ confirmData?.paymentDay }}일</span
          >
        </div>

        <div class="d-flex justify-content-between align-items-center">
          <span class="text-secondary micro-text">자동이체금액</span>
          <span class="fw-bold text-dark micro-text"
            >{{ confirmData?.autoTransferAmount?.toLocaleString() }}원</span
          >
        </div>
      </div>

      <!-- 하단 안내 문구 -->
      <div class="p-3 bg-light rounded-3 micro-text text-secondary lh-base">
        <p class="mb-1">
          * 위 상품정보 요약은 이용자의 상품에 대한 이해를 제고하기 위해
          안내하며, 자세한 내용은 해당 상품의 상품설명서를 참고해주시기
          바랍니다.
        </p>
        <p class="mb-0">
          * 가입 중인 계좌의 비밀번호는 KB국민은행 출금계좌 비밀번호와
          동일합니다.
        </p>
      </div>

      <button
        class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark mt-2"
        :disabled="loading"
        @click="handleFinalSubscribe"
      >
        {{ loading ? '가입 처리 중...' : '다음 (가입 완료)' }}
      </button>
    </div>

    <!-- ========================================== -->
    <!-- STEP 3: 최종 가입 완료 화면 (POST /subscribe) -->
    <!-- ========================================== -->
    <div v-else-if="currentStep === 3" class="d-flex flex-column gap-3 py-2">
      <!-- 헤더 -->
      <div class="header d-flex justify-content-between align-items-center">
        <h1 class="header-title">적금 신규</h1>
        <div class="d-flex align-items-center gap-3 text-secondary">
          <button
            class="icon-btn border-0 bg-transparent p-0"
            style="cursor: pointer"
            @click="router.push('/home')"
          >
            <i class="fa-solid fa-house"></i>
          </button>
          <button
            class="icon-btn border-0 bg-transparent p-0"
            style="cursor: pointer"
          >
            <i class="fa-solid fa-bars"></i>
          </button>
        </div>
      </div>

      <!-- 상단 축하 캐릭터 및 일자 강조 카드 -->
      <div class="text-center py-4 my-2">
        <!-- 캐릭터 아이콘 (꿀벌) -->
        <div class="mb-3 d-inline-block position-relative">
          <div
            class="bg-warning bg-opacity-20 rounded-circle p-4 d-flex align-items-center justify-content-center"
            style="width: 100px; height: 100px; margin: 0 auto"
          >
            <!-- 🐝 꿀벌 이모지 -->
            <span class="display-3">🐝</span>
          </div>
        </div>

        <h4 class="fw-bold text-dark mb-2 lh-base">
          오늘부터 1일!<br />
          <span class="text-primary">{{
            formatDateKorean(resultData?.endDate)
          }}</span
          >까지 <br />
          함께해요!
        </h4>
      </div>

      <!-- 계좌 및 가입 세부 정보 카드 -->
      <div
        class="card border-0 bg-light rounded-4 p-3 d-flex flex-column gap-3"
      >
        <!-- 상품명 -->
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">상품명</span>
          <span class="fw-bold text-dark micro-text">{{
            resultData?.productName
          }}</span>
        </div>

        <!-- 가입금액 -->
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">가입금액</span>
          <span class="fw-bold text-dark micro-text"
            >{{ resultData?.depositAmount?.toLocaleString() }}원</span
          >
        </div>

        <!-- 적용금리 -->
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">적용금리</span>
          <div class="text-end micro-text">
            <div class="fw-bold text-primary fs-6">
              연 {{ resultData?.appliedRate }}%
            </div>
            <div class="text-secondary micro-text" style="font-size: 11px">
              (연 {{ resultData?.baseRate }}% 기본금리)
            </div>
          </div>
        </div>

        <!-- 가입계좌번호 (랜덤 생성된 계좌번호) -->
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">가입계좌번호</span>
          <span class="fw-bold text-primary micro-text fs-6">{{
            resultData?.savingsAccountNo
          }}</span>
        </div>

        <!-- 자동이체일 -->
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">자동이체일</span>
          <span class="fw-bold text-dark micro-text"
            >매월 {{ resultData?.paymentDay }}일</span
          >
        </div>

        <!-- 자동이체금액 -->
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">자동이체금액</span>
          <span class="fw-bold text-dark micro-text"
            >{{ resultData?.autoTransferAmount?.toLocaleString() }}원</span
          >
        </div>

        <!-- 계약체결일시 (시작일) -->
        <div class="d-flex justify-content-between align-items-center">
          <span class="text-secondary micro-text">계약체결일시</span>
          <span class="fw-bold text-dark micro-text">{{
            resultData?.startDate
          }}</span>
        </div>
      </div>

      <!-- 하단 계좌비밀번호 변경 안내 버튼 -->
      <!-- <button
        class="btn btn-outline-secondary w-100 py-2 btn-sm rounded-3 fw-semibold my-1"
        style="border-color: #ddd"
      >
        계좌비밀번호 변경
      </button> -->

      <!-- 최종 확인 버튼 (메인으로 이동) -->
      <button
        class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark mt-2"
        @click="router.push('/home')"
      >
        확인
      </button>
    </div>
    <CustomAlertModal
      :show="alertState.show"
      :message="alertState.message"
      @close="hideAlert"
    />
  </div>
</template>

<style scoped>
.micro-text {
  font-size: 13px;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 16px;
}
.back-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #111;
  padding: 0;
  cursor: pointer;
}
.header-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
}
</style>
