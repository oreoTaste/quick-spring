package clonecoder.springLover.controller;

import clonecoder.springLover.domain.*;
import clonecoder.springLover.service.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final ProductService productService;
    private final OrderService orderService;
    private final MemberService memberService;
    private final AddressService addressService;
    private final CartService cartService;

    // 주문 결제 폼
    @GetMapping("product/checkout/productId={productId}&count={count}")
    @Transactional
    public String checkoutForm(@PathVariable String productId,
                               @PathVariable String count,
                               Model model,
                               HttpServletRequest request) {
        try {

            Member member = memberService.checkValidity(request);
            model.addAttribute("member", member);

            Hibernate.initialize(member.getAddressList());
            List<Address> addressList = member.getAddressList();
            // 메인 주소 정보만 추출
            Optional<Address> mainAddress = addressList.stream().filter((e) -> {
                return e.getId().equals(member.getMainAddressId());
            }).findAny();
            if(mainAddress.isPresent()) {
                model.addAttribute("address", mainAddress.get());
            }

            List<Product> productList = new ArrayList<>();
            List<Integer> countList = new ArrayList<>();

            String[] splitProductId = productId.split(",");
            for(String id : splitProductId) {
                Product product = productService.findOne(Long.parseLong(id));
                productList.add(product);
            }
            model.addAttribute("productList", productList);

            String[] splitCount = count.split(",");
            for(String cnt : splitCount) {
                countList.add(Integer.parseInt(cnt));
            }
            model.addAttribute("countList", countList);

        } catch (Exception e) {
            return "redirect:/";
        }
        return "order/checkout";
    }

    // 결제 확인후 오더접수(출고전)
    @PostMapping("checkout")
    @Transactional
    @ResponseBody
    public String checkout(HttpServletRequest request,
                           Model model) throws Exception {
        String exAddressId = request.getParameter("exAddressId");
        String exProductId = request.getParameter("exProductId");

        System.out.println("exAddressId: " + exAddressId);

        List<Long> idList = new ArrayList<>();
        List<Integer> cntList = new ArrayList<>();
        String[] split = exProductId.split(",");
        for(String part : split) {
            idList.add(Long.parseLong(part.split(":")[0]));
            cntList.add(Integer.parseInt(part.split(":")[1]));
        }

        Member member = memberService.checkValidity(request);
        System.out.println("++++++++++++++++++++++++++++");
        System.out.println(member);
        System.out.println("++++++++++++++++++++++++++++");
        Address address = addressService.findAddress(request, Long.parseLong(exAddressId));
        Long orderId = orderService.cardCheckout(member.getId(), idList, cntList, address);
        return orderId.toString();
    }
    
    // 접수완료 폼 띄우면서 재고정산
    @GetMapping("checkout/after/{orderId}")
    @Transactional
    public String checkoutAfter(@PathVariable Long orderId,
                                HttpServletRequest request,
                                Model model) throws Exception {

        Order order = orderService.findOne(orderId);
        model.addAttribute("order", order);

        Stream<Long> productIdStream = order.getOrderProductList().stream().map((e) -> {
            return e.getProduct().getId();
        });
        List<Long> productIdList = productIdStream.collect(Collectors.toList());

        cartService.clear(request, productIdList);
        return "order/after";
    }

}
