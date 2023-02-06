package com.woowahan.recipe.service;

import com.woowahan.recipe.domain.UserRole;
import com.woowahan.recipe.domain.dto.Response;
import com.woowahan.recipe.domain.dto.reviewDto.ReviewListResponse;
import com.woowahan.recipe.domain.dto.sellerDto.*;
import com.woowahan.recipe.domain.dto.userDto.UserLoginResDto;
import com.woowahan.recipe.domain.entity.RecipeEntity;
import com.woowahan.recipe.domain.entity.ReviewEntity;
import com.woowahan.recipe.domain.entity.SellerEntity;
import com.woowahan.recipe.domain.entity.UserEntity;
import com.woowahan.recipe.exception.AppException;
import com.woowahan.recipe.exception.ErrorCode;
import com.woowahan.recipe.repository.SellerRepository;
import com.woowahan.recipe.security.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellerService {
    private final SellerRepository sellerRepository;
    private final BCryptPasswordEncoder encoder;

    @Value("${jwt.token.secret}")
    private String secretKey;

    private long expiredTimeMs = 60 * 60 * 1000; //토큰 유효시간: 1시간

    public SellerEntity validateSeller(Long id) {
        SellerEntity seller = sellerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SELLER_NOT_FOUND, ErrorCode.SELLER_NOT_FOUND.getMessage()));
        return seller;
    }


    public SellerJoinResponse join(SellerJoinRequest sellerJoinRequest) {
        // sellerName (ID) 중복확인
        sellerRepository.findBySellerName(sellerJoinRequest.getSellerName())
                .ifPresent(seller -> {
                            throw new AppException(ErrorCode.DUPLICATED_USER_NAME, ErrorCode.DUPLICATED_USER_NAME.getMessage());
                        });

        // email 중복확인
        sellerRepository.findByEmail(sellerJoinRequest.getEmail())
                .ifPresent(seller -> {
                        throw new AppException(ErrorCode.DUPLICATED_EMAIL, ErrorCode.DUPLICATED_EMAIL.getMessage());
                });

        SellerEntity seller = sellerRepository.save(sellerJoinRequest.toEntity(
                encoder.encode(sellerJoinRequest.getPassword())
        ));

        return new SellerJoinResponse(seller.getSellerName(),
                String.format("%s님의 회원가입이 완료되었습니다.", seller.getSellerName()));
    }

    public String login(String sellerName , String password) {
        // userName(ID)가 없는 경우
        SellerEntity seller = sellerRepository.findBySellerName(sellerName)
                .orElseThrow(() -> new AppException(ErrorCode.SELLER_NOT_FOUND, ErrorCode.SELLER_NOT_FOUND.getMessage()));

        // password가 맞지 않는 경우
        if(!encoder.matches(password, seller.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD, ErrorCode.INVALID_PASSWORD.getMessage());
        }

        return JwtTokenUtils.createToken(sellerName, secretKey, expiredTimeMs);
    }

    public SellerResponse findById(Long id) {
        SellerEntity seller = sellerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SELLER_NOT_FOUND, ErrorCode.SELLER_NOT_FOUND.getMessage()));

        return SellerResponse.toSellerResponse(seller);
    }

    public SellerResponse update(Long id, SellerUpdateRequest sellerUpdateRequest) {
        // seller가 존재하는지 확인
        SellerEntity seller = validateSeller(id);

        // 본인이거나 ADMIN이 아니면 에러
        if (!seller.getSellerName().equals(seller.getSellerName()) && seller.getUserRole() != UserRole.ADMIN) {
            throw new AppException(ErrorCode.INVALID_PERMISSION, ErrorCode.INVALID_PERMISSION.getMessage());
        }

        // request로 받은 sellerName과 저장되어 있는 sellerName과 다르다면 중복체크를 한다
        if (!seller.getSellerName().equals(sellerUpdateRequest.getSellerName())) {
            sellerRepository.findBySellerName(sellerUpdateRequest.getSellerName())
                    .ifPresent(sellerEntity -> {
                        throw new AppException(ErrorCode.DUPLICATED_USER_NAME, ErrorCode.DUPLICATED_USER_NAME.getMessage());
                    });
        }

        // password encoding 하기
        String password = sellerUpdateRequest.getPassword();
        password = encoder.encode(sellerUpdateRequest.getPassword());


        // request로 받은 이메일이 저장되어 있는 이메일과 다르다면 중복체크를 한다
        if (!seller.getEmail().equals(sellerUpdateRequest.getEmail())) {
            sellerRepository.findByEmail(sellerUpdateRequest.getEmail())
                    .ifPresent(sellerEntity -> {
                        throw new AppException(ErrorCode.DUPLICATED_EMAIL, ErrorCode.DUPLICATED_EMAIL.getMessage());
                    });
        }


        seller.updateUser(sellerUpdateRequest.getSellerName(), password, sellerUpdateRequest.getCompanyName(),
                sellerUpdateRequest.getAddress(),sellerUpdateRequest.getEmail(), sellerUpdateRequest.getPhoneNum(), sellerUpdateRequest.getBusinessRegNum());

        sellerRepository.save(seller);

        return SellerResponse.toSellerResponse(seller);
    }

    public SellerDeleteResponse deleteSeller(Long id) {
        SellerEntity seller = validateSeller(id);

        // 본인이거나 ADMIN이 아니면 에러
        if (!seller.getSellerName().equals(seller.getSellerName()) && seller.getUserRole() != UserRole.ADMIN) {
            throw new AppException(ErrorCode.INVALID_PERMISSION, ErrorCode.INVALID_PERMISSION.getMessage());
        }

        sellerRepository.delete(seller);
        return new SellerDeleteResponse(id, "회원 삭제가 완료되었습니다");
    }

    public Page<SellerListResponse> findAll(Pageable pageable) {

        // 20개씩 만들어진 순으로 정렬
        pageable = PageRequest.of(0, 20, Sort.by("createdDate").descending());

        Page<SellerEntity> sellers = sellerRepository.findAll(pageable);
        return sellers.map(SellerListResponse::from);
    }
}