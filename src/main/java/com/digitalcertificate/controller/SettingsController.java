package com.digitalcertificate.controller;

import com.digitalcertificate.dto.PasswordForm;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {
    private final UserService userService;
    private final RequestInfo requestInfo;

    public SettingsController(UserService userService, RequestInfo requestInfo) {
        this.userService = userService;
        this.requestInfo = requestInfo;
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("user", userService.getById(me.getId()));
        model.addAttribute("passwordForm", new PasswordForm());
        return "settings";
    }

    @PostMapping("/settings/profile")
    public String profile(@AuthenticationPrincipal AppUserDetails me, @RequestParam String name,
                          HttpServletRequest request, RedirectAttributes redirect) {
        if (name == null || name.isBlank() || name.length() > 100) {
            redirect.addFlashAttribute("flashError", "Please enter a name (max. 100 characters).");
        } else {
            userService.updateName(me.getId(), name, requestInfo.ip(request));
            redirect.addFlashAttribute("flashSuccess", "Profile updated. The new name appears after your next sign-in.");
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/password")
    public String password(@AuthenticationPrincipal AppUserDetails me,
                           @Valid @ModelAttribute("passwordForm") PasswordForm form, BindingResult result,
                           HttpServletRequest request, Model model, RedirectAttributes redirect) {
        if (!result.hasFieldErrors("newPassword") && !result.hasFieldErrors("confirmPassword")
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!result.hasFieldErrors("newPassword") && !Patterns.isValidPassword(form.getNewPassword())) {
            result.rejectValue("newPassword", "invalid", Patterns.PASSWORD_MESSAGE);
        }
        if (!result.hasErrors()) {
            try {
                userService.changePassword(me.getId(), form.getCurrentPassword(), form.getNewPassword(), requestInfo.ip(request));
                redirect.addFlashAttribute("flashSuccess", "Your password was changed.");
                return "redirect:/settings";
            } catch (BusinessException e) {
                result.rejectValue("currentPassword", "wrong", e.getMessage());
            }
        }
        model.addAttribute("user", userService.getById(me.getId()));
        return "settings";
    }
}
