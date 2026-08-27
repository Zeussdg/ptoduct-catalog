package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Diske gerçekten yazan/silen davranışı @TempDir ile doğrular — Spring context'i gerekmez. */
class StorageServiceTest {

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03};
    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] NOT_AN_IMAGE = "this is not an image, just text".getBytes();

    @TempDir
    Path tempDir;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getUpload().setDir(tempDir.toString());
        storageService = new StorageService(props);
        invokeInit();
    }

    private void invokeInit() {
        try {
            Method init = StorageService.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(storageService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void uploadMailImage_validJpeg_writesFileUnderMailSubfolder() {
        StorageService.Uploaded up = storageService.uploadMailImage(5, JPEG_BYTES, "image/jpeg", "banner.jpg");

        assertThat(up.key()).startsWith("mail/5/");
        assertThat(up.url()).isEqualTo("/uploads/" + up.key());
        assertThat(Files.exists(tempDir.resolve(up.key()))).isTrue();
    }

    @Test
    void uploadMailImage_validPng_succeeds() {
        StorageService.Uploaded up = storageService.uploadMailImage(5, PNG_BYTES, "image/png", "banner.png");

        assertThat(Files.exists(tempDir.resolve(up.key()))).isTrue();
    }

    @Test
    void uploadMailImage_rejectsDisallowedExtension() {
        assertThatThrownBy(() -> storageService.uploadMailImage(5, JPEG_BYTES, "image/jpeg", "malware.exe"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("uzantı");
    }

    @Test
    void uploadMailImage_rejectsDisallowedMimeType() {
        assertThatThrownBy(() -> storageService.uploadMailImage(5, JPEG_BYTES, "application/octet-stream", "banner.jpg"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("tür");
    }

    @Test
    void uploadMailImage_rejectsRenamedExecutable_contentDoesNotMatchExtension() {
        // "malware.exe"nin ".jpg" olarak yeniden adlandırılması senaryosu: uzantı ve MIME type
        // istemci tarafından doğru bildirilmiş olsa bile gerçek baytlar bir görsel imzası taşımıyor.
        assertThatThrownBy(() -> storageService.uploadMailImage(5, NOT_AN_IMAGE, "image/jpeg", "banner.jpg"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("içeriği");
    }

    @Test
    void uploadMailImage_rejectsEmptyFile() {
        assertThatThrownBy(() -> storageService.uploadMailImage(5, new byte[0], "image/jpeg", "banner.jpg"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("boş");
    }

    @Test
    void deleteObject_removesFileFromDisk() {
        StorageService.Uploaded up = storageService.uploadMailImage(5, JPEG_BYTES, "image/jpeg", "banner.jpg");
        assertThat(Files.exists(tempDir.resolve(up.key()))).isTrue();

        storageService.deleteObject(up.key());

        assertThat(Files.exists(tempDir.resolve(up.key()))).isFalse();
    }

    @Test
    void deleteObject_pathTraversalAttempt_isIgnoredWithoutEscapingRoot() throws IOException {
        // Kök dizinin dışında bir "kanıt" dosyası oluşturup, deleteObject'e path traversal
        // denemesi (../) ile bu dosyayı silmeye çalışıyoruz — silinmemesi gerekiyor.
        Path outsideFile = tempDir.getParent().resolve("outside-secret.txt");
        Files.writeString(outsideFile, "should not be deleted");
        try {
            storageService.deleteObject("../outside-secret.txt");

            assertThat(Files.exists(outsideFile)).isTrue();
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    @Test
    void uploadMailImage_filenameWithPathSeparators_isSanitizedNotTraversed() {
        StorageService.Uploaded up = storageService.uploadMailImage(5, JPEG_BYTES, "image/jpeg", "../../etc/passwd.jpg");

        // Sanitize edilen dosya adı sadece [a-zA-Z0-9._-] içerir — "/" ve ".." kaybolur,
        // dosya her zaman mail/{id}/ altında, root'un dışına çıkmadan oluşturulur.
        assertThat(up.key()).startsWith("mail/5/");
        assertThat(Files.exists(tempDir.resolve(up.key()))).isTrue();
        assertThat(tempDir.resolve(up.key()).normalize().startsWith(tempDir)).isTrue();
    }
}
