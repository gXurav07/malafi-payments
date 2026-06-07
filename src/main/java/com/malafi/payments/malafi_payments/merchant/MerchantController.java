package com.malafi.payments.malafi_payments.merchant;

import com.malafi.payments.malafi_payments.merchant.dto.CreateMerchantRequest;
import com.malafi.payments.malafi_payments.merchant.dto.MerchantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse createMerchant(
            @Valid @RequestBody CreateMerchantRequest request) {
        return merchantService.createMerchant(request);
    }

    @GetMapping("/{merchantId}")
    public MerchantResponse getMerchant(
            @PathVariable Long merchantId) {
        return merchantService.getMerchant(merchantId);
    }
}
