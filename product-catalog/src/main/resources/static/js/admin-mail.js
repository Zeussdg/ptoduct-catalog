// Mail Gönderme (admin) — şablonlar gerçek backend'e (/admin/mail-templates) bağlı, görseller
// mevcut StorageService üzerinden gerçek dosya olarak (uploads/mail/...) yükleniyor — base64
// sadece dosya seçildiği an, kaydedilene kadarki anlık önizleme için kullanılıyor.
// Gerçek mail gönderimi hâlâ yapılmıyor; "Mail Gönder" pasif bırakılmıştır.
(function () {
  "use strict";

  var API_BASE = "/admin/mail-templates";
  var selectedImage = null;      // aktif mail görseli (önizleme: dataURL/kampanya URL'i/gerçek yüklenmiş URL)
  var selectedImageSource = null; // "upload" | "campaign" | null
  var pendingImageFile = null;    // seçilip henüz yüklenmemiş dosya — kaydedince /image endpoint'ine gönderilir
  var editingTemplateId = null;   // null = yeni şablon oluşturulacak, dolu = o şablon güncellenecek
  var searchDebounceTimer = null;

  document.addEventListener("DOMContentLoaded", function () {
    var root = document.querySelector(".adm__content");
    if (!root || !document.getElementById("mailTabCompose")) return; // bu sayfada değiliz

    // ---- CSRF (mutasyon isteklerinde gerekli — GET'ler etkilenmez) ----
    function csrfOnlyHeaders() {
      var token = document.querySelector('meta[name="_csrf"]');
      var header = document.querySelector('meta[name="_csrf_header"]');
      var headers = {};
      if (token && header && header.getAttribute("content")) {
        headers[header.getAttribute("content")] = token.getAttribute("content");
      }
      return headers;
    }

    function jsonHeaders() {
      var headers = csrfOnlyHeaders();
      headers["Content-Type"] = "application/json";
      return headers;
    }

    function apiFetch(url, options) {
      return fetch(url, options).then(function (res) {
        if (res.status === 204) return null;
        return res.json().catch(function () { return null; }).then(function (body) {
          if (!res.ok) {
            var message = (body && body.message) || "İstek başarısız oldu (" + res.status + ")";
            throw new Error(message);
          }
          return body;
        });
      });
    }

    // ---- Sekmeler ----
    var tabComposeBtn = document.getElementById("mailTabComposeBtn");
    var tabTemplatesBtn = document.getElementById("mailTabTemplatesBtn");
    var tabCompose = document.getElementById("mailTabCompose");
    var tabTemplates = document.getElementById("mailTabTemplates");

    function showTab(name) {
      var compose = name === "compose";
      tabCompose.hidden = !compose;
      tabTemplates.hidden = compose;
      tabComposeBtn.classList.toggle("adp__tab--active", compose);
      tabTemplatesBtn.classList.toggle("adp__tab--active", !compose);
      if (!compose) loadTemplateList();
    }
    tabComposeBtn.addEventListener("click", function () { showTab("compose"); });
    tabTemplatesBtn.addEventListener("click", function () { showTab("templates"); });

    // ---- Alıcılar ----
    // "Müşteri Grubu" seçeneği hâlâ örnek/mock veridir (gerçek müşteri grupları bu projede henüz
    // bir varlık olarak yok) — bu yüzden gönderim sırasında ayrıca engellenir (bkz. mailSendBtn).
    // "Tüm Müşteriler" / "Seçili Müşteriler" ise gerçek müşteri kayıtlarına bağlıdır.
    var recipRadios = document.querySelectorAll('input[name="mailRecipients"]');
    var groupBox = document.getElementById("mailGroupBox");
    var selectedBox = document.getElementById("mailSelectedBox");
    Array.prototype.forEach.call(recipRadios, function (r) {
      r.addEventListener("change", function () {
        groupBox.hidden = document.getElementById("mailRecipGroup").checked !== true;
        selectedBox.hidden = document.getElementById("mailRecipSelected").checked !== true;
      });
    });

    // ---- Gerçek müşteri listesi (Seçili Müşteriler kutusu için) ----
    var selectedCustomerListEl = document.getElementById("mailSelectedCustomerList");
    var selectedCustomerEmptyEl = document.getElementById("mailSelectedCustomerEmpty");

    function loadCustomers() {
      apiFetch("/admin/mail/customers", { method: "GET" })
        .then(function (customers) {
          renderCustomerCheckboxes(customers || []);
        })
        .catch(function () {
          selectedCustomerListEl.innerHTML = "";
          selectedCustomerEmptyEl.textContent = "Müşteri listesi yüklenemedi.";
          selectedCustomerEmptyEl.hidden = false;
        });
    }

    function renderCustomerCheckboxes(customers) {
      selectedCustomerListEl.innerHTML = "";
      selectedCustomerEmptyEl.hidden = customers.length > 0;
      customers.forEach(function (c) {
        var label = document.createElement("label");
        label.style.cssText = "display:flex;align-items:center;gap:6px;font-size:13px;color:var(--ink-900)";
        label.innerHTML = '<input type="checkbox" class="js-mail-customer" data-customer-id="' + c.id + '"/> '
          + esc(c.displayName) + ' — ' + esc(c.email);
        selectedCustomerListEl.appendChild(label);
      });
    }

    loadCustomers();

    // ---- Form alanları ----
    var subjectEl = document.getElementById("mailSubject");
    var titleEl = document.getElementById("mailTitle");
    var bodyEl = document.getElementById("mailBody");
    var imageInput = document.getElementById("mailImageInput");
    var imagePreview = document.getElementById("mailImagePreview");
    var imageRemoveBtn = document.getElementById("mailImageRemoveBtn");

    // ---- Görsel yükleme (kendi görselin) ----
    // Seçilen dosya HENÜZ sunucuya gönderilmez — sadece anlık önizleme için dataURL'e çevrilir.
    // Gerçek dosya, "Şablon Olarak Kaydet" tıklanınca /admin/mail-templates/{id}/image üzerinden
    // multipart/form-data ile yüklenir (bkz. uploadPendingImage).
    imageInput.addEventListener("change", function () {
      var file = imageInput.files && imageInput.files[0];
      if (!file) return;
      pendingImageFile = file;
      var reader = new FileReader();
      reader.onload = function (e) {
        setImage(e.target.result, "upload");
      };
      reader.readAsDataURL(file);
    });

    imageRemoveBtn.addEventListener("click", function () {
      imageInput.value = "";
      pendingImageFile = null;
      setImage(null, null);
    });

    // ---- Kampanya görseli seçimi ----
    var thumbButtons = document.querySelectorAll(".js-campaign-thumb");
    Array.prototype.forEach.call(thumbButtons, function (btn) {
      btn.addEventListener("click", function () {
        pendingImageFile = null;
        setImage(btn.getAttribute("data-image"), "campaign");
      });
    });

    function setImage(dataUrl, source) {
      selectedImage = dataUrl || null;
      selectedImageSource = selectedImage ? source : null;

      if (selectedImage) {
        imagePreview.src = selectedImage;
        imagePreview.hidden = false;
        imageRemoveBtn.hidden = false;
      } else {
        imagePreview.hidden = true;
        imagePreview.removeAttribute("src");
        imageRemoveBtn.hidden = true;
      }
      if (source !== "upload") imageInput.value = "";

      Array.prototype.forEach.call(thumbButtons, function (btn) {
        var isSelected = selectedImageSource === "campaign" && btn.getAttribute("data-image") === selectedImage;
        btn.classList.toggle("adp__thumb--selected", isSelected);
      });

      renderPreview();
    }

    // ---- Mail Önizleme ----
    var previewSubject = document.getElementById("mailPreviewSubject");
    var previewEmpty = document.getElementById("mailPreviewEmpty");
    var previewContent = document.getElementById("mailPreviewContent");
    var previewImage = document.getElementById("mailPreviewImage");
    var previewTitle = document.getElementById("mailPreviewTitle");
    var previewText = document.getElementById("mailPreviewText");

    function renderPreview() {
      var subject = subjectEl.value.trim();
      var title = titleEl.value.trim();
      var body = bodyEl.value.trim();

      previewSubject.textContent = subject || "—";

      var hasContent = subject || title || body || selectedImage;
      previewEmpty.hidden = !!hasContent;
      previewContent.hidden = !hasContent;
      if (!hasContent) return;

      if (selectedImage) {
        previewImage.src = selectedImage;
        previewImage.hidden = false;
      } else {
        previewImage.hidden = true;
        previewImage.removeAttribute("src");
      }
      previewTitle.textContent = title;
      previewTitle.hidden = !title;
      previewText.textContent = body;
    }

    [subjectEl, titleEl, bodyEl].forEach(function (el) {
      el.addEventListener("input", renderPreview);
    });
    document.getElementById("mailPreviewBtn").addEventListener("click", renderPreview);

    // ---- Temizle ----
    document.getElementById("mailClearBtn").addEventListener("click", function () {
      clearForm();
    });

    function clearForm() {
      subjectEl.value = "";
      titleEl.value = "";
      bodyEl.value = "";
      document.getElementById("mailRecipAll").checked = true;
      groupBox.hidden = true;
      selectedBox.hidden = true;
      editingTemplateId = null;
      pendingImageFile = null;
      hideSaveMessages();
      setImage(null, null);
    }

    // ---- Şablon kaydet (POST/PUT) ----
    var saveBtn = document.getElementById("mailSaveTemplateBtn");
    var saveErrorEl = document.getElementById("mailSaveError");
    var saveSuccessEl = document.getElementById("mailSaveSuccess");

    function hideSaveMessages() {
      saveErrorEl.hidden = true;
      saveSuccessEl.hidden = true;
    }

    function showSaveError(message) {
      saveSuccessEl.hidden = true;
      saveErrorEl.textContent = message;
      saveErrorEl.hidden = false;
    }

    function showSaveSuccess(message) {
      saveErrorEl.hidden = true;
      saveSuccessEl.textContent = message;
      saveSuccessEl.hidden = false;
    }

    saveBtn.addEventListener("click", function () {
      var name = window.prompt("Şablon adı:", "");
      if (name === null) return; // vazgeçildi
      name = name.trim();
      if (!name) { showSaveError("Şablon adı boş olamaz."); return; }

      // Gerçek görsel dosyası ayrı bir multipart endpoint'inden yükleniyor; henüz yüklenmemiş
      // (pendingImageFile dolu) bir seçim varsa burada imageUrl boş gönderilir, birazdan
      // uploadPendingImage() gerçek URL'i sunucudan alıp güncelleyecek.
      var payload = {
        name: name,
        subject: subjectEl.value,
        title: titleEl.value,
        content: bodyEl.value,
        imageUrl: (selectedImageSource === "upload" && !pendingImageFile) ? selectedImage : null,
        campaignImageUrl: selectedImageSource === "campaign" ? selectedImage : null,
        status: "ACTIVE"
      };

      var isUpdate = editingTemplateId != null;
      var url = isUpdate ? API_BASE + "/" + editingTemplateId : API_BASE;
      var method = isUpdate ? "PUT" : "POST";

      hideSaveMessages();
      saveBtn.disabled = true;
      var originalText = saveBtn.textContent;
      saveBtn.textContent = "Kaydediliyor...";

      apiFetch(url, { method: method, headers: jsonHeaders(), body: JSON.stringify(payload) })
        .then(function (saved) {
          editingTemplateId = saved.id;
          if (pendingImageFile) {
            saveBtn.textContent = "Görsel yükleniyor...";
            return uploadPendingImage(saved.id);
          }
          return saved;
        })
        .then(function (finalTemplate) {
          showSaveSuccess("Şablon kaydedildi: " + finalTemplate.name);
          loadTemplateList();
        })
        .catch(function (err) {
          showSaveError(err.message || "Mail şablonu oluşturulamadı.");
        })
        .finally(function () {
          saveBtn.disabled = false;
          saveBtn.textContent = originalText;
        });
    });

    /** Bekleyen (henüz yüklenmemiş) dosyayı multipart/form-data ile /image endpoint'ine gönderir,
     * dönen gerçek URL'i mevcut önizleme state'ine yansıtır. */
    function uploadPendingImage(id) {
      var formData = new FormData();
      formData.append("image", pendingImageFile);
      return apiFetch(API_BASE + "/" + id + "/image", { method: "POST", headers: csrfOnlyHeaders(), body: formData })
        .then(function (updated) {
          pendingImageFile = null;
          selectedImage = updated.imageUrl;
          selectedImageSource = "upload";
          renderPreview();
          return updated;
        });
    }

    function loadTemplateIntoForm(t) {
      subjectEl.value = t.subject || "";
      titleEl.value = t.title || "";
      bodyEl.value = t.content || "";
      pendingImageFile = null;
      if (t.imageUrl) setImage(t.imageUrl, "upload");
      else if (t.campaignImageUrl) setImage(t.campaignImageUrl, "campaign");
      else setImage(null, null);
      renderPreview();
    }

    // ---- Şablon listesi (GET / arama) ----
    var listEl = document.getElementById("mailTemplateList");
    var emptyEl = document.getElementById("mailTemplateEmpty");
    var loadingEl = document.getElementById("mailTemplateLoading");
    var listErrorEl = document.getElementById("mailTemplateError");
    var searchInput = document.getElementById("mailTemplateSearch");

    function fmtDate(iso) {
      var d = new Date(iso);
      if (isNaN(d.getTime())) return "-";
      var p = function (n) { return String(n).padStart(2, "0"); };
      return p(d.getDate()) + "." + p(d.getMonth() + 1) + "." + d.getFullYear();
    }

    function esc(s) {
      var div = document.createElement("div");
      div.textContent = s == null ? "" : s;
      return div.innerHTML;
    }

    function loadTemplateList() {
      var query = (searchInput.value || "").trim();
      var url = query ? API_BASE + "/search?query=" + encodeURIComponent(query) : API_BASE;

      listErrorEl.hidden = true;
      loadingEl.hidden = false;
      emptyEl.hidden = true;
      listEl.innerHTML = "";

      apiFetch(url, { method: "GET" })
        .then(function (templates) {
          loadingEl.hidden = true;
          renderTemplateList(templates || []);
        })
        .catch(function (err) {
          loadingEl.hidden = true;
          listErrorEl.textContent = err.message || "Mail şablonları yüklenemedi.";
          listErrorEl.hidden = false;
        });
    }

    function renderTemplateList(templates) {
      listEl.innerHTML = "";
      emptyEl.hidden = templates.length > 0;
      if (templates.length === 0) return;

      templates.forEach(function (t) {
        var card = document.createElement("div");
        card.className = "adp__card";
        card.style.margin = "0";

        var thumbSrc = t.imageUrl || t.campaignImageUrl;
        var thumbHtml = thumbSrc
          ? '<div style="width:100%;height:110px;border-radius:var(--radius-sm);background:center/cover no-repeat url(' + thumbSrc + ');margin-bottom:10px"></div>'
          : '<div style="width:100%;height:110px;border-radius:var(--radius-sm);background:var(--surface-100);display:flex;align-items:center;justify-content:center;color:var(--ink-500);font-size:12px;margin-bottom:10px">Görsel yok</div>';

        card.innerHTML =
          thumbHtml +
          '<div style="font-size:14px;font-weight:600;color:var(--ink-900)">' + esc(t.name) + '</div>' +
          '<div style="font-size:12.5px;color:var(--ink-500);margin:2px 0 6px">' + esc(t.subject || "—") + '</div>' +
          '<div style="font-size:12.5px;color:var(--ink-700);margin-bottom:8px;max-height:38px;overflow:hidden">' + esc((t.content || "").slice(0, 90)) + '</div>' +
          '<div style="font-size:11px;color:var(--ink-500);margin-bottom:10px">Oluşturuldu: ' + fmtDate(t.createdAt) + ' · Güncellendi: ' + fmtDate(t.updatedAt) + '</div>' +
          '<div style="display:flex;gap:6px;flex-wrap:wrap">' +
          '  <button type="button" class="adp__btn adp__btn--sm js-tpl-use">Kullan</button>' +
          '  <button type="button" class="adp__btn adp__btn--sm adp__btn--ghost js-tpl-edit">Düzenle</button>' +
          '  <button type="button" class="adp__btn adp__btn--sm adp__btn--danger js-tpl-delete">Sil</button>' +
          '</div>';

        card.querySelector(".js-tpl-use").addEventListener("click", function () {
          fetchAndUse(t.id, false);
        });
        card.querySelector(".js-tpl-edit").addEventListener("click", function () {
          fetchAndUse(t.id, true);
        });
        card.querySelector(".js-tpl-delete").addEventListener("click", function () {
          if (!window.confirm("Bu mail şablonunu silmek istediğinize emin misiniz?")) return;
          apiFetch(API_BASE + "/" + t.id, { method: "DELETE", headers: jsonHeaders() })
            .then(function () {
              if (editingTemplateId === t.id) editingTemplateId = null;
              loadTemplateList();
            })
            .catch(function (err) {
              window.alert(err.message || "Mail şablonu silinemedi.");
            });
        });

        listEl.appendChild(card);
      });
    }

    function fetchAndUse(id, setEditing) {
      apiFetch(API_BASE + "/" + id, { method: "GET" })
        .then(function (t) {
          loadTemplateIntoForm(t);
          editingTemplateId = setEditing ? t.id : null;
          hideSaveMessages();
          showTab("compose");
        })
        .catch(function (err) {
          window.alert(err.message || "Mail şablonu bulunamadı.");
        });
    }

    document.getElementById("mailTemplateEmptyCreateBtn").addEventListener("click", function () {
      clearForm();
      showTab("compose");
    });

    searchInput.addEventListener("input", function () {
      clearTimeout(searchDebounceTimer);
      searchDebounceTimer = setTimeout(loadTemplateList, 300);
    });

    // ---- Mail Gönder (gerçek gönderim — backend /admin/mail/send üzerinden Resend API) ----
    var sendBtn = document.getElementById("mailSendBtn");
    var sendErrorEl = document.getElementById("mailSendError");
    var sendSuccessEl = document.getElementById("mailSendSuccess");

    function hideSendMessages() {
      sendErrorEl.hidden = true;
      sendSuccessEl.hidden = true;
    }
    function showSendError(message) {
      sendSuccessEl.hidden = true;
      sendErrorEl.textContent = message;
      sendErrorEl.hidden = false;
    }
    function showSendSuccess(message) {
      sendErrorEl.hidden = true;
      sendSuccessEl.textContent = message;
      sendSuccessEl.hidden = false;
    }

    sendBtn.addEventListener("click", function () {
      hideSendMessages();

      var subject = subjectEl.value.trim();
      var title = titleEl.value.trim();
      var content = bodyEl.value.trim();
      if (!subject) { showSendError("Mail konusu boş olamaz."); return; }
      if (!content) { showSendError("Mail metni boş olamaz."); return; }

      var recipientType = document.querySelector('input[name="mailRecipients"]:checked').value;
      if (recipientType === "GROUP") {
        showSendError("Müşteri grubu ile gönderim henüz desteklenmiyor. Lütfen 'Tüm Müşteriler' veya 'Seçili Müşteriler' seçin.");
        return;
      }

      var customerIds = null;
      if (recipientType === "SELECTED") {
        customerIds = Array.prototype.filter.call(
          document.querySelectorAll(".js-mail-customer"), function (cb) { return cb.checked; }
        ).map(function (cb) { return parseInt(cb.getAttribute("data-customer-id"), 10); });
        if (customerIds.length === 0) { showSendError("Lütfen en az bir müşteri seçin."); return; }
      }

      // Görsel sadece gerçek, sunucuda barınan bir URL'e sahipse maile eklenir — henüz yüklenmemiş
      // (pendingImageFile dolu) bir seçim yerel bir önizlemedir (dataURL), mail HTML'inde kullanılamaz.
      if (pendingImageFile) {
        showSendError("Seçtiğiniz görsel henüz sunucuya yüklenmedi. Göndermeden önce 'Şablon Olarak Kaydet' ile yükleyin ya da görseli kaldırın.");
        return;
      }

      var payload = {
        recipientType: recipientType,
        customerIds: customerIds,
        subject: subject,
        title: title,
        content: content,
        imageUrl: selectedImage || null
      };

      sendBtn.disabled = true;
      var originalText = sendBtn.textContent;
      sendBtn.textContent = "Gönderiliyor...";

      apiFetch("/admin/mail/send", { method: "POST", headers: jsonHeaders(), body: JSON.stringify(payload) })
        .then(function (result) {
          showSendSuccess((result && result.message) || "Mail başarıyla gönderildi.");
        })
        .catch(function (err) {
          showSendError(err.message || "Mail gönderilemedi. Lütfen tekrar deneyin.");
        })
        .finally(function () {
          sendBtn.disabled = false;
          sendBtn.textContent = originalText;
        });
    });

    // İlk yüklemede önizleme boş durumda başlasın.
    renderPreview();
  });
})();
