package com.digitalcertificate.controller;

import com.digitalcertificate.config.AppProperties;
import com.digitalcertificate.dto.RegisterForm;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.UserService;
import com.digitalcertificate.util.Patterns;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService userService;
    private final AppProperties props;
    private final RequestInfo requestInfo;

    public AuthController(UserService userService, AppProperties props, RequestInfo requestInfo) {
        this.userService = userService;
        this.props = props;
        this.requestInfo = requestInfo;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AppUserDetails me, Model model) {
        if (me != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("demoMode", props.demoData());
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(@AuthenticationPrincipal AppUserDetails me, Model model) {
        if (me != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
                           HttpServletRequest request, RedirectAttributes redirect) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!result.hasFieldErrors("password") && !Patterns.isValidPassword(form.getPassword())) {
            result.rejectValue("password", "invalid", Patterns.PASSWORD_MESSAGE);
        }
        if (form.getRole() == Role.ADMIN) {
            result.rejectValue("role", "invalid", "Administrator accounts cannot be self-registered");
        }
        if (form.getRole() == Role.ISSUER && (form.getInstitutionName() == null || form.getInstitutionName().isBlank())) {
            result.rejectValue("institutionName", "required", "Institution name is required for issuers");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            User u = userService.register(form.getName(), form.getEmail(), form.getPassword(), form.getRole(),
                    form.getInstitutionName(), requestInfo.ip(request));
            redirect.addFlashAttribute("flashSuccess", u.isEnabled()
                    ? "Account created! You can sign in now."
                    : "Registration received. An administrator must approve your issuer account before you can sign in.");
            return "redirect:/login";
        } catch (BusinessException e) {
            result.reject("register", e.getMessage());
            return "register";
        }
    }
}
