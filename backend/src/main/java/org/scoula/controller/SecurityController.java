package org.scoula.controller;

import lombok.extern.log4j.Log4j2;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.user.domain.UserVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Log4j2
@RequestMapping("/api/security")
@RestController
public class SecurityController {
    @GetMapping("/all")
    public ResponseEntity<String> doAll() {
        log.info("do all can access everybody");
        return ResponseEntity.ok("All can access everybody"); }

    @GetMapping("/member")
    public ResponseEntity<String> doMember(Authentication authentication) {
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        log.info("username = " + userDetails.getUsername());
        return ResponseEntity.ok(userDetails.getUsername()); }

    @GetMapping("/admin")
    public ResponseEntity<UserVO> doAdmin(@AuthenticationPrincipal CustomUser customUser) {
        UserVO member = customUser.getUserVO();
        log.info("username = " + member);
        return ResponseEntity.ok(member); }


//    @GetMapping("/all") // 모두 접근 가능
//    public void doAll() {
//        log.info("do all can access everybody");
//    }
//
//    // 직접 꺼내는 방법
////    @GetMapping("/member") // MEMBER 또는 ADMIN 권한 필요
////    public void doMember() {
////        log.info("logined member");
////    }
////
////    @GetMapping("/admin") // ADMIN 권한 필요
////    public void doAdmin() {
////        log.info("admin only");
////    }
//
//    @GetMapping("/login")
//    public void login() {
//        log.info("login page");
//    }
//
//    @GetMapping("/logout")
//    public void logout() {
//        log.info("logout page");
//    }
//
//    //post가 없지만 알아서 해줌
//
//    //유저 아이디만 꺼낼수 있음
////    @GetMapping("/member")
////    public void doMember(Principal principal) {
////        log.info("username = " + principal.getName()); }
////
//     //유저 네임, 비번, 언한 꺼낼 수 있음. 수동 형변환 필요함
//    @GetMapping("/member") public void doMember(Authentication authentication) {
//        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
//        log.info("username = " + userDetails.getUsername());
//    }
//
//    // 자동 형변환 됨. 이름 이메일 등 db 회원 전체정보를 한줄로 꺼낼 수 있음. 가장 많이 씀.
//    @GetMapping("/admin") public void doAdmin(@AuthenticationPrincipal CustomUser customUser) {
//        MemberVO member = customUser.getMember();
//        log.info("username = " + member);
//    }


}