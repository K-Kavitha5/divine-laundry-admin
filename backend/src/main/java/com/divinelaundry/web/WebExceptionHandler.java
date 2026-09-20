package com.divinelaundry.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.divinelaundry.service.PaymentReceiptService;

@ControllerAdvice(basePackages = "com.divinelaundry.web")
public class WebExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalid(RuntimeException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(CustomerWebService.CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(Model model) {
        model.addAttribute("message", "Customer not found");
        return "error";
    }

    @ExceptionHandler(PaymentReceiptService.PaymentReceiptNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String receiptNotFound(Model model) {
        model.addAttribute("message", "Payment receipt not found");
        return "error";
    }
}
