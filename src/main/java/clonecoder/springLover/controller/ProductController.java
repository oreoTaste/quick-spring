package clonecoder.springLover.controller;

import clonecoder.springLover.domain.Member;
import clonecoder.springLover.domain.Product;
import clonecoder.springLover.service.MemberService;
import clonecoder.springLover.service.OrderService;
import clonecoder.springLover.service.ProductService;
import clonecoder.springLover.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.connector.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final OrderService orderService;
    private final MemberService memberService;
    private final StorageService storageService;

    @GetMapping("register/product")
    public String registerForm(HttpServletRequest request) throws Exception {
        Long id = (Long) request.getSession().getAttribute("id");
        Member member = memberService.findOne(id);
        if (member == null) {
            throw new IllegalAccessException("유효하지 않은 접근입니다");
        }
        return "product/registerForm";
    }

    @PostMapping("register/product")
    public String register(@RequestParam("description") MultipartFile description,
                           @RequestParam("thumbnail") MultipartFile thumbnail,
                           @ModelAttribute() ProductForm productForm,
                           HttpServletRequest request
    ) throws Exception {

        String key = "" + System.currentTimeMillis();
        storageService.store(description, key + "/description", request);
        storageService.store(thumbnail, key + "/thumbnail", request);

        if (productForm.getIs_rocket() == null) {
            productForm.setIs_rocket("off");
        }
        Product product = new Product().create(productForm);
        product.setDescription("upload/" + key + "/description/" + description.getOriginalFilename());
        product.setThumbnail("upload/" + key + "/thumbnail/" + thumbnail.getOriginalFilename());

        productService.saveProduct(product);
        return "redirect:../login";
    }

    @GetMapping("product/{id}")
    public String productDetail(@PathVariable Long id, HttpServletRequest request, Model model) throws Exception {
        Product product = productService.findOne(id);
        model.addAttribute("product", product);
        return "product/detail";
    }

    @GetMapping("product/checkout/productId={productId}&count={count}")
    public String checkout(@PathVariable Long productId,
                           @PathVariable int count,
                           Model model,
                           HttpServletRequest request) {
        Long memberId = (Long) request.getSession().getAttribute("id");
        Member member = memberService.findOne(memberId);
        model.addAttribute("member", member);

        Product product = productService.findOne(productId);
        model.addAttribute("product", product);

        model.addAttribute("count", count);

        return "order/directCheckout";
    }

}
