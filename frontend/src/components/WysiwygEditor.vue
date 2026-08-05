<script setup>
/**
 * Toast UI Editor 래퍼 컴포넌트
 * - v-model="article.content" 형태로 사용
 * - HTML 결과물을 그대로 저장 (게시판 detail 페이지에서 v-html로 렌더링 필요)
 *
 * 설치 필요:
 *   npm install @toast-ui/editor
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import Editor from '@toast-ui/editor';
import '@toast-ui/editor/dist/toastui-editor.css';

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: String, default: '400px' },
  placeholder: { type: String, default: '내용을 입력하세요' },
});

const emit = defineEmits(['update:modelValue']);

const editorRef = ref(null);
let editorInstance = null;
let isSettingExternally = false;

onMounted(() => {
  editorInstance = new Editor({
    el: editorRef.value,
    height: props.height,
    initialEditType: 'wysiwyg', // WYSIWYG 모드로 시작 (마크다운 모드 원하면 'markdown')
    previewStyle: 'vertical',
    placeholder: props.placeholder,
    initialValue: props.modelValue || ' ', // 빈 문자열이면 placeholder 안 보이는 버그 방지
    hideModeSwitch: false, // WYSIWYG/마크다운 전환 탭 노출
    toolbarItems: [
      ['heading', 'bold', 'italic', 'strike'],
      ['hr', 'quote'],
      ['ul', 'ol', 'task', 'indent', 'outdent'],
      ['table', 'image', 'link'],
      ['code', 'codeblock'],
    ],
    events: {
      change: () => {
        if (isSettingExternally) return;
        // HTML 기준으로 저장 (detail 페이지에서 v-html 렌더링)
        emit('update:modelValue', editorInstance.getHTML());
      },
    },
  });
});

// 외부에서 modelValue가 바뀌는 경우(예: 수정 페이지에서 기존 글 로드) 에디터에 반영
watch(
  () => props.modelValue,
  (val) => {
    if (editorInstance && val !== editorInstance.getHTML()) {
      isSettingExternally = true;
      editorInstance.setHTML(val || '');
      isSettingExternally = false;
    }
  }
);

onBeforeUnmount(() => {
  editorInstance?.destroy();
});
</script>

<template>
  <div ref="editorRef"></div>
</template>
