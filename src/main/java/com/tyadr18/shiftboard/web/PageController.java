package com.tyadr18.shiftboard.web;

import com.tyadr18.shiftboard.config.LoginProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * トップとログイン画面。ログイン処理そのもの（POST /login）は Spring Security が受け持つ。
 */
@Controller
public class PageController {

    private final LoginProperties loginProperties;

    public PageController(LoginProperties loginProperties) {
        this.loginProperties = loginProperties;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/shifts";
    }

    @GetMapping("/login")
    public String login(Model model) {
        // デモ用パスワードのまま起動しているときだけ、ログイン画面にヒントを出す
        model.addAttribute("demoMode", loginProperties.usesDemoPassword());
        model.addAttribute("demoUsername", loginProperties.username());
        model.addAttribute("demoPassword", LoginProperties.DEMO_PASSWORD);
        return "login";
    }
}
