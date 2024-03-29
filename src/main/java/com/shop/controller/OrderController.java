package com.shop.controller;

import com.shop.dto.OrderDto;
import com.shop.dto.OrderHistDto;
import com.shop.serivce.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/order")
    //@ResponseBody 어노테이션이 메서드 선언에 위치할 경우(지금 경우), 전체 응답을 나타내는 객체를 반환하며(ResponseEntity),
    // 메서드 내부에 위치할 경우, 해당 메서드가 반환하는 값 자체가 응답 본문으로 사용됩니다.
    public @ResponseBody ResponseEntity order(@RequestBody @Valid OrderDto orderDto, BindingResult bindingResult,
                                              Principal principal) {
        if (bindingResult.hasErrors()) {   //주문 정보를 받는 orderDto 객체에 데이터 바인딩 시 에러가 있는지 검사.
            StringBuilder sb = new StringBuilder();
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                sb.append(fieldError.getDefaultMessage());
            }
            return new ResponseEntity<String>(sb.toString(), HttpStatus.BAD_REQUEST);   //에러 정보를 ResponseEntity 객체에 담아서 반환
        }

        String email = principal.getName();     //현재 로그인 유저의 정보를 얻기 위해 @Controller 어노테이션이 선언된 클래스에서 메소드 인자로 principal 객체를 넘겨 줄 경우
        //해당 객체에 직접 접근할 수 있다.
        //prinicipal 객체에서 현재 로그인한 회원의 이메일 정보를 조회한다.
        Long orderId;
        try {
            orderId = orderService.order(orderDto, email);  //화면으로부터 넘어오는 주문 정보와 회원의 이메일 정보를 이용하여 주문 로직을 호출
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Long>(orderId, HttpStatus.OK);        //결과값으로 생성된 주문 정보와 요청이 성공했다는 HTTP 응답 상태 코드 반환
    }

    @GetMapping(value = {"/orders", "/orders/{page}"})
    public String orderHist(@PathVariable("page") Optional<Integer> page,
                            Principal principal, Model model) {
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 4);  //한번에 가지고 올 주문의 개수는 4개로 설정

        Page<OrderHistDto> ordersHistDtoList =
                orderService.getOrderList(principal.getName(), pageable);   //현재 로그인한 회원은 이메일과 페이징 객체를 파라미터로 전달하여 화면에 전달한 주문 목록 데이터를 리턴 값으로 받는다

        model.addAttribute("orders", ordersHistDtoList);
        model.addAttribute("page", pageable.getPageNumber());
        model.addAttribute("maxPage", 5);

        return "order/orderHist";
    }

    @PostMapping("/order/{orderId}/cancel")
    public @ResponseBody ResponseEntity orderCancel(@PathVariable("orderId") Long orderId, Principal principal) {
        if (!orderService.validateOrder(orderId, principal.getName())) {
            return new ResponseEntity<String>("주문 취소 권한이 없습니다", HttpStatus.FORBIDDEN);  //다른 사람의 주문을 취소하지 못하도록 주문 취소 권한 검사를 한다
        }
        orderService.cancelOrder(orderId);  //주문 취소 로직을 호출
        return new ResponseEntity<Long>(orderId, HttpStatus.OK);
    }
}

