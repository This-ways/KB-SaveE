package org.scoula.board.mapper;

import org.scoula.board.domain.BoardAttachmentVO;
import org.scoula.board.domain.BoardVO;
import org.scoula.common.pagination.PageRequest;

import java.util.List;

public interface BoardMapper {
    int getTotalCount();
    List<BoardVO> getPage(PageRequest pageRequest);

    public void create(BoardVO member);

    public List<BoardVO> getList();

    public BoardVO get(Long no);

    public int update(BoardVO member);

    public int delete(Long no);

    public void createAttachment(BoardAttachmentVO attach);
    public List<BoardAttachmentVO> getAttachmentList(Long bno);
    public BoardAttachmentVO getAttachment(Long no);
    public int deleteAttachment(Long no);
}
