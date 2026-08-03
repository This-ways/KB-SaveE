package org.scoula.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import org.scoula.security.filter.AuthenticationErrorFilter;
import org.scoula.security.filter.JwtAuthenticationFilter;
import org.scoula.security.handler.CustomAccessDeniedHandler;
import org.scoula.security.handler.CustomAuthenticationEntryPoint;
import org.scoula.security.handler.JwtUsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.CharacterEncodingFilter;

@Configuration
@EnableWebSecurity
@Log4j2
@MapperScan(basePackages = {"org.scoula.security.account.mapper"})
@ComponentScan(basePackages = {"org.scoula.security"})
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final AuthenticationErrorFilter authenticationErrorFilter;

    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;


    @Autowired
    private JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter;


    //패스워드 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // 문자셋 필터
    public CharacterEncodingFilter encodingFilter() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return encodingFilter;
    }

//    //페이지 접근 권한, 로그인 로그아웃 정책 설정
//    @Override
//    public void configure(HttpSecurity http) throws Exception {
//        http.addFilterBefore(encodingFilter(), CsrfFilter.class); //CsrfFilter 전 인코딩 필터 실행시켜서 한글깨짐 방지
//
//        http.authorizeRequests()
//                .antMatchers("/security/all").permitAll() //모두 허용
//                //특정 역할에게만 허용
//                .antMatchers("/security/admin").access("hasRole('ROLE_ADMIN')")
//                .antMatchers("/security/member").access("hasAnyRole('ROLE_MEMBER', 'ROLE_ADMIN')");
//
    ////        http.formLogin(); // form 기반 로그인 활성화, 나머지는 모두 디폴트
//        http.formLogin()
//                .loginPage("/security/login") //get 로그인 시 지금 보여주는 페이지
//                .loginProcessingUrl("/security/login") //post 아이디 패스워드 누르고 로그인 버튼 눌렀을때 처리해주는
//                .defaultSuccessUrl("/"); //로그인 성공했을때
//
//        http.logout()// 로그아웃 설정 시작
//                .logoutUrl("/security/logout") // POST: 로그아웃 호출 url
//                .invalidateHttpSession(true) // 세션 invalidate
//                .deleteCookies("remember-me", "JSESSION-ID") // 삭제할 쿠키 목록
//                .logoutSuccessUrl("/security/logout"); // GET: 로그아웃 이후 이동할 페이지
//
//    }


    //사용자 정보 어디서 가져올지
    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {
        // - in memory인 경우
        log.info("configure .........................................");
//        auth.inMemoryAuthentication()
//                .withUser("admin")
//                //.password("{noop}1234")
//                .password("$2a$10$.AYH7ZJXoCMivWvarkiLcezpbVB/8HnLihXJjUV7idK9bR4Ljbj2q")
//                .roles("ADMIN","MEMBER"); // ROLE_ADMIN
//        auth.inMemoryAuthentication()
//                .withUser("member")
//                //.password("{noop}1234")
//                .password("$2a$10$9q7hhf7YXgQQCdKVlsCKeOaqhdIm5JPZEHgYZrBbBqKEFuo1GCu0i")
//                .roles("MEMBER"); // ROLE_MEMBER


        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());


    }

//    // 💡 1. 여기에 @Lazy 어노테이션을 추가합니다. (순환 참조 방지)
//    @Autowired
//    @Lazy
//    private JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter;

    // 💡 2. 맨 아래나 적절한 위치에 이 메서드를 그대로 복사해서 붙여넣습니다. (매니저 빈 등록)
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/assets/**",
                "/*",
//                "/api/member/**",
// Swagger 관련 url은 보안에서 제외
                "/swagger-ui.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs"
        ); }


    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 한글 인코딩 필터 설정
        http.addFilterBefore(encodingFilter(), CsrfFilter.class)
                // 인증 에러 필터
                .addFilterBefore(authenticationErrorFilter, UsernamePasswordAuthenticationFilter.class)
                // Jwt 인증 필터
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 로그인 인증 필터
                .addFilterBefore(jwtUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 예외 처리 설정
        http
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);


        http
                .authorizeRequests() // 경로별 접근 권한 설정
                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .antMatchers(HttpMethod.PUT,"/api/member", "/api/member/*/changepassword").authenticated()
                .antMatchers(HttpMethod.POST, "/api/board/**").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/board/**").authenticated()
                .antMatchers(HttpMethod.DELETE, "/api/board/**").authenticated()
                .antMatchers(HttpMethod.POST, "/api/users/signup").permitAll()   // 회원가입은 로그인 전 접근
                .antMatchers("/api/users/mydata/**").authenticated()
                .antMatchers("/api/goals/**").authenticated()
                .antMatchers("/api/deposit-account/**").authenticated()
                .anyRequest().permitAll();


//                .antMatchers("/api/security/all").permitAll() // 모두 허용
//                .antMatchers("/api/security/member").access("hasRole('ROLE_MEMBER')") // ROLE_MEMBER 이상 접근 허용
//                .antMatchers("/api/security/admin").access("hasRole('ROLE_ADMIN')")  // ROLE_ADMIN 이상 접근 허용
//                .anyRequest().authenticated();  // 나머지는 로그인 된 경우 모두 허용



        http.httpBasic().disable() // 기본 HTTP 인증 비활성화
                .csrf().disable()       // CSRF 비활성화
                .formLogin().disable()  // formLogin 비활성화 관련 필터 해제
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 세션 생성 모드 설정





    }


}