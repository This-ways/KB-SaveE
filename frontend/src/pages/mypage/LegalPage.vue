<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 열려 있는 항목 (아코디언)
const openKey = ref(null);
const toggle = (key) => (openKey.value = openKey.value === key ? null : key);
</script>

<template>
  <div class="legal-page">
    <div class="header">
      <button class="back-btn" @click="router.push('/mypage')">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <h1 class="header-title">법적 정보</h1>
    </div>

    <p class="notice">
      본 서비스는 학습용 프로젝트로, 아래 내용은 실제 법적 효력이 없는 예시입니다.
    </p>

    <div class="group">
      <!-- 개인정보 처리방침 -->
      <button class="row" @click="toggle('privacy')">
        <span class="row-label">개인정보 처리방침</span>
        <i
          class="fa-solid arrow"
          :class="openKey === 'privacy' ? 'fa-chevron-up' : 'fa-chevron-down'"
        ></i>
      </button>
      <div v-if="openKey === 'privacy'" class="content">
        <h3>1. 수집하는 개인정보 항목</h3>
        <p>
          회원가입 시 아이디, 비밀번호, 이름, 생년월일을 수집합니다.
          계좌 연결에 동의한 경우 거래내역과 잔액 정보를 함께 수집합니다.
        </p>

        <h3>2. 개인정보의 이용 목적</h3>
        <p>
          수집한 정보는 회원 식별과 로그인, 소비 분석 및 지출 관리 기능 제공,
          적금 상품 추천에만 사용합니다. 그 외의 목적으로는 이용하지 않습니다.
        </p>

        <h3>3. 보유 및 이용 기간</h3>
        <p>
          회원 탈퇴 시 지체 없이 파기합니다. 다만 관계 법령에 따라 보존이
          필요한 정보는 해당 기간 동안 분리 보관합니다.
        </p>

        <h3>4. 제3자 제공</h3>
        <p>
          이용자의 동의 없이 개인정보를 외부에 제공하지 않습니다.
          법령에 따른 요청이 있는 경우에만 예외적으로 제공될 수 있습니다.
        </p>

        <h3>5. 이용자의 권리</h3>
        <p>
          언제든지 본인의 개인정보를 조회하거나 수정할 수 있으며,
          수집·이용 동의를 철회하고 회원 탈퇴를 요청할 수 있습니다.
        </p>
      </div>

      <!-- 서비스 이용약관 -->
      <button class="row" @click="toggle('terms')">
        <span class="row-label">서비스 이용약관</span>
        <i
          class="fa-solid arrow"
          :class="openKey === 'terms' ? 'fa-chevron-up' : 'fa-chevron-down'"
        ></i>
      </button>
      <div v-if="openKey === 'terms'" class="content">
        <h3>제1조 (목적)</h3>
        <p>
          본 약관은 SaveE(이하 "서비스")의 이용 조건과 절차, 이용자와 서비스의
          권리·의무를 정하는 것을 목적으로 합니다.
        </p>

        <h3>제2조 (서비스의 내용)</h3>
        <p>
          서비스는 이용자의 소비 내역을 분석하여 카테고리별 지출 관리를 돕고,
          절약한 금액을 기반으로 적금 상품을 안내합니다.
        </p>

        <h3>제3조 (이용자의 의무)</h3>
        <p>
          이용자는 타인의 정보를 도용하거나 서비스의 정상적인 운영을 방해하는
          행위를 해서는 안 됩니다.
        </p>

        <h3>제4조 (서비스의 변경 및 중단)</h3>
        <p>
          서비스는 운영상 필요에 따라 내용을 변경하거나 일시 중단할 수 있으며,
          중요한 변경은 사전에 공지합니다.
        </p>

        <h3>제5조 (면책)</h3>
        <p>
          본 서비스가 제공하는 소비 분석과 적금 안내는 참고 정보이며,
          이용자의 금융 의사결정에 대한 책임은 이용자 본인에게 있습니다.
        </p>
      </div>

      <!-- 전자금융거래 이용약관 -->
      <button class="row" @click="toggle('efin')">
        <span class="row-label">전자금융거래 이용약관</span>
        <i
          class="fa-solid arrow"
          :class="openKey === 'efin' ? 'fa-chevron-up' : 'fa-chevron-down'"
        ></i>
      </button>
      <div v-if="openKey === 'efin'" class="content">
        <h3>제1조 (적용 범위)</h3>
        <p>
          본 약관은 서비스를 통해 이루어지는 적금 가입, 자동이체 등
          전자금융거래에 적용됩니다.
        </p>

        <h3>제2조 (자동이체)</h3>
        <p>
          이용자가 지정한 납입일에 연결된 예금 계좌에서 약정 금액이 출금됩니다.
          납입일이 영업일이 아닌 경우 다음 영업일에 처리됩니다.
        </p>

        <h3>제3조 (잔액 부족)</h3>
        <p>
          출금 시점에 잔액이 부족하면 해당 회차의 이체는 실패로 처리되며,
          이용자에게 알림이 발송됩니다.
        </p>

        <h3>제4조 (거래 기록의 보관)</h3>
        <p>
          전자금융거래 기록은 관계 법령에서 정한 기간 동안 보관합니다.
        </p>
      </div>

      <!-- 오픈소스 라이선스 -->
      <button class="row" @click="toggle('oss')">
        <span class="row-label">오픈소스 라이선스</span>
        <i
          class="fa-solid arrow"
          :class="openKey === 'oss' ? 'fa-chevron-up' : 'fa-chevron-down'"
        ></i>
      </button>
      <div v-if="openKey === 'oss'" class="content">
        <p>본 서비스는 다음 오픈소스를 사용합니다.</p>
        <ul>
          <li>Vue.js — MIT License</li>
          <li>Vue Router, Pinia — MIT License</li>
          <li>Bootstrap — MIT License</li>
          <li>Font Awesome Free — CC BY 4.0 / SIL OFL 1.1 / MIT</li>
          <li>Pretendard — SIL Open Font License 1.1</li>
          <li>axios, moment — MIT License</li>
          <li>Spring Framework, MyBatis — Apache License 2.0</li>
        </ul>
      </div>
    </div>

    <p class="version">KB SaveE v1.0.0</p>
  </div>
</template>

<style scoped>
.legal-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 76px 16px 40px;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
  background: #f7f8fa;
}
.back-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #111;
  padding: 0;
}
.header-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
}
.notice {
  font-size: 12px;
  color: #9ca3af;
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.group {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 16px;
  background: none;
  border: none;
  border-bottom: 1px solid #f4f5f7;
  text-align: left;
  cursor: pointer;
}
.row-label {
  flex: 1 1 0;
  font-size: 14px;
  font-weight: 500;
  color: #111;
}
.arrow {
  font-size: 12px;
  color: #d1d5db;
}
.content {
  padding: 4px 16px 20px;
  border-bottom: 1px solid #f4f5f7;
}
.content h3 {
  font-size: 13px;
  font-weight: 700;
  color: #374151;
  margin: 16px 0 6px;
}
.content h3:first-child {
  margin-top: 8px;
}
.content p {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0;
}
.content ul {
  margin: 8px 0 0;
  padding-left: 18px;
}
.content li {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.9;
}

.version {
  text-align: center;
  font-size: 12px;
  color: #c8ccd1;
  margin-top: 20px;
}
</style>