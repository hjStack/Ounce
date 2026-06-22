package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("mode", "login");
        model.addAttribute("isLoggedIn", false);
        return "auth";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("mode", "signup");
        model.addAttribute("isLoggedIn", false);
        return "auth";
    }

    // TODO: Member 엔티티에 email/name 필드와 Spring Security가 아직 없어 실제 인증 로직은 미구현 상태.
    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("toastMessage", "로그인 기능은 아직 준비 중입니다.");
        redirectAttributes.addFlashAttribute("toastType", "error");
        return "redirect:/login";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String name, @RequestParam String email,
                          @RequestParam String password, @RequestParam String confirmPassword,
                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("toastMessage", "회원가입 기능은 아직 준비 중입니다.");
        redirectAttributes.addFlashAttribute("toastType", "error");
        return "redirect:/signup";
    }
}
