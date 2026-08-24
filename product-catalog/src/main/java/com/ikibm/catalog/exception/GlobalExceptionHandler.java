package com.ikibm.catalog.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException ex) {
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        mv.addObject("message", ex.getMessage());
        return mv;
    }

    /** Var olmayan statik dosya istekleri (ör. tarayıcının otomatik favicon.ico çağrısı) —
     * genel Exception yakalayıcısına düşüp 500 dönmesin diye ayrıca 404 olarak ele alınır. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResource(NoResourceFoundException ex) {
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        mv.addObject("message", "Kaynak bulunamadı");
        return mv;
    }

    @ExceptionHandler(ConflictException.class)
    public ModelAndView handleConflict(ConflictException ex) {
        ModelAndView mv = new ModelAndView("error/500");
        mv.setStatus(HttpStatus.CONFLICT);
        mv.addObject("message", ex.getMessage());
        return mv;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleTooLarge(MaxUploadSizeExceededException ex) {
        ModelAndView mv = new ModelAndView("error/500");
        mv.setStatus(HttpStatus.PAYLOAD_TOO_LARGE);
        mv.addObject("message", "Dosya çok büyük — en fazla 5MB yükleyebilirsiniz.");
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        log.error("Beklenmeyen hata", ex);
        ModelAndView mv = new ModelAndView("error/500");
        mv.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mv.addObject("message", "Beklenmeyen bir hata oluştu");
        return mv;
    }
}
