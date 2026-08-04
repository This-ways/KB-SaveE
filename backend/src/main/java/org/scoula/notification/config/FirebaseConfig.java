package org.scoula.notification.config;

import com.google.auth.oauth2.GoogleCredentials;                  // 서비스 계정 키로 인증 정보 생성
import com.google.firebase.FirebaseApp;                           // Firebase 앱 인스턴스 (전역 싱글톤)
import com.google.firebase.FirebaseOptions;                       // Firebase 초기화 옵션
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;             // resources 폴더의 파일 읽기

import javax.annotation.PostConstruct;                            // 빈 생성 직후 1회 실행
import java.io.InputStream;

@Log4j2
@Configuration
public class FirebaseConfig {

    // 서비스 계정 키 파일명 (src/main/resources 바로 아래에 위치, .gitignore 처리됨)
    private static final String KEY_PATH = "firebase-serviceAccountKey.json";

    // 서버 기동 시 Firebase 앱을 딱 한 번만 초기화
    // FirebaseApp은 전역 싱글톤이라 중복 초기화 시 예외 발생 -> getApps()로 먼저 확인
    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp 이미 초기화됨 - 스킵");
                return;
            }

            InputStream keyStream = new ClassPathResource(KEY_PATH).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(keyStream))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp 초기화 완료");

        } catch (Exception e) {
            // 키 파일은 개인 발급이라 팀원 환경에는 없음
            // 예외를 삼켜서 푸시 발송만 실패하고 서버는 정상 기동되도록 함
            log.warn("FirebaseApp 초기화 실패 - FCM 푸시 발송 불가 (키 파일 없으면 정상): {}", e.getMessage());
        }
    }
}