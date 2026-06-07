package com.malafi.payments.malafi_payments.merchant;

import com.malafi.payments.malafi_payments.merchant.dto.CreateMerchantRequest;
import com.malafi.payments.malafi_payments.merchant.dto.MerchantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        Merchant merchant = new Merchant(request.name(), generateApiKey());
        Merchant savedMerchant = merchantRepository.save(merchant);

        return MerchantResponse.from(savedMerchant);
    }

    public MerchantResponse getMerchant(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));

        return MerchantResponse.from(merchant);
    }

    private String generateApiKey() {
        return "malafi_" + UUID.randomUUID().toString().replace("-", "");
    }
}
