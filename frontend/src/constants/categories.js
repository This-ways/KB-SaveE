// 카테고리별 아이콘 / 컬러 (팀 디자인 확정본)
// category_id는 DB 기준 1~13 고정이라 키로 그대로 사용
export const CATEGORY_STYLE = {
  1: { icon: 'fa-solid fa-utensils', color: '#4a90d9' }, // 식비
  2: { icon: 'fa-solid fa-mug-hot', color: '#8b5e3c' }, // 카페/간식
  3: { icon: 'fa-solid fa-cart-shopping', color: '#26a69a' }, // 온라인쇼핑
  4: { icon: 'fa-solid fa-shirt', color: '#ec6d9c' }, // 패션/쇼핑
  5: { icon: 'fa-solid fa-film', color: '#8e6fce' }, // 문화/여가
  6: { icon: 'fa-solid fa-martini-glass', color: '#f0a830' }, // 술/유흥
  7: { icon: 'fa-solid fa-bus', color: '#5cb85c' }, // 교통
  8: { icon: 'fa-solid fa-house', color: '#5b9bd5' }, // 생활
  9: { icon: 'fa-solid fa-wand-magic-sparkles', color: '#e05a9e' }, // 뷰티
  10: { icon: 'fa-solid fa-book', color: '#5a6472' }, // 교육
  11: { icon: 'fa-solid fa-paw', color: '#a97c50' }, // 반려동물
  12: { icon: 'fa-solid fa-plane', color: '#4a90d9' }, // 여행
  13: { icon: 'fa-solid fa-ellipsis', color: '#4a90d9' }, // 기타
};

// 알 수 없는 categoryId가 와도 화면이 깨지지 않도록 기본값 제공
export const DEFAULT_STYLE = { icon: 'fa-solid fa-ellipsis', color: '#9ca3af' };

export function getCategoryStyle(categoryId) {
  return CATEGORY_STYLE[categoryId] ?? DEFAULT_STYLE;
}

// SaveE 브랜드 컬러
export const BRAND = {
  primary: '#FFBC00', // 메인 노랑 (선택 상태, 주요 버튼)
  danger: '#EF4444', // 예산 초과 경고 전용
  gray: '#9CA3AF', // 보조 액션 (새로고침 등)
};
