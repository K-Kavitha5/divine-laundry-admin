package com.divinelaundry.web;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.*;
import com.divinelaundry.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class AdminWebController {
    private final CustomerRepository customers;
    private final LaundryServiceRepository catalog;
    private final LaundryOrderRepository orders;
    private final CustomerWebService customerService;
    private final WebOrderService webOrders;
    private final PaymentService payments;
    private final DocumentService documents;
    private final InvoicePaymentImageService images;
    private final WhatsappMessageRepository messages;
    private final ZoneId zone;
    private final String businessName;
    private final boolean upiConfigured;

    public AdminWebController(CustomerRepository customers, LaundryServiceRepository catalog,
            LaundryOrderRepository orders, CustomerWebService customerService, WebOrderService webOrders,
            PaymentService payments, DocumentService documents, InvoicePaymentImageService images,
            WhatsappMessageRepository messages, @Value("${app.business-zone}") String zone,
            @Value("${app.business.name}") String businessName, @Value("${app.payment.upi-id:}") String upiId) {
        this.customers = customers; this.catalog = catalog; this.orders = orders;
        this.customerService = customerService; this.webOrders = webOrders;
        this.payments = payments; this.documents = documents; this.images = images;
        this.messages = messages; this.zone = ZoneId.of(zone); this.businessName = businessName;
        this.upiConfigured = !upiId.isBlank();
    }

    @InitBinder
    void limitBinding(WebDataBinder binder) { binder.setAutoGrowCollectionLimit(50); }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("businessName", businessName);
        model.addAttribute("businessZone", zone);
        model.addAttribute("upiConfigured", upiConfigured);
    }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/")
    String dashboard(Model model) {
        model.addAttribute("customerCount", customers.count());
        model.addAttribute("orderCount", orders.count());
        model.addAttribute("recentOrders", orders.findTop50ByOrderByPlacedAtDesc());
        return "dashboard";
    }

    @GetMapping("/customers")
    String customers(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("customers", customers.findTop30ByNameContainingIgnoreCaseOrPhoneContainingOrderByNameAsc(q, q));
        return "customers";
    }

    @GetMapping("/customers/new")
    String newCustomer(Model model) { model.addAttribute("customerForm", new CustomerForm()); return "customer-form"; }

    @PostMapping("/customers")
    String createCustomer(@Valid @ModelAttribute CustomerForm customerForm, BindingResult errors, Model model) {
        if (errors.hasErrors()) return "customer-form";
        try {
            Customer saved = customerService.create(customerForm);
            return "redirect:/orders/new?customerId=" + saved.getId();
        } catch (IllegalArgumentException ex) { errors.reject("invalid", ex.getMessage()); }
        catch (DataIntegrityViolationException ex) { errors.reject("duplicate", "This mobile number already exists. Open Customers to find it."); }
        return "customer-form";
    }

    @GetMapping("/orders/new")
    String newOrder(@RequestParam(required = false) Long customerId, Model model) {
        var form = new OrderForm();
        form.setCustomerId(customerId);
        form.setDeliveryAt(LocalDateTime.now(zone).plusDays(2).withSecond(0).withNano(0).toString());
        form.getItems().add(new LineForm());
        model.addAttribute("orderForm", form);
        populateOrder(model);
        return "order-form";
    }

    private void populateOrder(Model model) {
        model.addAttribute("customers", customers.findAll(org.springframework.data.domain.Sort.by("name")));
        model.addAttribute("catalog", catalog.findByActiveTrueOrderByCategoryAscNameAsc());
    }

    @PostMapping("/orders")
    String createOrder(@Valid @ModelAttribute OrderForm orderForm, BindingResult errors, Model model, Principal principal) {
        if (!errors.hasErrors()) {
            try { return "redirect:/orders/" + webOrders.create(orderForm, principal.getName()); }
            catch (IllegalArgumentException | IllegalStateException ex) { errors.reject("invalid", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { errors.reject("conflict", "The submission conflicts with an existing record. Check the dashboard before retrying."); }
        }
        populateOrder(model);
        return "order-form";
    }

    @GetMapping("/orders/{number}")
    String detail(@PathVariable String number, Model model) {
        model.addAttribute("paymentForm", new PaymentForm());
        populateDetail(number, model);
        return "order-detail";
    }

    private void populateDetail(String number, Model model) {
        var summary = payments.summary(number);
        model.addAttribute("order", summary.order());
        model.addAttribute("summary", summary);
        model.addAttribute("modes", PaymentMode.values());
        String state = summary.order().getInvoiceNumber() == null ? "No invoice" : messages
                .findByDeduplicationKey("INVOICE_IMAGE:" + summary.order().getInvoiceNumber())
                .map(message -> message.getDeliveryStatus().name()).orElse("NOT_QUEUED");
        model.addAttribute("whatsappState", state);
    }

    @PostMapping("/orders/{number}/payments")
    String pay(@PathVariable String number, @Valid @ModelAttribute PaymentForm paymentForm,
            BindingResult errors, Model model, Principal principal, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                payments.record(new PaymentService.RecordPaymentCommand(paymentForm.getRequestId(), number,
                        paymentForm.getMode(), paymentForm.getReference(), paymentForm.getAmount(), Instant.now(), principal.getName()));
                redirect.addFlashAttribute("success", "Payment recorded. This is a manual entry, not bank verification.");
                return "redirect:/orders/" + number;
            } catch (IllegalArgumentException | IllegalStateException ex) { errors.reject("invalid", ex.getMessage()); }
            catch (org.springframework.dao.OptimisticLockingFailureException | DataIntegrityViolationException ex) {
                errors.reject("conflict", "This order changed during payment. Refresh and check its payment history before retrying.");
            }
        }
        populateDetail(number, model);
        return "order-detail";
    }

    @GetMapping("/orders/{number}/invoice")
    String invoice(@PathVariable String number, Model model) {
        model.addAttribute("document", documents.document(number));
        return "invoice";
    }

    @GetMapping(value = "/orders/{number}/invoice.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    ResponseEntity<byte[]> image(@PathVariable String number) {
        var bundle = documents.document(number);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(bundle.order().getInvoiceNumber() + ".png").build().toString())
                .body(images.render(bundle));
    }

    @GetMapping("/services")
    String services(Model model) { model.addAttribute("catalog", catalog.findByActiveTrueOrderByCategoryAscNameAsc()); return "services"; }
}
