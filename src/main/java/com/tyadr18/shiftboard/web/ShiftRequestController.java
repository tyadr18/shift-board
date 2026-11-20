package com.tyadr18.shiftboard.web;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.domain.TimeSlot;
import com.tyadr18.shiftboard.service.ShiftRequestService;
import com.tyadr18.shiftboard.service.ShiftRuleViolationException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shifts")
public class ShiftRequestController {

    static final String LIST_VIEW = "shifts/list";
    static final String FORM_VIEW = "shifts/form";
    private static final String REDIRECT_TO_LIST = "redirect:/shifts";

    private final ShiftRequestService service;

    public ShiftRequestController(ShiftRequestService service) {
        this.service = service;
    }

    @ModelAttribute("timeSlots")
    TimeSlot[] timeSlots() {
        return TimeSlot.values();
    }

    @GetMapping
    public String list(@Valid @ModelAttribute("search") ShiftSearchForm search, BindingResult result, Model model) {
        // 条件が不正なときは検索せず、エラーメッセージだけを表示する
        List<ShiftRequest> shifts = result.hasErrors() ? List.of() : service.search(search.toCriteria());
        model.addAttribute("shifts", shifts);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ShiftRequestForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ShiftRequestForm form, BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            service.create(form.toCommand());
        } catch (ShiftRuleViolationException e) {
            result.rejectValue(e.getField(), "shiftRule", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "シフト希望を登録しました");
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ShiftRequestForm.from(service.get(id)));
        model.addAttribute("shiftId", id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ShiftRequestForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("shiftId", id);
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            service.update(id, form.toCommand());
        } catch (ShiftRuleViolationException e) {
            result.rejectValue(e.getField(), "shiftRule", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "シフト希望を更新しました");
        return REDIRECT_TO_LIST;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.delete(id);
        redirectAttributes.addFlashAttribute("message", "シフト希望を削除しました");
        return REDIRECT_TO_LIST;
    }
}
