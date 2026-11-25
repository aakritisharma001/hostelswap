package com.hostelswap.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Instead of dropping the connection, send the user back with a clear warning.
@ControllerAdvice
public class UploadExceptionHandler {

    @Value("${hostelswap.max-photo-mb}")
    private int maxMb;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest req, RedirectAttributes ra) {
        ra.addFlashAttribute("flashErr", "This photo is too large. The maximum allowed size is "
                + maxMb + " MB. Please choose a smaller file.");
        String back = req.getHeader("Referer");
        return "redirect:" + (back != null ? back : "/dashboard");
    }
}
