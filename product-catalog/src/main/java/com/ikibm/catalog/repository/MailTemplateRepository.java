package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.MailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailTemplateRepository extends JpaRepository<MailTemplate, Integer> {

    List<MailTemplate> findAllByOrderByUpdatedAtDesc();

    List<MailTemplate> findByNameContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrderByUpdatedAtDesc(
            String name, String subject);
}
