package clonecoder.springLover.service;

import clonecoder.springLover.controller.CookieForm;
import clonecoder.springLover.domain.*;
import clonecoder.springLover.repository.CartRepository;
import clonecoder.springLover.repository.MemberRepository;
import clonecoder.springLover.repository.OrderRepository;
import clonecoder.springLover.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class CartService {
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final MemberService memberService;

    @Transactional
    public void save(Long memberId, List<Long> productIdList, List<Integer> countList) throws Exception {
        for(int i = 0; i < productIdList.size(); i++) {
            save(memberId, productIdList.get(i), countList.get(i));
        }
    }

    @Transactional
    public void save(HttpServletRequest request, Long productId, int quantity) throws Exception {
        Member member = memberService.checkValidity(request);
        save(member.getId(), productId, quantity);
    }

    @Transactional
    public Long save(Long memberId, Long productId, int count) throws Exception{
        // 엔티티조회
        Member member = memberRepository.findOne(memberId);
        Product product = productRepository.findOne(productId);

        // 이미 카트에 있는 경우, 상품 개수만 늘리기
        if(!member.plusQuantityCart(productId, count)) {
            // 카드에 없는 경우, 추가하기
            Cart savedCart = Cart.createCart(member, product, count);
            cartRepository.save(savedCart);
            return savedCart.getId();
        }
        return null;
    }

    public Cart findOne(Long cartId) {
        return cartRepository.findOne(cartId);
    }

    public List<Cart> findMyCart(Long memberId) {
        return cartRepository.findAllByString(memberId);
    }

    public List<Cart> findMyCart(HttpServletRequest request) {
        Member member = memberService.checkValidity(request);
        return cartRepository.findAllByString(member.getId());
    }


    @Transactional
    public void clear(Cart cart) {
        cartRepository.delete(cart);
    }

    @Transactional
    public void clear(Member member, List<Long> productIdList) {
        List<Cart> cartList = cartRepository.findAllByString(member.getId());
        for(Cart cart : cartList) {
            for(int i = 0; i < productIdList.size(); i++) {
                if(cart.getProduct().getId().equals(productIdList.get(i))) {
                    cartRepository.delete(cart);
                    break;
                }
            }
        }
    }

    @Transactional
    public void clear(HttpServletRequest request, List<Long> productIdList) {
        Member member = memberService.checkValidity(request);
        List<Cart> cartList = cartRepository.findAllByString(member.getId());
        for(Cart cart : cartList) {
            for(int i = 0; i < productIdList.size(); i++) {
                if(cart.getProduct().getId().equals(productIdList.get(i))) {
                    cartRepository.delete(cart);
                    break;
                }
            }
        }
    }

    @Transactional
    public void setQuantity(Long cartId, int quantity) {
        Cart cart = cartRepository.findOne(cartId);
        cart.setCount(quantity);
    }

    @Transactional
    public void adjust(Long memberId, Long productId, int quantity) {
        // 엔티티조회
        Member member = memberRepository.findOne(memberId);
        Product product = productRepository.findOne(productId);

        // 이미 카트에 있어서, 상품 개수만 조정하기
        member.adjustQuantityCart(productId, quantity);
    }

    @Transactional
    public boolean adjust(HttpServletRequest request, Long productId, int quantity) {
        Member member = memberService.checkValidity(request);
        Hibernate.initialize(member.getCartList());
//        List<Cart> myCart = findMyCart(request);
        List<Cart> myCart = member.getCartList();
        for (Cart cart : myCart) {
            if (cart.getProduct().getId() == productId) {
                adjust(member.getId(), productId, quantity);
                return true;
            }
        }
        return false;
    }

}
