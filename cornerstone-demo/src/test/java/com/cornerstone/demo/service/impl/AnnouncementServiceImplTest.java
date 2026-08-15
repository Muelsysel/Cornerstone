package com.cornerstone.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.demo.domain.Announcement;
import com.cornerstone.demo.mapper.AnnouncementMapper;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** 公告服务单测：状态单向流转（草稿→已发布→已下线）、作者自动填充、必填校验。 */
class AnnouncementServiceImplTest {

    private AnnouncementServiceImpl service;
    private AnnouncementMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = mock(AnnouncementMapper.class);
        AnnouncementServiceImpl impl = new AnnouncementServiceImpl();
        Field field =
                com.baomidou.mybatisplus.spring.repository.CrudRepository.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(impl, mapper);
        service = Mockito.spy(impl);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    private void loginAs(String username) {
        UserContext ctx = new UserContext();
        ctx.setUsername(username);
        UserContextHolder.set(ctx);
    }

    private Announcement announcement(Long id, Integer status) {
        Announcement a = new Announcement();
        a.setId(id);
        a.setTitle("公告");
        a.setStatus(status);
        return a;
    }

    @Test
    void createRejectsBlankTitle() {
        loginAs("admin");
        Announcement a = announcement(null, null);
        a.setTitle(" ");
        assertThatThrownBy(() -> service.create(a)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createForcesDraftAndFillsAuthor() {
        loginAs("admin");
        Announcement a = announcement(null, null);
        a.setTitle("新公告");

        Announcement created = service.create(a);

        assertThat(created.getStatus()).isEqualTo(0);
        assertThat(created.getAuthor()).isEqualTo("admin");
        verify(mapper).insert(any(Announcement.class));
    }

    @Test
    void createAnonymousKeepsAuthorBlank() {
        Announcement a = announcement(null, null);
        a.setTitle("匿名创建（正常业务不会出现，防御兜底）");
        service.create(a);
        assertThat(a.getAuthor()).isNull();
    }

    @Test
    void updateRejectsNonDraft() {
        doReturn(announcement(1L, 1)).when(service).getById(1L);
        Announcement patch = announcement(1L, null);
        patch.setTitle("改标题");
        assertThatThrownBy(() -> service.update(patch)).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateDraftWorks() {
        doReturn(announcement(1L, 0)).when(service).getById(1L);
        Announcement patch = announcement(1L, null);
        patch.setTitle("新标题");
        service.update(patch);
        verify(mapper).updateById(any(Announcement.class));
    }

    @Test
    void publishOnlyFromDraft() {
        doReturn(announcement(1L, 0)).when(service).getById(1L);
        service.publish(1L);
        // 状态由服务内部更新后落库：捕获 updateById 参数验证
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getPublishTime()).isNotNull();

        doReturn(announcement(2L, 2)).when(service).getById(2L);
        assertThatThrownBy(() -> service.publish(2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void offlineOnlyFromPublished() {
        doReturn(announcement(1L, 1)).when(service).getById(1L);
        service.offline(1L);
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(2);

        doReturn(announcement(2L, 0)).when(service).getById(2L);
        assertThatThrownBy(() -> service.offline(2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteRejectsMissing() {
        // 不 stub getById：走真实实现（baseMapper 返回 null → 抛不存在异常）
        when(mapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
        verify(mapper, never()).deleteById(1L);
    }
}
