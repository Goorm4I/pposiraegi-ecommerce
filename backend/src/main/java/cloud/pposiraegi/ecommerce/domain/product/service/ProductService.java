package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.dto.ImageDto;
import cloud.pposiraegi.ecommerce.domain.product.dto.ProductDto;
import cloud.pposiraegi.ecommerce.domain.product.entity.*;
import cloud.pposiraegi.ecommerce.domain.product.enums.ImageType;
import cloud.pposiraegi.ecommerce.domain.product.repository.*;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final SkuOptionMappingRepository skuOptionMappingRepository;

    private final TsidFactory tsidFactory;

    @Transactional()
    public List<ProductDto.ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductDto.ProductResponse::from)
                .toList();
    }

    @Transactional
    public Long createProduct(ProductDto.ProductCreateRequest request) {
        Product product = Product.builder()
                .id(tsidFactory.create().toLong())
                .categoryId(request.categoryId())
                .name(request.name())
                .description(request.description())
                .brandName(request.brandName())
                .originPrice(request.originPrice())
                .salePrice(request.salePrice())
                .status(request.status())
                .build();

        // THUMBNAIL 이미지 URL을 save 전에 세팅 (persist 전 설정으로 INSERT 시 반영)
        if (request.images() != null && !request.images().isEmpty()) {
            request.images().stream()
                    .filter(img -> img.imageType() == ImageType.THUMBNAIL)
                    .findFirst()
                    .ifPresent(thumb -> product.updateThumbnailUrl(thumb.imageUrl()));
        }

        productRepository.save(product);

        // 이미지 저장
        if (request.images() != null && !request.images().isEmpty()) {
            List<ProductImage> productImages = request.images().stream()
                    .map(image -> ProductImage.builder()
                            .id(tsidFactory.create().toLong())
                            .productId(product.getId())
                            .imageUrl(image.imageUrl())
                            .imageType(image.imageType())
                            .displayOrder(image.displayOrder())
                            .build())
                    .toList();
            productImageRepository.saveAll(productImages);
        }

        if (request.optionGroups() == null || request.optionGroups().isEmpty()) {
            ProductSku defaultSku = ProductSku.builder()
                    .id(tsidFactory.create().toLong())
                    .productId(product.getId())
                    .skuCode(request.skus().getFirst().skuCode())
                    .combinationKey("")
                    .status(request.skus().getFirst().status())
                    .additionalPrice(0)
                    .stockQuantity(request.skus().getFirst().stockQuantity())
                    .build();

            productSkuRepository.save(defaultSku);
            return product.getId();
        }

        List<Map<String, Long>> optionValueMaps = new ArrayList<>();
        List<ProductOption> optionsToSave = new ArrayList<>();
        List<ProductOptionValue> optionValuesToSave = new ArrayList<>();

        for (int i = 0; i < request.optionGroups().size(); i++) {
            var groupRequest = request.optionGroups().get(i);
            ProductOption productOption = ProductOption.builder()
                    .id(tsidFactory.create().toLong())
                    .productId(product.getId())
                    .name(groupRequest.optionName())
                    .displayOrder(i + 1)
                    .build();
            optionsToSave.add(productOption);

            Map<String, Long> valueMap = new HashMap<>();
            for (int j = 0; j < groupRequest.optionsValues().size(); j++) {
                String value = groupRequest.optionsValues().get(j);
                ProductOptionValue productOptionValue = ProductOptionValue.builder()
                        .id(tsidFactory.create().toLong())
                        .optionId(productOption.getId())
                        .value(value)
                        .displayOrder(j + 1)
                        .build();
                optionValuesToSave.add(productOptionValue);
                valueMap.put(value, productOptionValue.getId());
            }
            optionValueMaps.add(valueMap);
        }

        productOptionRepository.saveAll(optionsToSave);
        productOptionValueRepository.saveAll(optionValuesToSave);

        List<ProductSku> skusToSave = new ArrayList<>();
        List<SkuOptionMapping> skuOptionMappingsToSave = new ArrayList<>();

        for (var skuRequest : request.skus()) {

            List<Long> selectedOptionValueIds = new ArrayList<>();
            for (int i = 0; i < skuRequest.selectedOptionValues().size(); i++) {
                String selectedOptionValue = skuRequest.selectedOptionValues().get(i);
                selectedOptionValueIds.add(optionValueMaps.get(i).get(selectedOptionValue));
            }

            String combinationKey = selectedOptionValueIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(":"));

            ProductSku sku = ProductSku.builder()
                    .id(tsidFactory.create().toLong())
                    .productId(product.getId())
                    .skuCode(skuRequest.skuCode())
                    .combinationKey(combinationKey)
                    .status(skuRequest.status())
                    .additionalPrice(skuRequest.additionalPrice())
                    .stockQuantity(skuRequest.stockQuantity())
                    .build();

            skusToSave.add(sku);

            for (int i = 0; i < skuRequest.selectedOptionValues().size(); i++) {
                SkuOptionMapping mapping = SkuOptionMapping.builder()
                        .id(tsidFactory.create().toLong())
                        .skuId(sku.getId())
                        .optionValueId(optionValueMaps.get(i).get(skuRequest.selectedOptionValues().get(i)))
                        .build();

                skuOptionMappingsToSave.add(mapping);
            }
        }

        productSkuRepository.saveAll(skusToSave);
        skuOptionMappingRepository.saveAll(skuOptionMappingsToSave);

        return product.getId();
    }

    @Transactional(readOnly = true)
    public ProductDto.ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ImageDto.ImageResponse> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)
                .stream()
                .map(image -> new ImageDto.ImageResponse(
                        image.getImageUrl(), image.getImageType(), image.getDisplayOrder()
                )).toList();

        List<ProductOption> options = productOptionRepository.findByProductId(productId);
        List<ProductOptionValue> allOptionValues = productOptionValueRepository.findByOptionIdIn(
                options.stream().map(ProductOption::getId).toList()
        );

        List<ProductDto.OptionGroupResponse> optionGroups = options.stream().map(option -> {
            List<ProductDto.OptionValueResponse> optionValues = allOptionValues.stream()
                    .filter(optionValue -> optionValue.getOptionId().equals(option.getId()))
                    .map(optionValue -> new ProductDto.OptionValueResponse(optionValue.getId(), optionValue.getValue()))
                    .toList();
            return new ProductDto.OptionGroupResponse(option.getId(), option.getName(), optionValues);
        }).toList();

        Map<Long, String> valueIdToNameMap = allOptionValues.stream()
                .collect(Collectors.toMap(ProductOptionValue::getId, ProductOptionValue::getValue));

        List<ProductDto.SkuResponse> skus = productSkuRepository.findByProductId(productId)
                .stream()
                .map(sku -> {
                    List<Long> optionValues = new ArrayList<>();

                    if (sku.getCombinationKey() != null && !sku.getCombinationKey().isEmpty()) {
                        String[] valueIds = sku.getCombinationKey().split(":");
                        for (String idStr : valueIds) {
                            Long valueId = Long.parseLong(idStr);
                            optionValues.add(valueId);
                        }
                    }
                    return ProductDto.SkuResponse.from(sku, optionValues);
                }).toList();

        return ProductDto.ProductDetailResponse.from(product, images, optionGroups, skus);
    }

}
