package org.scoula.report.service;

import org.scoula.report.dto.ReportDTO;

public interface ReportService {

    // 소비 리포트 화면 하나에 필요한 데이터 전체 조합
    ReportDTO getReport(Long userId, String yearMonth);
}
