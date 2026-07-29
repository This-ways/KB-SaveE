<div align="center">

# 🐝 KB SaveE

### 아낀 돈이 목돈이 되는 습관

KB IT's Your Life 7기 종합실무 파이널 프로젝트 · 팀 **SAFA**

[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](#)
[![Spring](https://img.shields.io/badge/Spring%20Framework-5.3.37-6DB33F?logo=spring&logoColor=white)](#)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.4.6-red)](#)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white)](#)
[![Vue.js](https://img.shields.io/badge/Vue.js-4FC08D?logo=vue.js&logoColor=white)](#)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](#)
[![JWT](https://img.shields.io/badge/Auth-JWT-black?logo=jsonwebtokens)](#)

</div>

---

## 📌 프로젝트 소개

**SaveE**는 소비 카테고리별 절약 목표를 설정하고, 아낀 금액을 KB 적금 상품으로 연결해주는
**2030세대 대상 소비관리 앱**입니다.

> Toss 등 기존 소비분석 서비스와 달리, **KB 전용 원스톱 적금 연결**과 **중도해지 방어**에 초점을 맞췄습니다.

| 🎯 핵심 기능 | 설명 |
|---|---|
| 카테고리별 예산 설정 | 매달 1~7일, 소비 카테고리별 절감 목표 설정 |
| 실시간 소진율 알림 | 예산 50/70/90% 도달 시 푸시 알림 |
| 소비 리포트 | 요일별·주차별 패턴, 또래 평균 비교, Top3 지출 |
| 적금 자동 추천 | 절약한 금액 기반 KB 적금 상품 추천 및 원스톱 연결 |
| 중도해지 방어 | 해지 시 손실 금액을 직관적으로 안내 |

---

## 👥 팀 SAFA

| 이름 | 역할 | 담당 도메인 |
|:---:|:---:|---|
| 🧑‍💼 김의겸 | 팀장 | 거래내역 `TRANSACTION` `CATEGORY` `PEER_STAT` |
| 🧑‍💻 강두형 | 팀원 | 사용자 `USER` `GOAL` `DEPOSIT_ACCOUNT` |
| 👩‍💻 최지혜 | 팀원 | 적금 `SAVINGS_PRODUCT` `SAVINGS_RATE` `SUBSCRIPTION` `PAYMENT` |
| 🧑‍💻 김민성 | 팀원 | 알림 `NOTIFICATION` `DEVICE_TOKEN` |

---

## 🛠 기술 스택

<table>
<tr>
<td><b>Frontend</b></td>
<td>Vue.js</td>
</tr>
<tr>
<td><b>Backend</b></td>
<td>Spring Framework 5.3.37 (Legacy) · MyBatis 3.4.6 · Spring Security 5.8.13 + JWT · Gradle</td>
</tr>
<tr>
<td><b>Database</b></td>
<td>MySQL · MongoDB</td>
</tr>
<tr>
<td><b>View</b></td>
<td>Servlet / JSP / JSTL</td>
</tr>
<tr>
<td><b>외부 연동</b></td>
<td>MyData(거래내역) · 금융감독원 API(적금상품·금리) · FCM(푸시알림) · KB 공식 홈페이지(계좌 개설)</td>
</tr>
</table>

---

## 📁 프로젝트 구조

```
backend/
└─ src/
   ├─ main/
   │  ├─ java/org/scoula/
   │  │   ├─ user/               사용자 · 목표 · 예금계좌
   │  │   │   ├─ controller/     UserController, GoalController, DepositAccountController
   │  │   │   ├─ domain/         UserVO, GoalVO, DepositAccountVO
   │  │   │   ├─ dto/
   │  │   │   ├─ mapper/
   │  │   │   └─ service/
   │  │   ├─ savings/            적금 상품 · 가입 · 납입
   │  │   │   ├─ controller/     SavingsProductController, SubscriptionController
   │  │   │   ├─ domain/         SavingsProductVO, SavingsRateVO, SubscriptionVO, PaymentVO
   │  │   │   ├─ dto/
   │  │   │   ├─ mapper/
   │  │   │   └─ service/
   │  │   ├─ transaction/        거래내역 · 카테고리 · 또래통계
   │  │   │   ├─ controller/     TransactionController, ReportController
   │  │   │   ├─ domain/         TransactionVO, CategoryVO, PeerStatVO
   │  │   │   ├─ dto/
   │  │   │   ├─ mapper/
   │  │   │   └─ service/
   │  │   ├─ notification/       알림 · 기기토큰
   │  │   │   ├─ controller/     NotificationController
   │  │   │   ├─ domain/         NotificationVO, DeviceTokenVO
   │  │   │   ├─ dto/
   │  │   │   ├─ mapper/
   │  │   │   └─ service/
   │  │   ├─ security/            인증/인가 (JWT)
   │  │   │   ├─ config/
   │  │   │   ├─ filter/
   │  │   │   └─ util/
   │  │   ├─ common/              공통 유틸 · 응답 래퍼
   │  │   ├─ config/              Spring/MyBatis 설정
   │  │   ├─ exception/           공통 예외 처리
   │  │   └─ handler/
   │  ├─ resources/org/scoula/    도메인별 MyBatis Mapper XML
   │  └─ webapp/WEB-INF/          web.xml 등 배포서술자
   └─ test/java/org/scoula/       도메인별 테스트 코드
```


---

## 🌿 브랜치 전략

```
main        발표/제출용 안정 버전
 └─ develop  팀 통합 개발 브랜치
     ├─ feature/user-*      강두형
     ├─ feature/savings-*   최지혜
     ├─ feature/txn-*       김의겸
     └─ feature/notify-*    김민성
```

---

<div align="center">
<sub>Made with 🐝 by Team SAFA</sub>
</div>
