package org.scoula.peerstat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.peerstat.dto.CategoryCompareDTO;
import org.scoula.peerstat.mapper.PeerStatMapper;
import org.scoula.peerstat.util.AgeGroupUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class PeerStatServiceImpl implements PeerStatService {

    private final PeerStatMapper mapper;

    @Override
    public List<CategoryCompareDTO> compareCategories(Long userId, String yearMonth) {
        String birthDate = mapper.getBirthDate(userId);
        String ageGroup = AgeGroupUtil.resolve(birthDate);
        log.debug("userId={} birthDate={} -> ageGroup={}", userId, birthDate, ageGroup);

        return mapper.compareCategories(userId, ageGroup, yearMonth);
    }
}
