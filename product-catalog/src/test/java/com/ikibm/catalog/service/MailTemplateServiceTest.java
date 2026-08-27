package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.MailTemplateRequest;
import com.ikibm.catalog.entity.MailTemplate;
import com.ikibm.catalog.entity.MailTemplateStatus;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.MailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailTemplateServiceTest {

    @Mock private MailTemplateRepository repository;
    @Mock private StorageService storageService;

    private MailTemplateService service;

    @BeforeEach
    void setUp() {
        service = new MailTemplateService(repository, storageService);
        lenient().when(repository.save(any(MailTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MailTemplateRequest request(String name) {
        return new MailTemplateRequest(name, "Konu", "Başlık", "Metin", null, null, null);
    }

    @Test
    void list_returnsAllOrderedByUpdatedAtDesc() {
        MailTemplate t = new MailTemplate();
        when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(t));

        assertThat(service.list()).containsExactly(t);
    }

    @Test
    void get_throwsNotFoundWhenMissing() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_setsDefaultStatusWhenNull() {
        MailTemplate created = service.create(request("Kampanya"));

        assertThat(created.getName()).isEqualTo("Kampanya");
        assertThat(created.getStatus()).isEqualTo(MailTemplateStatus.ACTIVE);
    }

    @Test
    void create_keepsExplicitStatus() {
        MailTemplateRequest req = new MailTemplateRequest("Ad", "Konu", "Başlık", "Metin", null, null, MailTemplateStatus.ARCHIVED);

        MailTemplate created = service.create(req);

        assertThat(created.getStatus()).isEqualTo(MailTemplateStatus.ARCHIVED);
    }

    @Test
    void update_appliesChangesToExisting() {
        MailTemplate existing = new MailTemplate();
        existing.setId(1);
        existing.setName("Eski Ad");
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        MailTemplate updated = service.update(1, request("Yeni Ad"));

        assertThat(updated.getName()).isEqualTo("Yeni Ad");
        assertThat(updated.getId()).isEqualTo(1);
    }

    @Test
    void update_throwsNotFoundWhenMissing() {
        when(repository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5, request("x"))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_removesExisting() {
        MailTemplate existing = new MailTemplate();
        existing.setId(3);
        when(repository.findById(3)).thenReturn(Optional.of(existing));

        service.delete(3);

        verify(repository).delete(existing);
    }

    @Test
    void delete_throwsNotFoundWhenMissing() {
        when(repository.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).delete(any(MailTemplate.class));
    }

    @Test
    void search_blankQuery_returnsFullList() {
        when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(new MailTemplate()));

        assertThat(service.search("  ")).hasSize(1);
    }

    @Test
    void search_filtersByNameOrSubject() {
        MailTemplate t = new MailTemplate();
        when(repository.findByNameContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrderByUpdatedAtDesc("kampanya", "kampanya"))
                .thenReturn(List.of(t));

        assertThat(service.search("kampanya")).containsExactly(t);
    }

    @Test
    void setImage_uploadsViaStorageServiceAndClearsCampaignSelection() {
        MailTemplate existing = new MailTemplate();
        existing.setId(4);
        existing.setCampaignImageUrl("/uploads/campaigns/ups-guardian/x.jpg");
        when(repository.findById(4)).thenReturn(Optional.of(existing));
        when(storageService.uploadMailImage(eq(4), any(byte[].class), any(), any()))
                .thenReturn(new StorageService.Uploaded("mail/4/uuid-banner.jpg", "/uploads/mail/4/uuid-banner.jpg"));

        MailTemplate updated = service.setImage(4, new byte[]{1, 2, 3}, "image/jpeg", "banner.jpg");

        assertThat(updated.getImageUrl()).isEqualTo("/uploads/mail/4/uuid-banner.jpg");
        assertThat(updated.getImageKey()).isEqualTo("mail/4/uuid-banner.jpg");
        assertThat(updated.getCampaignImageUrl()).isNull();
    }

    @Test
    void setImage_deletesPreviousFileWhenReplacing() {
        MailTemplate existing = new MailTemplate();
        existing.setId(4);
        existing.setImageKey("mail/4/old-uuid-old.jpg");
        when(repository.findById(4)).thenReturn(Optional.of(existing));
        when(storageService.uploadMailImage(eq(4), any(byte[].class), any(), any()))
                .thenReturn(new StorageService.Uploaded("mail/4/new-uuid-new.jpg", "/uploads/mail/4/new-uuid-new.jpg"));

        service.setImage(4, new byte[]{1}, "image/png", "new.png");

        verify(storageService).deleteObject("mail/4/old-uuid-old.jpg");
    }

    @Test
    void update_selectingCampaignImage_clearsAndDeletesOwnUploadedImage() {
        MailTemplate existing = new MailTemplate();
        existing.setId(1);
        existing.setImageKey("mail/1/uuid-old.jpg");
        existing.setImageUrl("/uploads/mail/1/uuid-old.jpg");
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        MailTemplateRequest req = new MailTemplateRequest("Ad", "Konu", "Başlık", "Metin", null,
                "/uploads/campaigns/ups-guardian/x.jpg", null);

        MailTemplate updated = service.update(1, req);

        assertThat(updated.getImageUrl()).isNull();
        assertThat(updated.getImageKey()).isNull();
        assertThat(updated.getCampaignImageUrl()).isEqualTo("/uploads/campaigns/ups-guardian/x.jpg");
        verify(storageService).deleteObject("mail/1/uuid-old.jpg");
    }

    @Test
    void update_removingImage_deletesOwnUploadedFile() {
        MailTemplate existing = new MailTemplate();
        existing.setId(1);
        existing.setImageKey("mail/1/uuid-old.jpg");
        existing.setImageUrl("/uploads/mail/1/uuid-old.jpg");
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        service.update(1, request("Ad")); // request() imageUrl=null, campaignImageUrl=null

        verify(storageService).deleteObject("mail/1/uuid-old.jpg");
    }

    @Test
    void delete_removesOwnUploadedFileFromStorage() {
        MailTemplate existing = new MailTemplate();
        existing.setId(9);
        existing.setImageKey("mail/9/uuid-x.jpg");
        when(repository.findById(9)).thenReturn(Optional.of(existing));

        service.delete(9);

        verify(storageService).deleteObject("mail/9/uuid-x.jpg");
        verify(repository).delete(existing);
    }

    @Test
    void delete_withNoImage_doesNotCallStorage() {
        MailTemplate existing = new MailTemplate();
        existing.setId(10);
        when(repository.findById(10)).thenReturn(Optional.of(existing));

        service.delete(10);

        verify(storageService, never()).deleteObject(any());
    }
}
